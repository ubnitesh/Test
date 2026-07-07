package com.nokia.powehi.rapp.mloptimizer.service;

import com.nokia.powehi.rapp.mloptimizer.config.A1MediatorProperties;
import com.nokia.powehi.rapp.mloptimizer.model.A1DeclarativePolicy;
import com.nokia.powehi.rapp.mloptimizer.model.A1PolicyPayload;
import com.nokia.powehi.rapp.mloptimizer.model.A1PolicyPublishResult;
import com.nokia.powehi.rapp.mloptimizer.model.A1TargetScope;
import com.nokia.powehi.rapp.mloptimizer.model.TrafficSteeringRecommendation;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class A1PolicyProducerService {

    private final WebClient a1MediatorWebClient;
    private final A1MediatorProperties properties;

    public Mono<A1PolicyPublishResult> publishSteeringPolicy(TrafficSteeringRecommendation recommendation) {
        A1DeclarativePolicy policy = buildPolicy(recommendation);

        if (!properties.isPublishEnabled()) {
            log.info(
                    "A1 publish disabled — mock accept policy {} for cell {}",
                    policy.getPolicyId(),
                    recommendation.getSourceCellId());
            return Mono.just(A1PolicyPublishResult.builder()
                    .policyId(policy.getPolicyId())
                    .status("mock-accepted")
                    .build());
        }

        log.info(
                "POST A1 steering policy {} to Near-RT RIC A1 Mediator for cell {}",
                policy.getPolicyId(),
                recommendation.getSourceCellId());

        return a1MediatorWebClient
                .post()
                .uri(properties.getPoliciesPath())
                .bodyValue(policy)
                .retrieve()
                .bodyToMono(A1PolicyPublishResult.class)
                .onErrorResume(error -> {
                    log.error(
                            "Failed to POST policy {} to A1 Mediator: {}",
                            policy.getPolicyId(),
                            error.getMessage());
                    return Mono.just(A1PolicyPublishResult.builder()
                            .policyId(policy.getPolicyId())
                            .status("failed")
                            .build());
                });
    }

    A1DeclarativePolicy buildPolicy(TrafficSteeringRecommendation recommendation) {
        String policyId = UUID.randomUUID().toString();
        String statusUri = properties.getStatusNotificationBase().replaceAll("/$", "")
                + "/a1/policies/" + policyId + "/status";

        return A1DeclarativePolicy.builder()
                .policyId(policyId)
                .policyTypeId(properties.getPolicyTypeId())
                .serviceId(properties.getServiceId())
                .statusNotificationUri(statusUri)
                .policyData(toPolicyPayload(recommendation))
                .build();
    }

    private A1PolicyPayload toPolicyPayload(TrafficSteeringRecommendation recommendation) {
        String targetCells = recommendation.getTargetCellIds().isEmpty()
                ? "neighbor_cells"
                : recommendation.getTargetCellIds().stream().collect(Collectors.joining(","));

        return A1PolicyPayload.builder()
                .targetScope(A1TargetScope.builder()
                        .cellId(recommendation.getSourceCellId())
                        .build())
                .policyStatements(new String[] {
                    String.format("OFFLOAD:%.1f", recommendation.getOffloadPercentage()),
                    "TARGET:" + targetCells,
                    String.format("THRESHOLD:prb_utilization<%.1f", recommendation.getLoadBalancingThreshold()),
                    "ACTION:TRAFFIC_STEERING"
                })
                .build();
    }
}
