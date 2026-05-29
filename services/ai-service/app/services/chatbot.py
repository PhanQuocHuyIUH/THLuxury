import json
import logging
import httpx
import random
from app.core.config import settings
from app.models.schemas import ChatResponse, SuggestedProduct
from app.services.recommendation import recommendation_engine
import redis.asyncio as redis

logger = logging.getLogger(__name__)

class ChatbotService:
    def __init__(self):
        self.redis_client = redis.Redis(
            host=settings.redis_host, 
            port=settings.redis_port, 
            decode_responses=True
        )

    async def chat(self, session_id: str, user_id: str, message: str) -> ChatResponse:
        # Determine intent
        intent = self._classify_intent(message)
        
        # Find relevant products dynamically based on user message instead of a static list
        relevant_products = self._search_products(message, limit=3)
        
        # If no Gemini Key, fallback to rule-based completely
        if not settings.gemini_api_key or settings.gemini_api_key.strip() == "":
            return self._fallback_reply(intent, relevant_products)

        # Load history
        history_key = f"chat:{session_id}"
        history = await self.redis_client.lrange(history_key, 0, 5)
        
        # Build prompt with RAG context
        context = self._build_context(relevant_products)
        prompt = self._build_prompt(context, history, message)

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                url = f"https://generativelanguage.googleapis.com/v1beta/models/{settings.gemini_model}:generateContent?key={settings.gemini_api_key}"
                payload = {
                    "contents": [{"parts": [{"text": prompt}]}],
                    "generationConfig": {
                        "temperature": 0.2,
                        "maxOutputTokens": 200
                    }
                }
                resp = await client.post(url, json=payload)
                if resp.status_code == 200:
                    data = resp.json()
                    candidates = data.get("candidates", [])
                    if candidates:
                        reply_text = candidates[0]["content"]["parts"][0]["text"]
                        
                        # Save to history
                        await self.redis_client.rpush(history_key, f"User: {message}")
                        await self.redis_client.rpush(history_key, f"AI: {reply_text}")
                        await self.redis_client.ltrim(history_key, -10, -1) # Keep last 10 messages
                        
                        # Extract suggested products based on intent
                        suggested_products = self._get_suggestions_if_applicable(intent, relevant_products)
                        
                        return ChatResponse(reply=reply_text, suggestedProducts=suggested_products, intent=intent)
                else:
                    logger.error(f"Gemini API Error: {resp.text}")
                    return self._fallback_reply(intent, relevant_products)

        except Exception as e:
            logger.error(f"Error calling Gemini API: {e}")
            return self._fallback_reply(intent, relevant_products)

    def _classify_intent(self, message: str) -> str:
        msg_lower = message.lower()
        if any(w in msg_lower for w in ["mua", "giá", "sản phẩm", "trang sức", "kim cương", "nhẫn", "vòng", "dây chuyền", "bông tai"]):
            return "PRODUCT_SEARCH"
        elif any(w in msg_lower for w in ["đơn hàng", "tình trạng", "đang ở đâu", "bao giờ giao"]):
            return "ORDER_STATUS"
        elif any(w in msg_lower for w in ["cửa hàng", "chi nhánh", "địa chỉ", "ở đâu"]):
            return "STORE_INFO"
        elif any(w in msg_lower for w in ["chào", "hello", "hi"]):
            return "GREETING"
        return "GENERAL_QA"

    def _search_products(self, query: str, limit: int = 3) -> list:
        if not recommendation_engine.products:
            return []
            
        query_lower = query.lower()
        keywords = [w for w in query_lower.split() if len(w) > 2]
        
        scored_products = []
        for p in recommendation_engine.products.values():
            score = 0
            name = p.get("ten_sp", "").lower()
            category = p.get("loai_sp", "").lower()
            
            # Simple scoring based on keyword match
            if any(k in name for k in keywords):
                score += 2
            if any(k in category for k in keywords):
                score += 1
                
            if score > 0:
                scored_products.append((score, p))
        
        # Sort by score descending
        scored_products.sort(key=lambda x: x[0], reverse=True)
        results = [p for score, p in scored_products][:limit]
        
        # If no semantic matches, return random products instead of always the first 3
        if not results:
            all_prods = list(recommendation_engine.products.values())
            results = random.sample(all_prods, min(limit, len(all_prods)))
            
        return results

    def _fallback_reply(self, intent: str, relevant_products: list) -> ChatResponse:
        suggested = []
        if intent == "PRODUCT_SEARCH":
            reply = "Bạn có thể tham khảo một số sản phẩm nổi bật của chúng tôi dưới đây."
            suggested = self._get_suggestions_if_applicable(intent, relevant_products)
        elif intent == "ORDER_STATUS":
            reply = "Để kiểm tra đơn hàng, bạn vui lòng truy cập vào mục Tài khoản > Đơn hàng của tôi."
        elif intent == "STORE_INFO":
            reply = "Chúng tôi có các chi nhánh tại TP.HCM, Hà Nội và Đà Nẵng. Bạn có thể xem chi tiết ở chân trang web."
        elif intent == "GREETING":
            reply = "Chào bạn! THLuxury có thể giúp gì cho bạn hôm nay?"
        else:
            reply = "Xin lỗi, hệ thống AI hiện đang xử lý chậm. Xin vui lòng thử lại sau hoặc liên hệ CSKH."
            
        return ChatResponse(reply=reply, suggestedProducts=suggested, intent=intent)

    def _build_context(self, relevant_products: list) -> str:
        context_str = json.dumps([{
            "ten_sp": p["ten_sp"], "gia": p["gia"], "loai_sp": p["loai_sp"]
        } for p in relevant_products], ensure_ascii=False)
        return context_str

    def _build_prompt(self, context: str, history: list, message: str) -> str:
        history_str = "\n".join(history)
        return f"""Bạn là trợ lý ảo của THLuxury — chuyên trang sức cao cấp.
Bạn luôn trả lời ngắn gọn, lịch sự, chuyên nghiệp bằng tiếng Việt.
Dưới đây là thông tin một số sản phẩm phù hợp với ngữ cảnh hiện tại để giới thiệu cho khách:
{context}

Lịch sử trò chuyện:
{history_str}

User: {message}
Trợ lý AI:"""

    def _get_suggestions_if_applicable(self, intent: str, relevant_products: list) -> list:
        # Only attach UI product cards if the user is explicitly searching or talking about products
        if intent == "PRODUCT_SEARCH" and relevant_products:
            suggestions = []
            for p in relevant_products:
                images = p.get("images", [])
                image_url = images[0].get("imageUrl") if images and isinstance(images[0], dict) else "/5t-logo.png"
                suggestions.append(SuggestedProduct(
                    productId=p["id"],
                    tenSP=p["ten_sp"],
                    giaSP=p["gia"],
                    imageUrl=image_url
                ))
            return suggestions
        return []

chatbot_service = ChatbotService()