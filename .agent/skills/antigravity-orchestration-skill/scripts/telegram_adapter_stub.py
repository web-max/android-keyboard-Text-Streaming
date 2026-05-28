#!/usr/bin/env python3
"""Build a CEO-ping payload for the Telegram adapter."""

from __future__ import annotations

import json
import sys
from datetime import datetime, timezone


def build_payload(event_type: str, message: str, run_id: str | None = None) -> dict:
    return {
        "adapter": "telegram-ceo-ping",
        "run_id": run_id,
        "event_type": event_type,
        "message": message,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "send_policy": "ceo_ping_only",
    }


def main() -> int:
    if len(sys.argv) < 3:
        print(
            "Usage: telegram_adapter_stub.py EVENT_TYPE MESSAGE [RUN_ID]",
            file=sys.stderr,
        )
        return 2

    event_type = sys.argv[1]
    message = sys.argv[2]
    run_id = sys.argv[3] if len(sys.argv) > 3 else None
    print(json.dumps(build_payload(event_type, message, run_id), indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
