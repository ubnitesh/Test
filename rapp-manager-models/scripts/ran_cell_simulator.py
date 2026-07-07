#!/usr/bin/env python3
"""
5G RAN cell performance simulator for 20 NR cells (cell-1 .. cell-20).

Models:
  - RSRP: Gaussian random walk plus log-normal shadow fading (dB domain)
  - RSRQ: correlated with traffic load within 3GPP / O-RAN ranges
  - Active users: Poisson arrivals, exponential (Poisson) departures per tick
  - PRB utilization: Beta distribution scaled by active-user load factor

Persists telemetry to a SQLite database and plots weighted network KPI averages.
"""

from __future__ import annotations

import argparse
import logging
import os
import sqlite3
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

import matplotlib.pyplot as plt
import matplotlib.animation as animation
import numpy as np

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger("ran-cell-simulator")

# ---------------------------------------------------------------------------
# 3GPP / O-RAN reference thresholds (aligned with rAPP ML Optimizer sample)
# ---------------------------------------------------------------------------
RSRP_MIN_DBM = -120.0
RSRP_MAX_DBM = -70.0
RSRQ_MIN_DB = -20.0
RSRQ_MAX_DB = -3.0
RSRQ_CRITICAL_DB = -12.0
PRB_CRITICAL_PCT = 80.0

NUM_CELLS = 20
DEFAULT_DB_PATH = Path(__file__).resolve().parent.parent / "data" / "ran_telemetry.db"
DEFAULT_TICK_SECONDS = 1.0
CHART_HISTORY_POINTS = 120


@dataclass
class SimulatorConfig:
    db_path: Path = DEFAULT_DB_PATH
    tick_seconds: float = DEFAULT_TICK_SECONDS
    num_cells: int = NUM_CELLS
    max_users_per_cell: int = 500
    arrival_rate_per_sec: float = 8.0
    departure_rate_per_user_sec: float = 0.02
    rsrp_walk_sigma_db: float = 0.35
    shadow_sigma_db: float = 1.2
    beta_alpha: float = 2.5
    beta_beta: float = 1.8
    chart_history: int = CHART_HISTORY_POINTS


@dataclass
class CellState:
    cell_id: str
    rsrp_dbm: float
    rsrq_db: float
    active_users: int
    prb_utilization_pct: float


@dataclass
class NetworkSnapshot:
    timestamp: datetime
    cells: list[CellState] = field(default_factory=list)

    @property
    def weights(self) -> np.ndarray:
        return np.array(
            [max(c.active_users, 1) for c in self.cells],
            dtype=np.float64,
        )

    def weighted_average(self, attr: str) -> float:
        values = np.array([getattr(c, attr) for c in self.cells], dtype=np.float64)
        return float(np.average(values, weights=self.weights))


