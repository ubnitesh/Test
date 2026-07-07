package org.oransc.rappmanager.dme.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.dme.repository.RanTelemetryRepository;
import org.oransc.rappmanager.models.dme.CellKpiTelemetry;
import org.oransc.rappmanager.models.dme.NetworkKpiSummary;
import org.oransc.rappmanager.models.exception.DmeTelemetryException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rappmanager.dme.telemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RanTelemetryExposureService {

    private final RanTelemetryRepository ranTelemetryRepository;

    public List<String> listCells() {
        ensureDatabaseAvailable();
        return ranTelemetryRepository.findAllCellIds();
    }

    public List<CellKpiTelemetry> getLatestKpis() {
        ensureDatabaseAvailable();
        return ranTelemetryRepository.findLatestKpis();
    }

    public CellKpiTelemetry getLatestKpiForCell(String cellId) {
        ensureDatabaseAvailable();
        return ranTelemetryRepository.findLatestKpiByCellId(cellId)
                .orElseThrow(() -> new DmeTelemetryException(
                        404, "No telemetry found for cell: " + cellId));
    }

    public List<CellKpiTelemetry> getHistoryForCell(String cellId, int limit) {
        ensureDatabaseAvailable();
        if (ranTelemetryRepository.findLatestKpiByCellId(cellId).isEmpty()) {
            throw new DmeTelemetryException(404, "No telemetry found for cell: " + cellId);
        }
        return ranTelemetryRepository.findHistoryByCellId(cellId, limit);
    }

    public NetworkKpiSummary getNetworkSummary() {
        List<CellKpiTelemetry> latest = getLatestKpis();
        if (latest.isEmpty()) {
            throw new DmeTelemetryException(
                    404, "No telemetry snapshots available in the simulator database");
        }

        Instant recordedAt = latest.stream()
                .map(CellKpiTelemetry::getRecordedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return NetworkKpiSummary.builder()
                .recordedAt(recordedAt)
                .cellCount(latest.size())
                .weightedRsrp(weightedAverage(latest, CellKpiTelemetry::getRsrp))
                .weightedRsrq(weightedAverage(latest, CellKpiTelemetry::getRsrq))
                .weightedActiveUsers(weightedAverage(latest, cell -> cell.getActiveUsers().doubleValue()))
                .weightedPrbUtilization(weightedAverage(latest, CellKpiTelemetry::getPrbUtilization))
                .build();
    }

    private void ensureDatabaseAvailable() {
        if (!ranTelemetryRepository.databaseAvailable()) {
            throw new DmeTelemetryException(
                    503,
                    "RAN telemetry database is unavailable. "
                            + "Start ran_cell_simulator.py to populate the SQLite store.");
        }
    }

    private static double weightedAverage(
            List<CellKpiTelemetry> cells, ToDoubleFunction<CellKpiTelemetry> valueExtractor) {
        double weightSum = 0.0;
        double valueSum = 0.0;
        for (CellKpiTelemetry cell : cells) {
            double weight = Math.max(cell.getActiveUsers(), 1);
            weightSum += weight;
            valueSum += valueExtractor.applyAsDouble(cell) * weight;
        }
        return weightSum == 0.0 ? 0.0 : valueSum / weightSum;
    }
}
