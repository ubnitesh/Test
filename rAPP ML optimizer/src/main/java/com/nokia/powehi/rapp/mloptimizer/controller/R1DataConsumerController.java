package com.nokia.powehi.rapp.mloptimizer.controller;

import com.nokia.powehi.rapp.mloptimizer.model.CellPerformanceMetrics;
import com.nokia.powehi.rapp.mloptimizer.model.TelemetryIngestResponse;
import com.nokia.powehi.rapp.mloptimizer.model.TrafficSteeringRecommendation;
import com.nokia.powehi.rapp.mloptimizer.service.A1PolicyProducerService;
import com.nokia.powehi.rapp.mloptimizer.service.TrafficSteeringMlService;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/r1/telemetry")
@RequiredArgsConstructor
@Slf4j
public class R1DataConsumerController {

    private final TrafficSteeringMlService trafficSteeringMlService;
    private final A1PolicyProducerService a1PolicyProducerService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<TelemetryIngestResponse> ingestTelemetry(
            @Valid @RequestBody Mono<CellPerformanceMetrics> metrics,
            @RequestParam(required = false) List<String> neighborCells) {

        List<String> neighbors = neighborCells == null ? Collections.emptyList() : neighborCells;

        return metrics
                .doOnNext(m -> log.info("Received R1 telemetry for cell {}", m.getCellId()))
                .map(m -> trafficSteeringMlService.evaluate(m, neighbors))
                .flatMap(this::handleRecommendation);
    }

    private Mono<TelemetryIngestResponse> handleRecommendation(TrafficSteeringRecommendation recommendation) {
        if (!recommendation.isCriticalCongestion()) {
            return Mono.just(TelemetryIngestResponse.builder()
                    .cellId(recommendation.getSourceCellId())
                    .criticalCongestion(false)
                    .a1PolicyPublished(false)
                    .recommendation(recommendation)
                    .build());
        }

        return a1PolicyProducerService
                .publishSteeringPolicy(recommendation)
                .map(result -> TelemetryIngestResponse.builder()
                        .cellId(recommendation.getSourceCellId())
                        .criticalCongestion(true)
                        .a1PolicyPublished(true)
                        .policyId(result.getPolicyId())
                        .recommendation(recommendation)
                        .build());
    }
}
