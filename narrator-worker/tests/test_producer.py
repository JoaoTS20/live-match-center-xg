import asyncio
from datetime import datetime, timezone
import json
from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

from app.kafka.producer import CommentaryProducer
from app.schemas.event import MatchCommentary, MatchEvent


def test_producer_publishes_camel_case_json_keyed_by_match_id(monkeypatch) -> None:
    fake_kafka = AsyncMock()
    fake_kafka.send_and_wait.return_value = SimpleNamespace(partition=0, offset=3)
    monkeypatch.setattr(
        "app.kafka.producer.AIOKafkaProducer",
        MagicMock(return_value=fake_kafka),
    )

    event = MatchEvent(
        id=uuid4(),
        matchId="sporting-vs-braga-2026",
        team="Sporting",
        playerName="Trincao",
        eventType="SHOT",
        x=91.5,
        y=52.0,
        minute=72,
        createdAt=datetime.now(timezone.utc),
    )
    commentary = MatchCommentary.from_event(event, xg=0.34, commentary="What a strike!")

    producer = CommentaryProducer(topic="match-commentary")

    async def scenario() -> None:
        await producer.start()
        await producer.publish(commentary)
        await producer.stop()

    asyncio.run(scenario())

    fake_kafka.start.assert_awaited_once()
    fake_kafka.stop.assert_awaited_once()
    fake_kafka.send_and_wait.assert_awaited_once()

    args, kwargs = fake_kafka.send_and_wait.call_args
    assert args == ("match-commentary",)
    assert kwargs["key"] == b"sporting-vs-braga-2026"

    decoded = json.loads(kwargs["value"].decode("utf-8"))
    assert decoded["matchId"] == "sporting-vs-braga-2026"
    assert decoded["playerName"] == "Trincao"
    assert decoded["eventType"] == "SHOT"
    assert decoded["xg"] == 0.34
    assert decoded["commentary"] == "What a strike!"
