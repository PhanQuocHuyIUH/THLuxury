import json
import logging
import asyncio
import httpx
import aio_pika
from app.core.config import settings
from app.services.recommendation import recommendation_engine

logger = logging.getLogger(__name__)

async def start_rabbitmq_consumer():
    connection_string = f"amqp://{settings.rabbitmq_user}:{settings.rabbitmq_pass}@{settings.rabbitmq_host}:{settings.rabbitmq_port}/"
    try:
        connection = await aio_pika.connect_robust(connection_string)
        channel = await connection.channel()
        
        # Declare exchange
        exchange = await channel.declare_exchange("catalog.events", aio_pika.ExchangeType.TOPIC, durable=True)
        
        # Declare queue
        queue = await channel.declare_queue("ai.catalog.sync.q", durable=True)
        
        # Bind queue to exchange
        await queue.bind(exchange, "product.#")
        
        logger.info("RabbitMQ Consumer started. Listening to catalog.events...")

        async with queue.iterator() as queue_iter:
            async for message in queue_iter:
                async with message.process():
                    payload = json.loads(message.body.decode("utf-8"))
                    logger.info(f"Received event {message.routing_key}")
                    
                    product_id = payload.get("productId")
                    if not product_id:
                        continue

                    if message.routing_key in ["product.created", "product.updated"]:
                        try:
                            async with httpx.AsyncClient() as client:
                                url = f"{settings.catalog_base_url}/api/products/{product_id}"
                                response = await client.get(url)
                                if response.status_code == 200:
                                    product_data = response.json()
                                    if product_data.get("status") == "ARCHIVED":
                                        recommendation_engine.remove_product(product_id)
                                    else:
                                        recommendation_engine.add_or_update_product(product_data)
                                else:
                                    logger.warning(f"Could not fetch product {product_id}. Status: {response.status_code}")
                        except Exception as req_e:
                            logger.error(f"Error fetching product {product_id}: {req_e}")
                    elif message.routing_key == "product.deleted":
                        recommendation_engine.remove_product(product_id)

    except Exception as e:
        logger.error(f"Failed to connect to RabbitMQ: {e}")
        # Retry logic could be implemented here
        await asyncio.sleep(5)
