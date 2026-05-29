import logging
import httpx
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import OneHotEncoder, MinMaxScaler
from app.core.config import settings

logger = logging.getLogger(__name__)

class RecommendationEngine:
    def __init__(self):
        self.products = {}
        self.product_ids = []
        self.feature_matrix = None

    async def initialize(self):
        """Fetch all products from catalog service and build matrix"""
        logger.info("Initializing recommendation engine...")
        try:
            async with httpx.AsyncClient() as client:
                url = f"{settings.catalog_base_url}/api/products?size=1000"
                response = await client.get(url)
                if response.status_code == 200:
                    data = response.json()
                    # Check if data contains 'content' (Page format)
                    items = data.get("content", data) if isinstance(data, dict) else data
                    if isinstance(items, list):
                        for item in items:
                            self.add_or_update_product(item, rebuild=False)
                        self.build_matrix()
                        logger.info(f"Loaded {len(self.products)} products into recommendation engine.")
        except Exception as e:
            logger.error(f"Failed to fetch catalog on startup: {e}")

    def add_or_update_product(self, product: dict, rebuild=True):
        pid = product.get("id") or product.get("productId")
        if not pid:
            return
        
        # Ensure safe format
        self.products[pid] = {
            "id": pid,
            "ten_sp": product.get("tenSP", product.get("ten_sp", "")),
            "loai_sp": product.get("loaiSP", product.get("loai_sp", "")),
            "mau_da": product.get("mauDa", product.get("mau_da", "")),
            "ham_luong": product.get("hamLuong", product.get("ham_luong", "")),
            "gia": float(product.get("giaGiamGia", 0) or product.get("giaBanDau", 0) or product.get("gia", 0)),
            "images": product.get("images", product.get("productImages", []))
        }
        
        if rebuild:
            self.build_matrix()

    def remove_product(self, product_id: str):
        if product_id in self.products:
            del self.products[product_id]
            self.build_matrix()
            logger.info(f"Removed product {product_id} from recommendation engine.")

    def build_matrix(self):
        if not self.products:
            self.feature_matrix = None
            self.product_ids = []
            return

        self.product_ids = list(self.products.keys())
        
        loai_sp_list = [[p.get("loai_sp", "")] for p in self.products.values()]
        mau_da_list = [[p.get("mau_da", "")] for p in self.products.values()]
        ham_luong_list = [[p.get("ham_luong", "")] for p in self.products.values()]
        gia_list = [[p.get("gia", 0)] for p in self.products.values()]

        # Fit encoders
        encoder_loai = OneHotEncoder(sparse_output=False, handle_unknown="ignore").fit(loai_sp_list)
        encoder_mau = OneHotEncoder(sparse_output=False, handle_unknown="ignore").fit(mau_da_list)
        encoder_ham = OneHotEncoder(sparse_output=False, handle_unknown="ignore").fit(ham_luong_list)
        scaler_gia = MinMaxScaler().fit(gia_list)

        # Transform
        loai_encoded = encoder_loai.transform(loai_sp_list)
        mau_encoded = encoder_mau.transform(mau_da_list)
        ham_encoded = encoder_ham.transform(ham_luong_list)
        gia_scaled = scaler_gia.transform(gia_list)

        # Concatenate features
        self.feature_matrix = np.hstack([loai_encoded, mau_encoded, ham_encoded, gia_scaled])

    def get_similar_products(self, product_id: str, limit: int = 6):
        if not self.feature_matrix is not None or product_id not in self.product_ids:
            return []
        
        idx = self.product_ids.index(product_id)
        target_vector = self.feature_matrix[idx].reshape(1, -1)
        
        similarities = cosine_similarity(target_vector, self.feature_matrix)[0]
        
        # Get top indices, excluding the product itself
        top_indices = similarities.argsort()[::-1]
        top_indices = [i for i in top_indices if self.product_ids[i] != product_id][:limit]
        
        return [self.products[self.product_ids[i]] for i in top_indices]

    def get_recommendations_for_user(self, user_id: str, limit: int = 8):
        # Fallback to general top products (cold start) if no user history is mockable
        if not self.products:
            return []
        # Return random/first ones for simplicity in this prototype unless we query order-service
        return list(self.products.values())[:limit]

recommendation_engine = RecommendationEngine()
