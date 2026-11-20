from datetime import datetime
from typing import Literal
from uuid import UUID
from pydantic import BaseModel, ConfigDict, Field


class MatchEvent(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: UUID
    match_id: str = Field(alias="matchId")
    team: str
    player_name: str = Field(alias="playerName")
    event_type: Literal["SHOT", "GOAL", "PASS", "FOUL", "SAVE"] = Field(alias="eventType")
    x: float = Field(ge=0.0, le=100.0)
    y: float = Field(ge=0.0, le=100.0)
    minute: int = Field(ge=0, le=130)
    created_at: datetime = Field(alias="createdAt")
