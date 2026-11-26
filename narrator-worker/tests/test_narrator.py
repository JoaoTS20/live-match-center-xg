from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

from app.schemas.event import MatchEvent
from app.services.narrator import generate_commentary, get_match_history
from app.services.narrator import _get_genai_client


def _sample_event() -> MatchEvent:
    return MatchEvent.model_validate(
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
    )


@pytest.mark.asyncio
async def test_get_match_history_returns_events_dict_on_success():
    mock_response = MagicMock()
    mock_response.json.return_value = [{"eventType": "SHOT", "playerName": "Pavlidis"}]
    mock_response.raise_for_status.return_value = None

    mock_http_client = AsyncMock()
    mock_http_client.get.return_value = mock_response

    mock_cm = AsyncMock()
    mock_cm.__aenter__.return_value = mock_http_client

    with patch("app.services.narrator.httpx.AsyncClient", return_value=mock_cm):
        result = await get_match_history("benfica-vs-porto-2026")

    assert result == {"events": [{"eventType": "SHOT", "playerName": "Pavlidis"}]}


@pytest.mark.asyncio
async def test_get_match_history_returns_error_dict_on_http_error():
    mock_http_client = AsyncMock()
    mock_http_client.get.side_effect = httpx.ConnectError("Connection refused")

    mock_cm = AsyncMock()
    mock_cm.__aenter__.return_value = mock_http_client

    with patch("app.services.narrator.httpx.AsyncClient", return_value=mock_cm):
        result = await get_match_history("benfica-vs-porto-2026")

    assert result == {"error": "Match history unavailable"}


@pytest.mark.asyncio
async def test_generate_commentary_falls_back_when_api_key_missing(monkeypatch):
    monkeypatch.setattr("app.services.narrator.settings.gemini_api_key", "")

    result = await generate_commentary(_sample_event(), 0.32)

    assert "Pavlidis" in result
    assert "0.32" in result


@pytest.mark.asyncio
async def test_generate_commentary_calls_gemini_async_when_api_key_present(monkeypatch):
    monkeypatch.setattr("app.services.narrator.settings.gemini_api_key", "test-key")

    mock_chat = MagicMock()
    mock_chat.send_message = AsyncMock(
        return_value=SimpleNamespace(
            text="What a strike by Pavlidis! High xG chance at 0.32!"
        )
    )
    mock_client = MagicMock()
    mock_client.aio.chats.create.return_value = mock_chat

    with patch("app.services.narrator.genai.Client", return_value=mock_client):
        result = await generate_commentary(_sample_event(), 0.32)

    assert result == "What a strike by Pavlidis! High xG chance at 0.32!"
    mock_chat.send_message.assert_awaited_once()

@pytest.fixture(autouse=True)
def clear_genai_client_cache():
    _get_genai_client.cache_clear()
    yield
    _get_genai_client.cache_clear()