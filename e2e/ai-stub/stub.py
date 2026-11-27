"""OpenAI-compatible chat endpoint with fixed answers for the end-to-end tests.

The classification prompt asks for element types, the qualification prompts for items. Every input becomes one
task, so the tests can rely on a predictable result; a marker like [e2e-alert-123], [e2e-note-123] or
[e2e-appointment-123] makes it an alert, a note or an appointment (tomorrow 10:00-11:00 UTC) instead. The marker is copied into the title, so that a test finds its own element among those of
other tests. The requested model is added to the title as well, so that a test sees which provider analyzed an input.
"""
import json
import re
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, HTTPServer

CLASSIFICATION_MARKER = "Element types"
ALERT_PROMPT = "You extract all alerts"
NOTE_PROMPT = "You summarize"
APPOINTMENT_PROMPT = "You extract all appointments"
TEST_MARKER = re.compile(r"\[e2e-[\w-]+\]")


def classification(body):
    if "[e2e-alert-" in body:
        return {"types": ["ALERT"]}
    if "[e2e-note-" in body:
        return {"types": ["NOTE"]}
    if "[e2e-appointment-" in body:
        return {"types": ["APPOINTMENT"]}
    return {"types": ["TASK"]}


def title(prefix, body):
    model = json.loads(body).get("model", "unknown")
    marker = TEST_MARKER.search(body)
    return f"{prefix} ({model})" if marker is None else f"{prefix} ({model}) {marker.group(0)}"


def qualification(body):
    if ALERT_PROMPT in body:
        return {"items": [{"title": title("E2E alert", body), "message": "Disk almost full",
                           "severity": "CRITICAL", "affectedSystem": "nas01"}]}
    if APPOINTMENT_PROMPT in body:
        start = (datetime.now(timezone.utc) + timedelta(days=1)).replace(hour=10, minute=0, second=0, microsecond=0)
        return {"items": [{"title": title("E2E appointment", body), "start": start.isoformat(),
                           "end": (start + timedelta(hours=1)).isoformat(), "allDay": False,
                           "location": "Practice at the market"}]}
    if NOTE_PROMPT in body:
        return {"items": [{"title": title("E2E note", body), "summary": "Roof will be renovated in May"}]}
    return {"items": [{"title": title("E2E task", body), "priority": "NORMAL"}]}


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0))).decode()
        answer = classification(body) if CLASSIFICATION_MARKER in body else qualification(body)
        payload = json.dumps({
            "id": "e2e",
            "object": "chat.completion",
            "created": 0,
            "model": "stub",
            "choices": [{"index": 0, "finish_reason": "stop",
                         "message": {"role": "assistant", "content": json.dumps(answer)}}],
            "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2},
        }).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


HTTPServer(("0.0.0.0", 8000), Handler).serve_forever()
