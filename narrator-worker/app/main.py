import asyncio
import contextlib
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI

from app.api.routes import router
from app.kafka.consumer import consume_events

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    consumer_task = asyncio.create_task(consume_events())
    yield
    consumer_task.cancel()
    with contextlib.suppress(asyncio.CancelledError):
        await consumer_task


app = FastAPI(title="Narrator Worker", lifespan=lifespan)
app.include_router(router)
