import logging
from aiokafka import AIOKafkaProducer

from app.core.config import settings
from app.schemas.event import MatchCommentary

logger = logging.getLogger("narrator-worker")


class CommentaryProducer:
    def __init__(
        self,
        bootstrap_servers: str = settings.kafka_bootstrap_servers,
        topic: str = settings.kafka_commentary_topic,
    ) -> None:
        self._topic = topic
        self._producer = AIOKafkaProducer(bootstrap_servers=bootstrap_servers)

    async def start(self) -> None:
        await self._producer.start()
        logger.info("Kafka commentary producer started (topic: %s)", self._topic)

    async def stop(self) -> None:
        await self._producer.stop()
        logger.info("Kafka commentary producer stopped cleanly.")

    async def publish(self, commentary: MatchCommentary) -> None:
        metadata = await self._producer.send_and_wait(
            self._topic,
            key=commentary.match_id.encode("utf-8"),
            value=commentary.model_dump_json(by_alias=True).encode("utf-8"),
        )
        logger.info(
            "Published commentary for [%s] event %s to %s [partition=%d, offset=%d]",
            commentary.match_id,
            commentary.id,
            self._topic,
            metadata.partition,
            metadata.offset,
        )
