import logging
import asyncio
from fastapi import FastAPI
from contextlib import asynccontextmanager

from prometheus_fastapi_instrumentator import Instrumentator

from app.core.config import settings
from app.api.endpoints import router as ai_router
from app.services.recommendation import recommendation_engine
from app.services.rabbitmq import start_rabbitmq_consumer

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# Ensure proper context manager import for lifespan
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting AI Service...")
    
    # Initialize Recommendation Engine (fetches products)
    await recommendation_engine.initialize()
    
    # Start RabbitMQ consumer as a background task
    asyncio.create_task(start_rabbitmq_consumer())
    
    yield
    # Shutdown
    logger.info("Shutting down AI Service...")

app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    lifespan=lifespan
)

app.include_router(ai_router, prefix="/api/ai", tags=["ai"])

# Expose Prometheus metrics at /metrics (default HTTP metrics + custom counters)
Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)

@app.get("/actuator/health")
async def health_check():
    return {"status": "UP"}
