import asyncio
import json
import logging
from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from app.core.config import settings
from app.kafka.producer import CommentaryProducer
from app.schemas.event import MatchEvent, MatchCommentary
from app.services.xg_model import calculate_xg
from app.services.narrator import generate_commentary

logger = logging.getLogger("narrator-worker")


async def consume_events() -> None:
    producer = CommentaryProducer()
    consumer = AIOKafkaConsumer(
        settings.kafka_topic,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        auto_offset_reset="earliest",
    )

    await consumer.start()
    try:
        await producer.start()
    except Exception:
        await consumer.stop()
        raise

    logger.info(
        "Kafka consumer started on %s (topic: %s, group: %s)",
        settings.kafka_bootstrap_servers,
        settings.kafka_topic,
        settings.kafka_group_id,
    )

    try:
        async for msg in consumer:
            if msg.value is None:
                continue
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
                continue
            if event.event_type in ("SHOT", "GOAL"):
                xg = calculate_xg(event.x, event.y)
                try:
                    commentary = await generate_commentary(event, xg)
                except Exception as exc:
                    logger.error("Failed to generate commentary for %s: %s", event.id, exc)
                    commentary = f"{event.minute}' {event.player_name} ({event.team}) — {event.event_type} (xG: {xg:.2f})" 

                match_commentary = MatchCommentary.from_event(event, xg, commentary)
                await producer.publish(match_commentary)

                logger.info(
                    "\n[%s] %d' %s (%s) | xG: %.4f\n🎙️  %s",
                    event.match_id,
                    event.minute,
                    event.player_name,
                    event.team,
                    xg,
                    commentary,
                )
            else:
                logger.debug(
                    "Ignored non-shot event [%s] %d' %s (%s) — %s",
                    event.match_id,
                    event.minute,
                    event.player_name,
                    event.team,
                    event.event_type,
                    )
    except asyncio.CancelledError:
        logger.info("Kafka consumer task cancelled. Shutting down...")
        raise
    finally:
        await consumer.stop()
        await producer.stop()
        logger.info("Kafka consumer and producer stopped cleanly.")
