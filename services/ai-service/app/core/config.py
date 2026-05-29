from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    app_name: str = "THLuxury AI Service"
    redis_host: str = "localhost"
    redis_port: int = 6379
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "admin"
    rabbitmq_pass: str = "admin"
    gemini_api_key: Optional[str] = None
    gemini_model: str = "gemini-1.5-flash"
    catalog_base_url: str = "http://catalog-service:8082"

    class Config:
        env_file = ".env"

settings = Settings()