class RanTelemetryDatabase:
    """SQLite persistence for cell registry and time-series telemetry."""

    def __init__(self, db_path: Path) -> None:
        self.db_path = db_path
        db_path.parent.mkdir(parents=True, exist_ok=True)
        self._conn = sqlite3.connect(db_path, check_same_thread=False)
        self._conn.row_factory = sqlite3.Row
        self._initialize_schema()

    def _initialize_schema(self) -> None:
        self._conn.executescript(
            """
            PRAGMA journal_mode = WAL;

            CREATE TABLE IF NOT EXISTS cells (
                cell_id     TEXT PRIMARY KEY,
                created_at  TEXT NOT NULL DEFAULT (datetime('now'))
            );

            CREATE TABLE IF NOT EXISTS cell_telemetry (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                cell_id         TEXT NOT NULL,
                recorded_at     TEXT NOT NULL,
                rsrp_dbm        REAL NOT NULL,
                rsrq_db         REAL NOT NULL,
                active_users    INTEGER NOT NULL,
                prb_utilization REAL NOT NULL,
                FOREIGN KEY (cell_id) REFERENCES cells(cell_id)
            );

            CREATE TABLE IF NOT EXISTS cell_latest (
                cell_id         TEXT PRIMARY KEY,
                recorded_at     TEXT NOT NULL,
                rsrp_dbm        REAL NOT NULL,
                rsrq_db         REAL NOT NULL,
                active_users    INTEGER NOT NULL,
                prb_utilization REAL NOT NULL,
                FOREIGN KEY (cell_id) REFERENCES cells(cell_id)
            );

            CREATE INDEX IF NOT EXISTS idx_telemetry_cell_time
                ON cell_telemetry(cell_id, recorded_at);
            """
        )
        self._conn.commit()

    def seed_cells(self, cell_ids: list[str]) -> None:
        """Register cells in the fleet registry (idempotent INSERT OR IGNORE)."""
        self._conn.executemany(
            "INSERT OR IGNORE INTO cells (cell_id) VALUES (?)",
            [(cell_id,) for cell_id in cell_ids],
        )
        self._conn.commit()
        logger.info("Seeded %d cells in %s", len(cell_ids), self.db_path)

    def prune_inactive_latest_cells(self, active_cell_ids: list[str]) -> None:
        """Remove stale rows from cell_latest for cells outside the active fleet.

        Intended for one-time startup sync when the simulated fleet size changes
        (e.g. 100 → 20 cells). Does not modify cell_telemetry history.
        """
        if not active_cell_ids:
            self._conn.execute("DELETE FROM cell_latest")
        else:
            placeholders = ", ".join("?" for _ in active_cell_ids)
            self._conn.execute(
                f"DELETE FROM cell_latest WHERE cell_id NOT IN ({placeholders})",
                tuple(active_cell_ids),
            )
        self._conn.commit()
        logger.info(
            "Pruned cell_latest to %d active cells in %s",
            len(active_cell_ids),
            self.db_path,
        )

    def persist_snapshot(self, snapshot: NetworkSnapshot) -> None:
        recorded_at = snapshot.timestamp.isoformat()
        rows = [
            (
                cell.cell_id,
                recorded_at,
                cell.rsrp_dbm,
                cell.rsrq_db,
                cell.active_users,
                cell.prb_utilization_pct,
            )
            for cell in snapshot.cells
        ]

        with self._conn:
            self._conn.executemany(
                """
                INSERT INTO cell_telemetry
                    (cell_id, recorded_at, rsrp_dbm, rsrq_db, active_users, prb_utilization)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                rows,
            )
            self._conn.executemany(
                """
                INSERT INTO cell_latest
                    (cell_id, recorded_at, rsrp_dbm, rsrq_db, active_users, prb_utilization)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(cell_id) DO UPDATE SET
                    recorded_at     = excluded.recorded_at,
                    rsrp_dbm        = excluded.rsrp_dbm,
                    rsrq_db         = excluded.rsrq_db,
                    active_users    = excluded.active_users,
                    prb_utilization = excluded.prb_utilization
                """,
                rows,
            )

    def close(self) -> None:
        self._conn.close()


class RanCellSimulator:
    """Stochastic 5G RAN KPI generator for a fleet of cells."""

    def __init__(self, config: SimulatorConfig) -> None:
        self.config = config
        self._rng = np.random.default_rng()
        self._cells = self._initialize_cells()

    def _initialize_cells(self) -> list[CellState]:
        cells: list[CellState] = []
        for index in range(1, self.config.num_cells + 1):
            cell_id = f"cell-{index}"
            base_rsrp = self._rng.uniform(-112.0, -82.0)
            active_users = int(self._rng.integers(40, 320))
            prb = self._derive_prb(active_users, self.config.max_users_per_cell)
            rsrq = self._derive_rsrq(base_rsrp, active_users, prb)
            cells.append(
                CellState(
                    cell_id=cell_id,
                    rsrp_dbm=base_rsrp,
                    rsrq_db=rsrq,
                    active_users=active_users,
                    prb_utilization_pct=prb,
                )
            )
        return cells

    def _derive_rsrq(
        self,
        rsrp_dbm: float,
        active_users: int,
        prb_utilization_pct: float,
    ) -> float:
        """Map RSRP and load to RSRQ within standardized NR measurement bounds."""
        rsrp_quality = (rsrp_dbm - RSRP_MIN_DBM) / (RSRP_MAX_DBM - RSRP_MIN_DBM)
        rsrp_quality = float(np.clip(rsrp_quality, 0.0, 1.0))

        load_ratio = active_users / self.config.max_users_per_cell
        prb_ratio = prb_utilization_pct / 100.0

        base_rsrq = RSRQ_MAX_DB - 9.0 * (1.0 - rsrp_quality)
        load_penalty = 5.5 * load_ratio + 4.0 * prb_ratio
        noise = self._rng.normal(0.0, 0.25)

        rsrq = base_rsrq - load_penalty + noise
        return float(np.clip(rsrq, RSRQ_MIN_DB, RSRQ_MAX_DB))

    def _derive_prb(self, active_users: int, max_users: int) -> float:
        beta_sample = self._rng.beta(self.config.beta_alpha, self.config.beta_beta)
        load_scale = active_users / max_users
        prb = beta_sample * load_scale * 100.0
        return float(np.clip(prb, 0.0, 100.0))

    def _update_active_users(self, cell: CellState) -> None:
        dt = self.config.tick_seconds
        arrivals = int(self._rng.poisson(self.config.arrival_rate_per_sec * dt))
        if cell.active_users > 0:
            departures = int(
                self._rng.poisson(
                    self.config.departure_rate_per_user_sec * cell.active_users * dt
                )
            )
            departures = min(departures, cell.active_users)
        else:
            departures = 0

        cell.active_users = int(
            np.clip(
                cell.active_users + arrivals - departures,
                0,
                self.config.max_users_per_cell,
            )
        )

    def _update_rsrp(self, cell: CellState) -> None:
        walk_step = self._rng.normal(0.0, self.config.rsrp_walk_sigma_db)
        shadow_fading_db = self._rng.normal(0.0, self.config.shadow_sigma_db)
        cell.rsrp_dbm = float(
            np.clip(
                cell.rsrp_dbm + walk_step + shadow_fading_db,
                RSRP_MIN_DBM,
                RSRP_MAX_DBM,
            )
        )

    def advance(self) -> NetworkSnapshot:
        for cell in self._cells:
            self._update_active_users(cell)
            self._update_rsrp(cell)
            cell.prb_utilization_pct = self._derive_prb(
                cell.active_users,
                self.config.max_users_per_cell,
            )
            cell.rsrq_db = self._derive_rsrq(
                cell.rsrp_dbm,
                cell.active_users,
                cell.prb_utilization_pct,
            )

        return NetworkSnapshot(
            timestamp=datetime.now(timezone.utc),
            cells=list(self._cells),
        )


class WeightedKpiChart:
    """Live matplotlib chart of active-user-weighted network KPI averages."""

    KPI_SPECS = (
        ("rsrp_dbm", "RSRP (dBm)", "tab:blue", RSRP_MIN_DBM, RSRP_MAX_DBM),
        ("rsrq_db", "RSRQ (dB)", "tab:orange", RSRQ_MIN_DB, RSRQ_MAX_DB),
        ("active_users", "Active Users", "tab:green", 0, 500),
        ("prb_utilization_pct", "PRB Utilization (%)", "tab:red", 0, 100),
    )

    def __init__(self, history_points: int, num_cells: int = NUM_CELLS) -> None:
        self.history_points = history_points
        self.timestamps: list[datetime] = []
        self.series: dict[str, list[float]] = {
            spec[0]: [] for spec in self.KPI_SPECS
        }

        self.fig, self.axes = plt.subplots(2, 2, figsize=(12, 7), sharex=True)
        self.fig.suptitle(
            f"5G RAN Network KPIs — Active-User Weighted Average ({num_cells} cells)",
            fontsize=13,
        )
        self.lines: dict[str, plt.Line2D] = {}
        self.threshold_lines: list[plt.Line2D] = []

        flat_axes = self.axes.flatten()
        for ax, (key, label, color, ymin, ymax) in zip(
            flat_axes, self.KPI_SPECS, strict=True
        ):
            (line,) = ax.plot([], [], color=color, linewidth=1.8, label=label)
            self.lines[key] = line
            ax.set_ylabel(label)
            ax.grid(True, alpha=0.3)
            if ymin is not None and ymax is not None:
                ax.set_ylim(ymin, ymax)

            if key == "rsrq_db":
                threshold = ax.axhline(
                    RSRQ_CRITICAL_DB,
                    color="crimson",
                    linestyle="--",
                    linewidth=1.0,
                    label=f"O-RAN critical ({RSRQ_CRITICAL_DB} dB)",
                )
                self.threshold_lines.append(threshold)
            if key == "prb_utilization_pct":
                threshold = ax.axhline(
                    PRB_CRITICAL_PCT,
                    color="crimson",
                    linestyle="--",
                    linewidth=1.0,
                    label=f"O-RAN critical ({PRB_CRITICAL_PCT}%)",
                )
                self.threshold_lines.append(threshold)

            ax.legend(loc="upper left", fontsize=8)

        flat_axes[-1].set_xlabel("Simulation time (UTC)")

    def update(self, snapshot: NetworkSnapshot) -> None:
        self.timestamps.append(snapshot.timestamp)
        for key, *_ in self.KPI_SPECS:
            if key == "active_users":
                value = snapshot.weighted_average("active_users")
            elif key == "prb_utilization_pct":
                value = snapshot.weighted_average("prb_utilization_pct")
            elif key == "rsrp_dbm":
                value = snapshot.weighted_average("rsrp_dbm")
            else:
                value = snapshot.weighted_average("rsrq_db")

            self.series[key].append(value)

        if len(self.timestamps) > self.history_points:
            self.timestamps = self.timestamps[-self.history_points :]
            for key in self.series:
                self.series[key] = self.series[key][-self.history_points :]

        x_values = [ts.timestamp() for ts in self.timestamps]
        for key, *_ in self.KPI_SPECS:
            self.lines[key].set_data(x_values, self.series[key])

        for ax in self.axes.flatten():
            ax.relim()
            ax.autoscale_view(scalex=True, scaley=False)

    def show(self) -> None:
        plt.tight_layout()
        plt.show()


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Simulate 5G RAN telemetry for 20 cells and persist to SQLite.",
    )
    parser.add_argument(
        "--db-path",
        type=Path,
        default=Path(os.environ.get("RAN_TELEMETRY_DB", DEFAULT_DB_PATH)),
        help="SQLite database file path (default: rapp-manager-models/data/ran_telemetry.db)",
    )
    parser.add_argument(
        "--tick-seconds",
        type=float,
        default=float(os.environ.get("RAN_SIM_TICK_SECONDS", DEFAULT_TICK_SECONDS)),
        help="Seconds between simulation ticks (default: 1.0)",
    )
    parser.add_argument(
        "--no-chart",
        action="store_true",
        help="Run headless: update database only, no matplotlib window",
    )
    return parser.parse_args(argv)


def run_headless(config: SimulatorConfig, database: RanTelemetryDatabase) -> None:
    simulator = RanCellSimulator(config)
    logger.info(
        "Headless mode: writing telemetry every %.1fs to %s",
        config.tick_seconds,
        config.db_path,
    )
    try:
        while True:
            snapshot = simulator.advance()
            database.persist_snapshot(snapshot)
            w_rsrp = snapshot.weighted_average("rsrp_dbm")
            w_rsrq = snapshot.weighted_average("rsrq_db")
            w_users = snapshot.weighted_average("active_users")
            w_prb = snapshot.weighted_average("prb_utilization_pct")
            logger.info(
                "tick | wRSRP=%.1f dBm | wRSRQ=%.1f dB | wUsers=%.0f | wPRB=%.1f%%",
                w_rsrp,
                w_rsrq,
                w_users,
                w_prb,
            )
            time.sleep(config.tick_seconds)
    except KeyboardInterrupt:
        logger.info("Simulation stopped.")


def run_with_chart(config: SimulatorConfig, database: RanTelemetryDatabase) -> None:
    simulator = RanCellSimulator(config)
    chart = WeightedKpiChart(config.chart_history, config.num_cells)

    def on_tick(_frame_index: int) -> None:
        snapshot = simulator.advance()
        database.persist_snapshot(snapshot)
        chart.update(snapshot)

    interval_ms = int(config.tick_seconds * 1000)
    _anim = animation.FuncAnimation(
        chart.fig,
        on_tick,
        interval=interval_ms,
        cache_frame_data=False,
    )
    chart.show()


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    config = SimulatorConfig(
        db_path=args.db_path,
        tick_seconds=args.tick_seconds,
    )

    cell_ids = [f"cell-{i}" for i in range(1, config.num_cells + 1)]
    database = RanTelemetryDatabase(config.db_path)
    database.seed_cells(cell_ids)
    database.prune_inactive_latest_cells(cell_ids)

    logger.info(
        "Starting 5G RAN simulator for %d cells | DB=%s | tick=%.1fs",
        config.num_cells,
        config.db_path,
        config.tick_seconds,
    )

    try:
        if args.no_chart:
            run_headless(config, database)
        else:
            run_with_chart(config, database)
    finally:
        database.close()

    return 0


if __name__ == "__main__":
    sys.exit(main())
