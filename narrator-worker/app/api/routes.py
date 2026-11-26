from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

router = APIRouter()


@router.get("/health")
async def health_check(request: Request) -> dict[str, str]:
    task = getattr(request.app.state, "consumer_task", None)
    if task is not None and not task.done():
        return JSONResponse(status_code=200, content={"status": "ok"})
    return JSONResponse(status_code=503, content={"status": "degraded"})
