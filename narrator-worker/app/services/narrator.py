import logging
import httpx
from google import genai
from google.genai import types

from app.core.config import settings
from app.schemas.event import MatchEvent

logger = logging.getLogger("narrator-worker")


def get_match_history(match_id: str) -> list[dict]:
    """
    Tool: Fetches all previous events for a given match from match-service
    so the narrator can reference match momentum, prior shots, or goals.
    """
    url = f"{settings.match_service_url}/api/v1/events/{match_id}"
    try:
        response = httpx.get(url, timeout=5.0)
        response.raise_for_status()
        events = response.json()
        logger.info("Tool call: fetched %d historical events for %s", len(events), match_id)
        return events
    except httpx.HTTPError as exc:
        logger.warning("Tool call failed for %s: %s", url, exc)
        return []


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

    client = genai.Client(api_key=settings.gemini_api_key)

    prompt = (
        f"You are an energetic live football commentator. "
        f"A new event just happened in match '{event.match_id}':\n"
        f"- Minute: {event.minute}'\n"
        f"- Team: {event.team}\n"
        f"- Player: {event.player_name}\n"
        f"- Type: {event.event_type}\n"
        f"- Pitch coordinates: ({event.x}, {event.y})\n"
        f"- Calculated xG: {xg:.2f}\n\n"
        f"First, use the get_match_history tool to check what has happened earlier in this match. "
        f"Then write 1-2 punchy sentences of live commentary referencing the xG and any relevant match context."
    )

    response = await client.aio.models.generate_content(
        model="models/gemini-3.8-flash",
        contents=prompt,
        config=types.GenerateContentConfig(
            tools=[get_match_history],
            temperature=0.7,
        ),
    )
    return response.text.strip()
