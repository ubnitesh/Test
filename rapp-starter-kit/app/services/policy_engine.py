"""Logic to build and publish A1 declarative policies to the Near-RT RIC."""

from __future__ import annotations

import logging
from typing import Any
from uuid import uuid4

import httpx

from app.config import Settings

logger = logging.getLogger(__name__)


class PolicyEngine:
    """Translates rApp decisions into A1 policy payloads and publishes them."""

    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None) -> None:
        self._settings = settings
        self._client = client
        self._owns_client = client is None

    async def __aenter__(self) -> PolicyEngine:
        if self._client is None:
            self._client = httpx.AsyncClient(
                base_url=self._settings.a1_pms_base_url,
                timeout=httpx.Timeout(30.0),
            )
        return self

    async def __aexit__(self, *args: object) -> None:
        if self._owns_client and self._client is not None:
            await self._client.aclose()

    def build_policy(
        self,
        policy_data: dict[str, Any],
        *,
        policy_id: str | None = None,
    ) -> dict[str, Any]:
        """Wrap domain data in an A1 declarative policy envelope."""
        return {
            "policy_id": policy_id or str(uuid4()),
            "policy_type_id": self._settings.a1_policy_type_id,
            "service_id": self._settings.a1_service_id,
            "status_notification_uri": f"/a1/policies/{{policy_id}}/status",
            "policy_data": policy_data,
        }

    def derive_from_r1_snapshot(self, snapshot: dict[str, Any]) -> dict[str, Any]:
        """Example policy derivation — replace with your rApp logic."""
        moi_count = len(snapshot)
        return {
            "scope": {"moi_count": moi_count},
            "action": "monitor",
            "threshold": 0.85,
            "source": self._settings.rapp_name,
        }

    async def publish_policy(self, policy: dict[str, Any]) -> dict[str, Any]:
        """POST a declarative policy to the A1 Policy Management Service."""
        assert self._client is not None
        policy_id = policy["policy_id"]
        url = f"/a1-p/v2/policies/{policy_id}"
        logger.info("Publishing A1 policy %s", policy_id)
        response = await self._client.put(url, json=policy)
        response.raise_for_status()
        return response.json()

    async def delete_policy(self, policy_id: str) -> None:
        """Remove a policy from the Near-RT RIC."""
        assert self._client is not None
        url = f"/a1-p/v2/policies/{policy_id}"
        response = await self._client.delete(url)
        response.raise_for_status()
