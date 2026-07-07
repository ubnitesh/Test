package com.nokia.powehi.rapp.mloptimizer.service;

import com.nokia.powehi.rapp.mloptimizer.model.CellPerformanceMetrics;
import com.nokia.powehi.rapp.mloptimizer.model.TrafficSteeringRecommendation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TrafficSteeringMlService {

    static final double PRB_CRITICAL_THRESHOLD = 80.0;
    static final double RSRQ_CRITICAL_THRESHOLD = -12.0;
    static final double MIN_OFFLOAD_PERCENT = 20.0;
    static final double MAX_OFFLOAD_PERCENT = 50.0;

    private final OnnxModelHook onnxModelHook;

    public TrafficSteeringMlService(OnnxModelHook onnxModelHook) {
        this.onnxModelHook = onnxModelHook;
    }

    /**
     * Evaluates cell metrics and returns a load-balancing recommendation when congestion
     * is critical (PRB utilization above 80% and average RSRQ below -12 dB).
     *
     * @param metrics            incoming cell performance telemetry
     * @param neighborTargetCells neighboring cells eligible to receive offloaded traffic
     */
    public TrafficSteeringRecommendation evaluate(
            CellPerformanceMetrics metrics,
            List<String> neighborTargetCells) {

        double prbUtilization = metrics.getPrbUtilization();
        double averageRsrq = metrics.getRsrq();
        boolean criticalCongestion = isCriticalCongestion(prbUtilization, averageRsrq);

        if (!criticalCongestion) {
            log.debug(
                    "Cell {} within limits — PRB {}%, RSRQ {} dB",
                    metrics.getCellId(),
                    prbUtilization,
                    averageRsrq);
            return TrafficSteeringRecommendation.builder()
                    .sourceCellId(metrics.getCellId())
                    .criticalCongestion(false)
                    .loadBalancingThreshold(PRB_CRITICAL_THRESHOLD)
                    .offloadPercentage(0.0)
                    .targetCellIds(List.copyOf(neighborTargetCells))
                    .rationale("No steering required — thresholds not exceeded")
                    .build();
        }

        double offloadPercentage = resolveOffloadPercentage(metrics, prbUtilization, averageRsrq);
        double loadBalancingThreshold = calculateLoadBalancingThreshold(
                prbUtilization, averageRsrq, offloadPercentage);

        log.info(
                "Critical congestion on cell {} — PRB {}%, RSRQ {} dB, "
                        + "offload {}% to {} neighbor(s), target PRB {}%",
                metrics.getCellId(),
                prbUtilization,
                averageRsrq,
                offloadPercentage,
                neighborTargetCells.size(),
                loadBalancingThreshold);

        return TrafficSteeringRecommendation.builder()
                .sourceCellId(metrics.getCellId())
                .criticalCongestion(true)
                .loadBalancingThreshold(loadBalancingThreshold)
                .offloadPercentage(offloadPercentage)
                .targetCellIds(List.copyOf(neighborTargetCells))
                .rationale(buildRationale(prbUtilization, averageRsrq, offloadPercentage))
                .build();
    }

    boolean isCriticalCongestion(double prbUtilization, double averageRsrq) {
        return prbUtilization > PRB_CRITICAL_THRESHOLD && averageRsrq < RSRQ_CRITICAL_THRESHOLD;
    }

    private double resolveOffloadPercentage(
            CellPerformanceMetrics metrics, double prbUtilization, double averageRsrq) {

        return onnxModelHook
                .predictOffloadPercentage(metrics)
                .map(this::clampOffloadPercentage)
                .orElseGet(() -> heuristicOffloadPercentage(prbUtilization, averageRsrq));
    }

    /**
     * Threshold matrix: severity rises with PRB headroom above 80% and RSRQ degradation below -12 dB.
     */
    double heuristicOffloadPercentage(double prbUtilization, double averageRsrq) {
        double prbSeverity = (prbUtilization - PRB_CRITICAL_THRESHOLD) / 15.0;
        double rsrqSeverity = (RSRQ_CRITICAL_THRESHOLD - averageRsrq) / 10.0;
        double combinedSeverity = clamp(prbSeverity + rsrqSeverity, 0.0, 1.0);
        return MIN_OFFLOAD_PERCENT + combinedSeverity * (MAX_OFFLOAD_PERCENT - MIN_OFFLOAD_PERCENT);
    }

    /**
     * Target PRB utilization for the source cell after distributing offload across neighbors.
     */
    double calculateLoadBalancingThreshold(
            double prbUtilization, double averageRsrq, double offloadPercentage) {

        double projectedPrb = prbUtilization * (1.0 - offloadPercentage / 100.0);
        double rsrqRelief = Math.max(0.0, (RSRQ_CRITICAL_THRESHOLD - averageRsrq) * 0.25);
        return clamp(projectedPrb + rsrqRelief, PRB_CRITICAL_THRESHOLD - 5.0, PRB_CRITICAL_THRESHOLD);
    }

    private double clampOffloadPercentage(double value) {
        return clamp(value, MIN_OFFLOAD_PERCENT, MAX_OFFLOAD_PERCENT);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String buildRationale(
            double prbUtilization, double averageRsrq, double offloadPercentage) {

        return String.format(
                "Critical congestion: PRB %.1f%% > %.1f%% and RSRQ %.1f dB < %.1f dB — "
                        + "recommend offloading %.1f%% of active users to neighbor cells",
                prbUtilization,
                PRB_CRITICAL_THRESHOLD,
                averageRsrq,
                RSRQ_CRITICAL_THRESHOLD,
                offloadPercentage);
    }
}
