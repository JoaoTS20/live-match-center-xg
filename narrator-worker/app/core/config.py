from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "match-events"
    kafka_group_id: str = "xg-narrator-group"
    match_service_url: str = "http://localhost:8080"


settings = Settings()
