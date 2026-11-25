import logging
import httpx
from google import genai
from google.genai import types

from app.core.config import settings
from app.schemas.event import MatchEvent
from functools import lru_cache

logger = logging.getLogger("narrator-worker")

SYSTEM_INSTRUCTION = (
    "You are an energetic live football commentator. "
    "First, use the get_match_history tool to check what has happened earlier in this match. "
    "Note that the returned timeline includes the current event as the latest entry—treat only "
    "earlier entries as prior match history so you never double-count the current action. "
    "Then write 1-2 punchy sentences of live commentary referencing the xG and any relevant "
    "match context (such as prior shots, goals, or team momentum). "
    "If match history is unavailable, narrate the current event without guessing earlier events."
)

@lru_cache(maxsize=1)
def _get_genai_client(api_key: str) -> genai.Client:
    return genai.Client(api_key=api_key)

async def get_match_history(match_id: str) -> dict:
    """
    Tool: Fetches all previous events for a given match from match-service
    so the narrator can reference match momentum, prior shots, or goals.
    """
    url = f"{settings.match_service_url}/api/v1/events/{match_id}"
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(url)
            response.raise_for_status()
            events = response.json()
            logger.info("Tool call: fetched %d historical events for %s", len(events), match_id)
            return {"events" : events}
    except httpx.HTTPError as exc:
        logger.warning("Tool call failed for %s: %s", url, exc)
        return {"error": "Match history unavailable"}


async def generate_commentary(event: MatchEvent, xg: float) -> str:
    """
    Uses an LLM agent with the get_match_history tool to generate
    live play-by-play commentary for a shot or goal.
    """
    if not settings.gemini_api_key:
        return (
            f"{event.minute}' — {event.player_name} ({event.team}) "
            f"fires a {event.event_type} (xG: {xg:.2f})!"
        )

    client = _get_genai_client(settings.gemini_api_key)

    prompt = (
        f"A new event just happened in match '{event.match_id}':\n"
        f"- Minute: {event.minute}'\n"
        f"- Team: {event.team}\n"
        f"- Player: {event.player_name}\n"
        f"- Type: {event.event_type}\n"
        f"- Pitch coordinates: ({event.x}, {event.y})\n"
        f"- Calculated xG: {xg:.2f}\n\n"
        f"Generate live commentary for this moment."
    )

    chat = client.aio.chats.create(
        model=settings.gemini_model,
        config=types.GenerateContentConfig(
            system_instruction=SYSTEM_INSTRUCTION,
            tools=[get_match_history],
            temperature=0.7,
        ),
    )
    response = await chat.send_message(prompt)
    return (response.text or "").strip()
