"""FastAPI server with R1 ingestion and A1 policy routing."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from typing import Any

from fastapi import Depends, FastAPI, HTTPException, status
from pydantic import BaseModel, Field

from app.config import Settings, get_settings
from app.services.policy_engine import PolicyEngine, evaluate_metrics
from app.services.r1_consumer import R1Consumer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("rApp-Starter")


class CellMetrics(BaseModel):
    cell_id: str = Field(..., description="Target Cell Identifier")
    rsrp: float = Field(..., description="Reference Signal Received Power")
    sinr: float = Field(..., description="Signal-to-Interference-plus-Noise Ratio")
    prb_utilization: float = Field(
        ..., description="Physical Radio Block utilization percentage"
    )


class SnapshotRequest(BaseModel):
    moi_paths: list[str] = Field(
        default=["SubNetwork=SN1,GNBDUFunction=1"],
        min_length=1,
    )


class PolicyRequest(BaseModel):
    policy_data: dict[str, Any] | None = None
    policy_id: str | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    logger.info("Starting %s v%s", settings.rapp_name, settings.rapp_version)
    yield
    get_settings.cache_clear()


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()

    app = FastAPI(
        title="O-RAN Non-RT RIC rApp Starter Kit",
        version="1.0.0",
        description="O-RAN rApp starter — R1 consumer and A1 policy publisher",
        lifespan=lifespan,
    )

    @app.get("/health", status_code=status.HTTP_200_OK)
    async def health_check() -> dict[str, str]:
        return {"status": "healthy"}

    @app.post("/r1/data-notification", status_code=status.HTTP_202_ACCEPTED)
    async def receive_ran_data(metrics: CellMetrics) -> dict[str, Any]:
        logger.info("Received RAN data for Cell: %s", metrics.cell_id)
        try:
            policy_triggered = await evaluate_metrics(metrics)
            return {
                "status": "processed",
                "cell_id": metrics.cell_id,
                "a1_policy_generated": policy_triggered,
            }
        except Exception as e:
            logger.error("Failed to process metrics: %s", e)
            raise HTTPException(
                status_code=500, detail="Internal processing error"
            ) from e

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
