from __future__ import annotations

import os
import logging
from typing import Optional

import requests
from flask import Flask, Response, request

app = Flask(__name__)

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger("twilio-voice")

BACKEND_URL = "https://google.com"
MAX_RECORD_SECONDS = int(os.getenv("MAX_RECORD_SECONDS", "60"))
VOICE_LANG = os.getenv("VOICE_LANG", "ru-RU")
VOICE_NAME = os.getenv("VOICE_NAME", "alice")


@app.route("/process-call", methods=["POST"])
def process_call() -> Response:
    """Respond to an incoming call with TwiML that records a message."""
    twiml = f"""<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Response>
  <Say language=\"{VOICE_LANG}\" voice=\"{VOICE_NAME}\">Здравствуйте, пожалуйста, оставьте ваше обращение после сигнала.</Say>
  <Record maxLength=\"{MAX_RECORD_SECONDS}\" action=\"/process-recording\" method=\"POST\" />
  <Say language=\"{VOICE_LANG}\" voice=\"{VOICE_NAME}\">Спасибо. Мы получили ваше сообщение.</Say>
</Response>"""
    return Response(twiml, mimetype="text/xml")


@app.route("/process-recording", methods=["POST"])
def process_recording() -> Response:
    """Handle recording callback from Twilio after <Record> completes."""
    recording_url = request.form.get("RecordingUrl")
    recording_sid = request.form.get("RecordingSid")
    recording_duration = request.form.get("RecordingDuration")
    from_number = request.form.get("From")

    if not recording_url:
        logger.warning("Missing RecordingUrl in callback payload")
        return Response("Missing RecordingUrl", status=400)

    mp3_url = f"{recording_url}.mp3"
    logger.info(
        "Recording received sid=%s duration=%s from=%s url=%s",
        recording_sid,
        recording_duration,
        from_number,
        mp3_url,
    )

    send_to_backend(mp3_url, recording_sid=recording_sid, from_number=from_number)
    return Response("Recording processed", status=200)


def send_to_backend(recording_url: str, recording_sid: Optional[str], from_number: Optional[str]) -> None:
    if not BACKEND_URL:
        logger.info("BACKEND_URL not set, skipping forward")
        return

    payload = {
        "audio_url": recording_url,
        "recording_sid": recording_sid,
        "from": from_number,
    }

    try:
        response = requests.post(BACKEND_URL, data=payload, timeout=10)
        response.raise_for_status()
        logger.info("Recording forwarded successfully")
    except requests.RequestException as exc:
        logger.error("Failed to forward recording: %s", exc)


@app.route("/health", methods=["GET"])
def health() -> Response:
    return Response("ok", status=200)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")), debug=True)
