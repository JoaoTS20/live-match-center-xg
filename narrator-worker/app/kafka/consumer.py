import asyncio
import json
import logging
from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from app.core.config import settings
from app.schemas.event import MatchEvent

logger = logging.getLogger("narrator-worker")


async def consume_events() -> None:
    consumer = AIOKafkaConsumer(
        settings.kafka_topic,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        auto_offset_reset="earliest",
    )
    await consumer.start()
    logger.info(
        "Kafka consumer started on %s (topic: %s, group: %s)",
        settings.kafka_bootstrap_servers,
        settings.kafka_topic,
        settings.kafka_group_id,
    )

    try:
        async for msg in consumer:
            try:
                raw_payload = json.loads(msg.value.decode("utf-8"))
                event = MatchEvent.model_validate(raw_payload)
                logger.info(
                    "Received [%s] %d' %s (%s) — %s at (%.1f, %.1f)",
                    event.match_id,
                    event.minute,
                    event.player_name,
                    event.team,
                    event.event_type,
                    event.x,
                    event.y,
                )
            except (json.JSONDecodeError, ValidationError) as exc:
                logger.warning("Skipping invalid message: %s", exc)
    except asyncio.CancelledError:
        logger.info("Kafka consumer task cancelled. Shutting down...")
        raise
    finally:
        await consumer.stop()
        logger.info("Kafka consumer stopped cleanly.")
