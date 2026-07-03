"""Application configuration loaded from environment variables."""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "rapp-starter-kit"
    host: str = "0.0.0.0"
    port: int = 8080
    log_level: str = "info"

    # Non-RT RIC / R1 data source
    nonrtric_base_url: str = "http://localhost:8081"
    r1_cm_path_prefix: str = "/ran-oam-cm/v1"
    r1_poll_interval_seconds: int = 30

    # R1 PM data notification (pushed from SMO)
    pm_notification_source: str = "SMO"
    pm_rsrp_min_dbm: float = -140.0
    pm_rsrp_max_dbm: float = -44.0
    pm_sinr_min_db: float = -20.0
    pm_sinr_max_db: float = 40.0
    pm_prb_utilization_max_pct: float = 100.0

    # A1 Policy Management Service
    a1_pms_base_url: str = "http://localhost:8082"
    a1_policy_type_id: str = "20008"
    a1_service_id: str = "rapp-starter-kit"

    # rApp identity
    rapp_name: str = "rapp-starter-kit"
    rapp_version: str = "0.1.0"


@lru_cache
def get_settings() -> Settings:
    return Settings()
