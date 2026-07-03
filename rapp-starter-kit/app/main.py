"""FastAPI server with R1 ingestion and A1 policy routing."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from fastapi import Depends, FastAPI, HTTPException
from pydantic import BaseModel, Field, model_validator

from app.config import Settings, get_settings
from app.services.policy_engine import PolicyEngine
from app.services.r1_consumer import R1Consumer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# --- PM metric models (R1 data notification from SMO) ---


class RsrpMetric(BaseModel):
    """Reference Signal Received Power."""

    value: float = Field(..., description="RSRP in dBm")
    unit: str = "dBm"


class SinrMetric(BaseModel):
    """Signal-to-Interference-plus-Noise Ratio."""

    value: float = Field(..., description="SINR in dB")
    unit: str = "dB"


class PrbUtilizationMetric(BaseModel):
    """Physical Resource Block utilization."""

    value: float = Field(..., description="PRB utilization percentage")
    unit: str = "%"


class CellPmMetrics(BaseModel):
    """PM metrics for a single cell."""

    cell_id: str = Field(..., min_length=1)
    moi: str | None = Field(None, description="Managed Object Instance path")
    rsrp: RsrpMetric | None = None
    sinr: SinrMetric | None = None
    prb_utilization: PrbUtilizationMetric | None = None

    @model_validator(mode="after")
    def at_least_one_metric(self) -> CellPmMetrics:
        if not any((self.rsrp, self.sinr, self.prb_utilization)):
            raise ValueError(
                "At least one of rsrp, sinr, or prb_utilization must be provided"
            )
        return self


class PmDataNotification(BaseModel):
    """Incoming PM data notification from the SMO."""

    metrics: list[CellPmMetrics] = Field(..., min_length=1)
    notification_id: str | None = None
    timestamp: datetime | None = None
    source: str = "SMO"


class PmNotificationResponse(BaseModel):
    status: str
    notification_id: str
    cells_processed: int


# --- Other request models ---


class SnapshotRequest(BaseModel):
    moi_paths: list[str] = Field(
        default=["SubNetwork=SN1,GNBDUFunction=1"],
        min_length=1,
    )


class PolicyRequest(BaseModel):
    policy_data: dict[str, Any] | None = None
    policy_id: str | None = None


class HealthResponse(BaseModel):
    status: str
    rapp: str


def _pm_bounds(cfg: Settings) -> dict[str, tuple[float, float]]:
    return {
        "rsrp": (cfg.pm_rsrp_min_dbm, cfg.pm_rsrp_max_dbm),
        "sinr": (cfg.pm_sinr_min_db, cfg.pm_sinr_max_db),
        "prb_utilization": (0.0, cfg.pm_prb_utilization_max_pct),
    }


def _validate_pm_metric_ranges(
    notification: PmDataNotification, cfg: Settings
) -> None:
    bounds = _pm_bounds(cfg)
    for cell in notification.metrics:
        for name, metric in (
            ("rsrp", cell.rsrp),
            ("sinr", cell.sinr),
            ("prb_utilization", cell.prb_utilization),
        ):
            if metric is None:
                continue
            lo, hi = bounds[name]
            if not lo <= metric.value <= hi:
                raise HTTPException(
                    status_code=422,
                    detail=(
                        f"Cell {cell.cell_id}: {name} value {metric.value} "
                        f"outside allowed range [{lo}, {hi}]"
                    ),
                )


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    logger.info("Starting %s v%s", settings.rapp_name, settings.rapp_version)
    yield
    get_settings.cache_clear()


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()

    app = FastAPI(
        title=settings.app_name,
        version=settings.rapp_version,
        description="O-RAN rApp starter — R1 consumer and A1 policy publisher",
        lifespan=lifespan,
    )

    @app.get("/health", response_model=HealthResponse)
    async def health() -> HealthResponse:
        return HealthResponse(status="ok", rapp=settings.rapp_name)

    # --- R1 routes (data ingestion) ---

    @app.post("/r1/data-notification", response_model=PmNotificationResponse)
    async def r1_pm_data_notification(
        body: PmDataNotification,
        cfg: Settings = Depends(get_settings),
    ) -> PmNotificationResponse:
        """Receive PM metrics (RSRP, SINR, PRB utilization) pushed from the SMO."""
        _validate_pm_metric_ranges(body, cfg)

        payload = body.model_dump(mode="json")
        if payload.get("notification_id") is None:
            payload["notification_id"] = str(uuid4())
        if payload.get("timestamp") is None:
            payload["timestamp"] = datetime.now(timezone.utc).isoformat()
        payload["source"] = body.source or cfg.pm_notification_source

        async with R1Consumer(cfg) as consumer:
            result = await consumer.process_pm_notification(payload)

        return PmNotificationResponse(**result)

    @app.get("/r1/cm/{moi_path:path}")
    async def r1_read_configuration(
        moi_path: str,
        cfg: Settings = Depends(get_settings),
    ) -> dict[str, Any]:
        async with R1Consumer(cfg) as consumer:
            try:
                return await consumer.read_configuration(moi_path)
            except Exception as exc:
                raise HTTPException(status_code=502, detail=str(exc)) from exc

    @app.post("/r1/snapshot")
    async def r1_snapshot(
        body: SnapshotRequest,
        cfg: Settings = Depends(get_settings),
    ) -> dict[str, Any]:
        async with R1Consumer(cfg) as consumer:
            return await consumer.fetch_snapshot(body.moi_paths)

    # --- A1 routes (declarative policy output) ---

    @app.post("/a1/policies")
    async def a1_publish_policy(
        body: PolicyRequest,
        cfg: Settings = Depends(get_settings),
    ) -> dict[str, Any]:
        async with R1Consumer(cfg) as consumer, PolicyEngine(cfg) as engine:
            if body.policy_data is not None:
                policy_data = body.policy_data
            else:
                snapshot = await consumer.fetch_snapshot(
                    ["SubNetwork=SN1,GNBDUFunction=1"]
                )
                policy_data = engine.derive_from_r1_snapshot(snapshot)

            policy = engine.build_policy(policy_data, policy_id=body.policy_id)
            try:
                return await engine.publish_policy(policy)
            except Exception as exc:
                raise HTTPException(status_code=502, detail=str(exc)) from exc

    @app.delete("/a1/policies/{policy_id}")
    async def a1_delete_policy(
        policy_id: str,
        cfg: Settings = Depends(get_settings),
    ) -> dict[str, str]:
        async with PolicyEngine(cfg) as engine:
            try:
                await engine.delete_policy(policy_id)
            except Exception as exc:
                raise HTTPException(status_code=502, detail=str(exc)) from exc
        return {"status": "deleted", "policy_id": policy_id}

    return app


app = create_app()
