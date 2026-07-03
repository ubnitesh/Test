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
