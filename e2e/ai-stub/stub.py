"""OpenAI-compatible chat endpoint with fixed answers for the end-to-end tests.

The classification prompt asks for element types, the qualification prompts for items. Every input becomes
one task, so the tests can rely on a predictable result.
"""
import json
from http.server import BaseHTTPRequestHandler, HTTPServer

CLASSIFICATION = {"types": ["TASK"]}
QUALIFICATION = {"items": [{"title": "E2E task", "priority": "NORMAL"}]}
CLASSIFICATION_MARKER = "Element types"


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0))).decode()
        answer = CLASSIFICATION if CLASSIFICATION_MARKER in body else QUALIFICATION
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
