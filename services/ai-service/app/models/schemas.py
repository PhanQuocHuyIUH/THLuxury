from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

class ChatRequest(BaseModel):
    sessionId: str
    userId: Optional[str] = None
    message: str

class SuggestedProduct(BaseModel):
    productId: str
    tenSP: str
    giaSP: float
    imageUrl: str

class ChatResponse(BaseModel):
    reply: str
    suggestedProducts: Optional[List[SuggestedProduct]] = []
    intent: str

class ProductEvent(BaseModel):
    id: str
    maSP: str
    tenSP: str
    loaiSP: str
    giaGiamGia: float
    giaBanDau: float
    mauDa: Optional[str] = None
    hamLuong: Optional[str] = None
