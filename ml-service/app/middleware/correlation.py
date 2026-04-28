"""
Correlation ID middleware for FastAPI ML service.
Reads X-Correlation-Id from incoming request or generates a new UUID.
Adds it to response headers and includes it in log context.
"""
import uuid
import logging
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

logger = logging.getLogger(__name__)

CORRELATION_ID_HEADER = "X-Correlation-Id"


class CorrelationIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        correlation_id = request.headers.get(CORRELATION_ID_HEADER)
        if not correlation_id:
            correlation_id = str(uuid.uuid4())

        # Make available to request state
        request.state.correlation_id = correlation_id

        logger.info(
            "Request: %s %s [correlationId=%s]",
            request.method, request.url.path, correlation_id
        )

        response = await call_next(request)
        response.headers[CORRELATION_ID_HEADER] = correlation_id
        return response
