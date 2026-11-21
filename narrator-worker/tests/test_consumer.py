import json
import logging
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

import pytest

from app.kafka.consumer import consume_events


class FakeKafkaConsumer:
    def __init__(self, messages):
        self._messages = messages
        self.started = False
        self.stopped = False

    async def start(self):
        self.started = True

    async def stop(self):
        self.stopped = True

    def __aiter__(self):
        return self._iterate()

    async def _iterate(self):
        for msg in self._messages:
            yield msg


@pytest.mark.asyncio
async def test_consume_events_processes_shots_and_skips_invalid(caplog):
    shot_payload = json.dumps(
        {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "matchId": "benfica-vs-porto-2026",
            "team": "Benfica",
            "playerName": "Pavlidis",
            "eventType": "SHOT",
            "x": 88.5,
            "y": 48.0,
            "minute": 64,
            "createdAt": "2026-11-21T20:00:00Z",
        }
    ).encode("utf-8")

    pass_payload = json.dumps(
        {
            "id": "550e8400-e29b-41d4-a716-446655440001",
            "matchId": "benfica-vs-porto-2026",
            "team": "Benfica",
            "playerName": "Sudakov",
            "eventType": "PASS",
            "x": 55.0,
            "y": 40.0,
            "minute": 63,
            "createdAt": "2026-11-21T19:59:00Z",
        }
    ).encode("utf-8")

    messages = [
        SimpleNamespace(value=None),
        SimpleNamespace(value=b"not-valid-json"),
        SimpleNamespace(value=pass_payload),
        SimpleNamespace(value=shot_payload),
    ]
    fake_consumer = FakeKafkaConsumer(messages)
    mock_narrator = AsyncMock(return_value="Pavlidis fires from inside the box!")

    with (
        patch("app.kafka.consumer.AIOKafkaConsumer", return_value=fake_consumer),
        patch("app.kafka.consumer.generate_commentary", mock_narrator),
        caplog.at_level(logging.DEBUG, logger="narrator-worker"),
    ):
        await consume_events()

    assert fake_consumer.started is True
    assert fake_consumer.stopped is True
    mock_narrator.assert_awaited_once()
    assert "Skipping invalid message" in caplog.text
    assert "Ignored non-shot event" in caplog.text
    assert "Pavlidis fires from inside the box!" in caplog.text
