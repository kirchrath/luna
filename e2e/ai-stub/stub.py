"""OpenAI-compatible chat endpoint with fixed answers for the end-to-end tests.

The classification prompt asks for element types, the qualification prompts for items. Every input becomes one
task, so the tests can rely on a predictable result; a marker like [e2e-alert-123] or [e2e-note-123] makes it an
alert or a note instead. The marker is copied into the title, so that a test finds its own element among those of
other tests.
"""
import json
import re
from http.server import BaseHTTPRequestHandler, HTTPServer

CLASSIFICATION_MARKER = "Element types"
ALERT_PROMPT = "You extract all alerts"
NOTE_PROMPT = "You summarize"
TEST_MARKER = re.compile(r"\[e2e-[\w-]+\]")


def classification(body):
    if "[e2e-alert-" in body:
        return {"types": ["ALERT"]}
    if "[e2e-note-" in body:
        return {"types": ["NOTE"]}
    return {"types": ["TASK"]}


def title(prefix, body):
    marker = TEST_MARKER.search(body)
    return prefix if marker is None else f"{prefix} {marker.group(0)}"


def qualification(body):
    if ALERT_PROMPT in body:
        return {"items": [{"title": title("E2E alert", body), "message": "Disk almost full",
                           "severity": "CRITICAL", "affectedSystem": "nas01"}]}
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
