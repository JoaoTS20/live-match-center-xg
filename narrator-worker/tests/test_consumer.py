import asyncio
from datetime import datetime, timezone
import json
from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

from app.kafka.consumer import consume_events
from app.schemas.event import MatchCommentary


class FakeKafkaConsumer:
    def __init__(self, messages: list[SimpleNamespace]) -> None:
        self._messages = messages
        self.start = AsyncMock()
        self.stop = AsyncMock()

    def __aiter__(self):
        self._iter = iter(self._messages)
        return self

    async def __anext__(self):
        try:
            return next(self._iter)
        except StopIteration:
            raise StopAsyncIteration


def _event_bytes(event_type: str = "SHOT") -> bytes:
    return json.dumps({
        "id": str(uuid4()),
        "matchId": "sporting-vs-braga-2026",
        "team": "Sporting",
        "playerName": "Trincao",
        "eventType": event_type,
        "x": 91.5,
        "y": 52.0,
        "minute": 72,
        "createdAt": datetime.now(timezone.utc).isoformat(),
    }).encode("utf-8")


def test_consume_events_publishes_only_shots_and_goals(monkeypatch) -> None:
    fake_consumer = FakeKafkaConsumer([
        SimpleNamespace(value=None),
        SimpleNamespace(value=b"{invalid-json"),
        SimpleNamespace(value=_event_bytes("PASS")),
        SimpleNamespace(value=_event_bytes("SHOT")),
    ])
    fake_producer = AsyncMock()

    monkeypatch.setattr(
        "app.kafka.consumer.AIOKafkaConsumer",
        MagicMock(return_value=fake_consumer),
    )
    monkeypatch.setattr(
        "app.kafka.consumer.CommentaryProducer",
        MagicMock(return_value=fake_producer),
    )
    monkeypatch.setattr(
        "app.kafka.consumer.generate_commentary",
        AsyncMock(return_value="Trincao fires just wide!"),
    )

    asyncio.run(consume_events())

    fake_producer.start.assert_awaited_once()
    fake_consumer.start.assert_awaited_once()
    fake_producer.publish.assert_awaited_once()

    published = fake_producer.publish.call_args.args[0]
    assert isinstance(published, MatchCommentary)
    assert published.commentary == "Trincao fires just wide!"
    assert 0.0 < published.xg <= 1.0

    fake_consumer.stop.assert_awaited_once()
    fake_producer.stop.assert_awaited_once()


def test_consume_events_publishes_fallback_on_narrator_error(monkeypatch) -> None:
    fake_consumer = FakeKafkaConsumer([SimpleNamespace(value=_event_bytes("GOAL"))])
    fake_producer = AsyncMock()

    monkeypatch.setattr(
        "app.kafka.consumer.AIOKafkaConsumer",
        MagicMock(return_value=fake_consumer),
    )
    monkeypatch.setattr(
        "app.kafka.consumer.CommentaryProducer",
        MagicMock(return_value=fake_producer),
    )
    monkeypatch.setattr(
        "app.kafka.consumer.generate_commentary",
        AsyncMock(side_effect=RuntimeError("Gemini timeout")),
    )

    asyncio.run(consume_events())

    fake_producer.publish.assert_awaited_once()
    published = fake_producer.publish.call_args.args[0]
    assert "72' Trincao (Sporting)" in published.commentary
    assert "GOAL" in published.commentary
