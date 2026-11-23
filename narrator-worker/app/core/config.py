from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "match-events"
    kafka_commentary_topic: str = "match-commentary"
    kafka_group_id: str = "xg-narrator-group"
    match_service_url: str = "http://localhost:8080"
    gemini_api_key: str = ""
    gemini_model: str = "gemini-3.8-flash"

settings = Settings()
