"""OpenAI-compatible chat endpoint with fixed answers for the end-to-end tests.

The classification prompt asks for element types, the qualification prompts for items. Every input becomes
one task, so the tests can rely on a predictable result. A marker like [e2e-123] in the input is copied into the
title of the task, so that a test finds its own task among those of other tests.
"""
import json
import re
from http.server import BaseHTTPRequestHandler, HTTPServer

CLASSIFICATION = {"types": ["TASK"]}
TASK_TITLE = "E2E task"
CLASSIFICATION_MARKER = "Element types"
TEST_MARKER = re.compile(r"\[e2e-[\w-]+\]")


def qualification(body):
    marker = TEST_MARKER.search(body)
    title = TASK_TITLE if marker is None else f"{TASK_TITLE} {marker.group(0)}"
    return {"items": [{"title": title, "priority": "NORMAL"}]}


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0))).decode()
        answer = CLASSIFICATION if CLASSIFICATION_MARKER in body else qualification(body)
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
