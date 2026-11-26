from unittest.mock import MagicMock
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.api.routes import router


def _build_test_app() -> FastAPI:
    app = FastAPI()
    app.include_router(router)
    return app


def test_health_returns_200_when_consumer_task_is_running():
    app = _build_test_app()
    mock_task = MagicMock()
    mock_task.done.return_value = False
    app.state.consumer_task = mock_task

    client = TestClient(app)
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_health_returns_503_when_consumer_task_has_exited():
    app = _build_test_app()
    mock_task = MagicMock()
    mock_task.done.return_value = True
    app.state.consumer_task = mock_task

    client = TestClient(app)
    response = client.get("/health")

    assert response.status_code == 503
    assert response.json() == {"status": "degraded"}


def test_health_returns_503_when_consumer_task_is_missing():
    app = _build_test_app()

    client = TestClient(app)
    response = client.get("/health")

    assert response.status_code == 503
    assert response.json() == {"status": "degraded"}
