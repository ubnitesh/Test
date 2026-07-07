package org.oransc.rappmanager.dme;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DmeRanTelemetryControllerTests {

    private static final Path DATABASE_PATH = createTelemetryDatabase();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void registerTelemetryDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "rappmanager.dme.telemetry.database-path",
                () -> DATABASE_PATH.toAbsolutePath().toString());
    }

    private static Path createTelemetryDatabase() {
        try {
            Path databasePath = Files.createTempFile("ran-telemetry-test-", ".db");
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
                    Statement statement = connection.createStatement()) {
                statement.execute(
                        "CREATE TABLE cells ("
                                + "cell_id TEXT PRIMARY KEY, "
                                + "created_at TEXT NOT NULL DEFAULT (datetime('now')))");
                statement.execute(
                        "CREATE TABLE cell_latest ("
                                + "cell_id TEXT PRIMARY KEY, "
                                + "recorded_at TEXT NOT NULL, "
                                + "rsrp_dbm REAL NOT NULL, "
                                + "rsrq_db REAL NOT NULL, "
                                + "active_users INTEGER NOT NULL, "
                                + "prb_utilization REAL NOT NULL)");
                statement.execute(
                        "CREATE TABLE cell_telemetry ("
                                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + "cell_id TEXT NOT NULL, "
                                + "recorded_at TEXT NOT NULL, "
                                + "rsrp_dbm REAL NOT NULL, "
                                + "rsrq_db REAL NOT NULL, "
                                + "active_users INTEGER NOT NULL, "
                                + "prb_utilization REAL NOT NULL)");
                statement.execute("INSERT INTO cells (cell_id) VALUES ('cell-1'), ('cell-2')");
                statement.execute(
                        """
                        INSERT INTO cell_latest
                            (cell_id, recorded_at, rsrp_dbm, rsrq_db, active_users, prb_utilization)
                        VALUES
                            ('cell-1', '2026-07-07T12:00:00Z', -95.0, -11.0, 200, 45.0),
                            ('cell-2', '2026-07-07T12:00:00Z', -100.0, -13.0, 100, 70.0)
                        """);
                statement.execute(
                        """
                        INSERT INTO cell_telemetry
                            (cell_id, recorded_at, rsrp_dbm, rsrq_db, active_users, prb_utilization)
                        VALUES
                            ('cell-1', '2026-07-07T11:59:00Z', -96.0, -11.5, 190, 44.0),
                            ('cell-1', '2026-07-07T12:00:00Z', -95.0, -11.0, 200, 45.0)
                        """);
            }
            return databasePath;
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Test
    void listCellsReturnsRegisteredCells() throws Exception {
        mockMvc.perform(get("/dme/ran-telemetry/v1/cells"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("cell-1"));
    }

    @Test
    void getLatestKpisReturnsAllCells() throws Exception {
        mockMvc.perform(get("/dme/ran-telemetry/v1/kpi/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].cellId").value("cell-1"))
                .andExpect(jsonPath("$[0].rsrp").value(-95.0));
    }

    @Test
    void getLatestKpiForCellReturnsOneSnapshot() throws Exception {
        mockMvc.perform(get("/dme/ran-telemetry/v1/kpi/latest/cell-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cellId").value("cell-2"))
                .andExpect(jsonPath("$.prbUtilization").value(70.0));
    }

    @Test
    void getHistoryForCellReturnsDescendingSamples() throws Exception {
        mockMvc.perform(get("/dme/ran-telemetry/v1/kpi/history/cell-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rsrp").value(-95.0));
    }

    @Test
    void getNetworkSummaryReturnsWeightedAverages() throws Exception {
        mockMvc.perform(get("/dme/ran-telemetry/v1/kpi/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cellCount").value(2))
                .andExpect(jsonPath("$.weightedRsrp").value(-96.66666666666667))
                .andExpect(jsonPath("$.weightedActiveUsers").value(166.66666666666666));
    }

    @Test
    void getExposureInfoReturnsConfiguredPaths() throws Exception {
        mockMvc.perform(get("/dme/ran-telemetry/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiBasePath").value("/dme/ran-telemetry/v1"))
                .andExpect(jsonPath("$.dmeBaseUrl").value("http://localhost:9082"));
    }
}
