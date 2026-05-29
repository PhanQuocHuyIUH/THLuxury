from fastapi import APIRouter, HTTPException, Depends
from typing import List
from prometheus_client import Counter
from app.models.schemas import ChatRequest, ChatResponse, SuggestedProduct
from app.services.recommendation import recommendation_engine
from app.services.chatbot import chatbot_service

router = APIRouter()

# Business metric: số request chatbot theo intent → Prometheus `ai_chat_requests_total{intent=...}`
AI_CHAT_REQUESTS = Counter("ai_chat_requests", "AI chat requests grouped by intent", ["intent"])

@router.get("/recommend/{userId}")
async def get_recommendations(userId: str, limit: int = 8):
    try:
        products = recommendation_engine.get_recommendations_for_user(userId, limit)
        return products
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/similar/{productId}")
async def get_similar_products(productId: str, limit: int = 6):
    try:
        products = recommendation_engine.get_similar_products(productId, limit)
        return products
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    try:
        response = await chatbot_service.chat(
            session_id=request.sessionId,
            user_id=request.userId,
            message=request.message
        )
        intent = getattr(response, "intent", None) or "UNKNOWN"
        AI_CHAT_REQUESTS.labels(intent=intent).inc()
        return response
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
