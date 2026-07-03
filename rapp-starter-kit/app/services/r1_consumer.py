"""Handles data ingestion from the Non-RT RIC via the R1 interface."""

from __future__ import annotations

import logging
from typing import Any

import httpx

from app.config import Settings

logger = logging.getLogger(__name__)


class R1Consumer:
    """Consumes configuration and telemetry exposed by the Non-RT RIC."""

    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None) -> None:
        self._settings = settings
        self._client = client
        self._owns_client = client is None

    async def __aenter__(self) -> R1Consumer:
        if self._client is None:
            self._client = httpx.AsyncClient(
                base_url=self._settings.nonrtric_base_url,
                timeout=httpx.Timeout(30.0),
            )
        return self

    async def __aexit__(self, *args: object) -> None:
        if self._owns_client and self._client is not None:
            await self._client.aclose()

    def _cm_url(self, moi_path: str) -> str:
        prefix = self._settings.r1_cm_path_prefix.rstrip("/")
        return f"{prefix}/{moi_path.lstrip('/')}"

    async def read_configuration(self, moi_path: str) -> dict[str, Any]:
        """GET configuration data for a managed object instance (MOI)."""
        assert self._client is not None
        url = self._cm_url(moi_path)
        logger.debug("R1 CM read: %s", url)
        response = await self._client.get(url)
        response.raise_for_status()
        return response.json()

    async def fetch_snapshot(self, moi_paths: list[str]) -> dict[str, Any]:
        """Fetch configuration for multiple MOI paths into a single snapshot."""
        snapshot: dict[str, Any] = {}
        for path in moi_paths:
            try:
                snapshot[path] = await self.read_configuration(path)
            except httpx.HTTPStatusError as exc:
                logger.warning("Failed to read MOI %s: %s", path, exc)
                snapshot[path] = {"error": str(exc)}
        return snapshot

    async def process_pm_notification(
        self, notification: dict[str, Any]
    ) -> dict[str, Any]:
        """Accept and process PM metrics pushed from the SMO via R1."""
        from uuid import uuid4

        notification_id = notification.get("notification_id") or str(uuid4())
        metrics: list[dict[str, Any]] = notification.get("metrics", [])
        source = notification.get("source", self._settings.pm_notification_source)

        logger.info(
            "PM data notification %s: %d cell(s) from %s",
            notification_id,
            len(metrics),
            source,
        )

        for cell in metrics:
            cell_id = cell.get("cell_id", "unknown")
            rsrp = cell.get("rsrp")
            sinr = cell.get("sinr")
            prb = cell.get("prb_utilization")
            logger.debug(
                "Cell %s — RSRP=%s SINR=%s PRB util=%s",
                cell_id,
                rsrp.get("value") if isinstance(rsrp, dict) else rsrp,
                sinr.get("value") if isinstance(sinr, dict) else sinr,
                prb.get("value") if isinstance(prb, dict) else prb,
            )

        return {
            "status": "accepted",
            "notification_id": notification_id,
            "cells_processed": len(metrics),
        }
