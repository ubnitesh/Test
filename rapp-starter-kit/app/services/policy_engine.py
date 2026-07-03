"""Logic to build and publish A1 declarative policies to the Near-RT RIC."""

from __future__ import annotations

import logging
from typing import Any
from uuid import uuid4

import httpx

from app.config import Settings, get_settings

logger = logging.getLogger(__name__)

TRAFFIC_STEERING_ACTION = "TRAFFIC_STEERING"


def _extract_prb_utilization(cell: dict[str, Any]) -> float | None:
    """Return PRB utilization percentage from a cell metrics dict, if present."""
    prb = cell.get("prb_utilization")
    if prb is None:
        return None
    if isinstance(prb, dict):
        return float(prb["value"])
    return float(prb)


class PolicyEngine:
    """Translates rApp decisions into A1 policy payloads and publishes them."""

    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None) -> None:
        self._settings = settings
        self._client = client
        self._owns_client = client is None
        self._termination_client: httpx.AsyncClient | None = None
        self._owns_termination_client = True

    async def __aenter__(self) -> PolicyEngine:
        if self._client is None:
            self._client = httpx.AsyncClient(
                base_url=self._settings.a1_pms_base_url,
                timeout=httpx.Timeout(30.0),
            )
        if self._termination_client is None:
            self._termination_client = httpx.AsyncClient(
                base_url=self._settings.a1_termination_base_url,
                timeout=httpx.Timeout(30.0),
            )
        return self

    async def __aexit__(self, *args: object) -> None:
        if self._owns_client and self._client is not None:
            await self._client.aclose()
        if self._owns_termination_client and self._termination_client is not None:
            await self._termination_client.aclose()

    def build_policy(
        self,
        policy_data: dict[str, Any],
        *,
        policy_id: str | None = None,
    ) -> dict[str, Any]:
        """Wrap domain data in an O-RAN A1 declarative policy envelope."""
        pid = policy_id or str(uuid4())
        return {
            "policy_id": pid,
            "policy_type_id": self._settings.a1_policy_type_id,
            "service_id": self._settings.a1_service_id,
            "status_notification_uri": (
                f"{self._settings.a1_status_notification_base.rstrip('/')}"
                f"/a1/policies/{pid}/status"
            ),
            "policy_data": policy_data,
        }

    def build_traffic_steering_policy_data(
        self,
        *,
        cell_id: str,
        prb_utilization_pct: float,
        moi: str | None = None,
    ) -> dict[str, Any]:
        """Build O-RAN A1 policy_data to steer traffic away from an overloaded cell."""
        scope: dict[str, Any] = {"cell_id": cell_id}
        if moi:
            scope["moi"] = moi

        return {
            "scope": scope,
            "action": TRAFFIC_STEERING_ACTION,
            "trigger": {
                "metric": "prb_utilization",
                "threshold_pct": self._settings.a1_prb_steering_threshold_pct,
                "observed_value_pct": prb_utilization_pct,
            },
            "steering": {
                "mode": "offload",
                "target": "neighbor_cells",
                "reason": "high_prb_utilization",
            },
            "source": self._settings.rapp_name,
        }

    def evaluate_metrics(
        self, metrics: list[dict[str, Any]]
    ) -> list[dict[str, Any]]:
        """
        Evaluate incoming PM metrics and return A1 policy envelopes for every
        cell whose PRB utilization exceeds the configured threshold.
        """
        threshold = self._settings.a1_prb_steering_threshold_pct
        policies: list[dict[str, Any]] = []

        for cell in metrics:
            cell_id = cell.get("cell_id")
            if not cell_id:
                logger.warning("Skipping cell metric with missing cell_id: %s", cell)
                continue

            prb_value = _extract_prb_utilization(cell)
            if prb_value is None:
                continue

            if prb_value <= threshold:
                logger.debug(
                    "Cell %s PRB utilization %.1f%% within threshold %.1f%%",
                    cell_id,
                    prb_value,
                    threshold,
                )
                continue

            logger.info(
                "Cell %s PRB utilization %.1f%% exceeds %.1f%% — "
                "building traffic-steering A1 policy",
                cell_id,
                prb_value,
                threshold,
            )
            policy_data = self.build_traffic_steering_policy_data(
                cell_id=cell_id,
                prb_utilization_pct=prb_value,
                moi=cell.get("moi"),
            )
            policies.append(self.build_policy(policy_data))

        return policies

    async def post_policy_to_a1_termination(
        self, policy: dict[str, Any]
    ) -> dict[str, Any]:
        """POST a declarative policy to the Non-RT RIC A1 termination endpoint."""
        assert self._termination_client is not None
        path = self._settings.a1_termination_path.lstrip("/")
        policy_id = policy["policy_id"]
        logger.info(
            "POST A1 traffic-steering policy %s for cell %s",
            policy_id,
            policy.get("policy_data", {}).get("scope", {}).get("cell_id"),
        )
        response = await self._termination_client.post(path, json=policy)
        response.raise_for_status()
        if response.content:
            return response.json()
        return {"policy_id": policy_id, "status": "accepted"}

    async def evaluate_and_publish_metrics(
        self, metrics: list[dict[str, Any]]
    ) -> list[dict[str, Any]]:
        """Evaluate PM metrics and POST traffic-steering policies for overloaded cells."""
        policies = self.evaluate_metrics(metrics)
        results: list[dict[str, Any]] = []
        for policy in policies:
            results.append(await self.post_policy_to_a1_termination(policy))
        return results

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
        """PUT a declarative policy to the A1 Policy Management Service."""
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


async def evaluate_metrics(metrics: Any) -> bool:
    """Evaluate cell metrics and publish A1 policies when thresholds are exceeded."""
    settings = get_settings()
    cell_dict = metrics.model_dump() if hasattr(metrics, "model_dump") else dict(metrics)

    async with PolicyEngine(settings) as engine:
        policies = engine.evaluate_metrics([cell_dict])
        if not policies:
            return False
        for policy in policies:
            if settings.a1_publish_enabled:
                await engine.post_policy_to_a1_termination(policy)
            else:
                logger.info(
                    "A1 publish disabled — policy generated but not sent: %s",
                    policy["policy_id"],
                )
        return True
