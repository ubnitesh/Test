package org.oransc.rappmanager.dme.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.dme.configuration.DmeConfiguration;
import org.oransc.rappmanager.dme.service.RanTelemetryExposureService;
import org.oransc.rappmanager.models.dme.CellKpiTelemetry;
import org.oransc.rappmanager.models.dme.NetworkKpiSummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DME exposure API for simulated 5G RAN KPI telemetry produced by ran_cell_simulator.py.
 */
@RestController
@RequestMapping("${rappmanager.dme.telemetry.api-base-path:/dme/ran-telemetry/v1}")
@ConditionalOnProperty(prefix = "rappmanager.dme.telemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Tag(name = "DME RAN Telemetry")
public class DmeRanTelemetryController {

    private final RanTelemetryExposureService ranTelemetryExposureService;
    private final DmeConfiguration dmeConfiguration;

    @GetMapping("/cells")
    @Operation(summary = "List simulated NR cells", description = "Returns cell identifiers cell-1 .. cell-100 from the simulator database.")
    public ResponseEntity<List<String>> listCells() {
        return ResponseEntity.ok(ranTelemetryExposureService.listCells());
    }

    @GetMapping("/kpi/latest")
    @Operation(summary = "Latest KPI snapshot for all cells")
    public ResponseEntity<List<CellKpiTelemetry>> getLatestKpis() {
        return ResponseEntity.ok(ranTelemetryExposureService.getLatestKpis());
    }

    @GetMapping("/kpi/latest/{cellId}")
    @Operation(summary = "Latest KPI snapshot for one cell")
    public ResponseEntity<CellKpiTelemetry> getLatestKpiForCell(@PathVariable String cellId) {
        return ResponseEntity.ok(ranTelemetryExposureService.getLatestKpiForCell(cellId));
    }

    @GetMapping("/kpi/history/{cellId}")
    @Operation(summary = "Historical KPI samples for one cell")
    public ResponseEntity<List<CellKpiTelemetry>> getHistoryForCell(
            @PathVariable String cellId,
            @RequestParam(defaultValue = "60") int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 1000);
        return ResponseEntity.ok(ranTelemetryExposureService.getHistoryForCell(cellId, boundedLimit));
    }

    @GetMapping("/kpi/summary")
    @Operation(
            summary = "Network-wide weighted KPI summary",
            description = "Active-user-weighted averages across all cells, matching ran_cell_simulator.py chart logic.")
    public ResponseEntity<NetworkKpiSummary> getNetworkSummary() {
        return ResponseEntity.ok(ranTelemetryExposureService.getNetworkSummary());
    }

    @GetMapping("/info")
    @Operation(summary = "DME telemetry exposure metadata")
    public ResponseEntity<DmeTelemetryInfo> getExposureInfo() {
        DmeConfiguration.Telemetry telemetry = dmeConfiguration.getTelemetry();
        return ResponseEntity.ok(new DmeTelemetryInfo(
                telemetry.getApiBasePath(),
                telemetry.getDatabasePath(),
                dmeConfiguration.getBaseUrl()));
    }

    public record DmeTelemetryInfo(String apiBasePath, String databasePath, String dmeBaseUrl) {}
}
