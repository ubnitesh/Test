package org.oransc.rappmanager.dme.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.models.dme.CellKpiTelemetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rappmanager.dme.telemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RanTelemetryRepository {

    private static final RowMapper<CellKpiTelemetry> KPI_ROW_MAPPER = RanTelemetryRepository::mapKpiRow;

    private final JdbcTemplate jdbcTemplate;

    public boolean databaseAvailable() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public List<String> findAllCellIds() {
        return jdbcTemplate.queryForList("SELECT cell_id FROM cells ORDER BY cell_id", String.class);
    }

    public List<CellKpiTelemetry> findLatestKpis() {
        return jdbcTemplate.query(
                """
                SELECT cell_id, recorded_at, rsrp_dbm, rsrq_db, active_users, prb_utilization
                FROM cell_latest
                ORDER BY cell_id
                """,
                KPI_ROW_MAPPER);
    }

    public Optional<CellKpiTelemetry> findLatestKpiByCellId(String cellId) {
        List<CellKpiTelemetry> rows = jdbcTemplate.query(
                """
                SELECT cell_id, recorded_at, rsrp_dbm, rsrq_db, active_users, prb_utilization
                FROM cell_latest
                WHERE cell_id = ?
                """,
                KPI_ROW_MAPPER,
                cellId);
        return rows.stream().findFirst();
    }

    public List<CellKpiTelemetry> findHistoryByCellId(String cellId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT cell_id, recorded_at, rsrp_dbm, rsrq_db, active_users, prb_utilization
                FROM cell_telemetry
                WHERE cell_id = ?
                ORDER BY recorded_at DESC
                LIMIT ?
                """,
                KPI_ROW_MAPPER,
                cellId,
                limit);
    }

    private static CellKpiTelemetry mapKpiRow(ResultSet rs, int rowNum) throws SQLException {
        return CellKpiTelemetry.builder()
                .cellId(rs.getString("cell_id"))
                .recordedAt(Instant.parse(rs.getString("recorded_at")))
                .rsrp(rs.getDouble("rsrp_dbm"))
                .rsrq(rs.getDouble("rsrq_db"))
                .activeUsers(rs.getInt("active_users"))
                .prbUtilization(rs.getDouble("prb_utilization"))
                .build();
    }
}
