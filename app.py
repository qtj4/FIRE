from __future__ import annotations

import os
import logging
import tempfile
from typing import Optional
import html as html_lib
from urllib.parse import quote, unquote
import re
from urllib.parse import quote
import requests
from flask import Flask, Response, request, jsonify
from openai import OpenAI

app = Flask(__name__)

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger("twilio-voice")

BACKEND_URL = os.getenv("BACKEND_URL") or "http://localhost:5000/receive-recording"
MAX_RECORD_SECONDS = int(os.getenv("MAX_RECORD_SECONDS", "60"))
VOICE_LANG = os.getenv("VOICE_LANG", "ru-RU")
VOICE_NAME = os.getenv("VOICE_NAME", "alice")
TWILIO_ACCOUNT_SID = "AC6f2894a55dd4a13ed1cbc5fdec666bfa"
TWILIO_AUTH_TOKEN = "143cb02c6c0555ea1044a432f7282842"
OPENAI_API_KEY = "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"
OPENAI_TRANSCRIBE_MODEL = os.getenv("OPENAI_TRANSCRIBE_MODEL", "gpt-4o-mini-transcribe")


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

    transcript = send_to_backend(
        mp3_url,
        recording_sid=recording_sid,
        from_number=from_number,
        recording_duration=recording_duration,
    )
    if transcript:
        logger.info("Transcript: %s", transcript)
    return Response("Recording processed", status=200)


def send_to_backend(
    recording_url: str,
    recording_sid: Optional[str],
    from_number: Optional[str],
    recording_duration: Optional[str],
) -> Optional[str]:
    if not BACKEND_URL:
        logger.info("BACKEND_URL not set, skipping forward")
        return None

    payload = {
        "audio_url": recording_url,
        "recording_sid": recording_sid,
        "from": from_number,
        "recording_duration": recording_duration,
    }

    try:
        response = requests.post(BACKEND_URL, data=payload, timeout=10)
        response.raise_for_status()
        logger.info("Recording forwarded successfully")
        try:
            data = response.json()
        except ValueError:
            return None
        return data.get("transcript")
    except requests.RequestException as exc:
        logger.error("Failed to forward recording: %s", exc)
        return None


@app.route("/receive-recording", methods=["POST"])
def receive_recording() -> Response:
    """Receive recording URL, run speech-to-text, and return the transcript."""
    data = request.get_json(silent=True) if request.is_json else None
    form_data = data or request.form

    audio_url = form_data.get("audio_url")
    recording_sid = form_data.get("recording_sid")
    from_number = form_data.get("from")
    recording_duration = form_data.get("recording_duration")

    if not audio_url:
        return jsonify({"error": "audio_url is required"}), 400

    normalized_url = normalize_audio_url(audio_url)
    logger.info("Downloading recording from %s", normalized_url)

    try:
        file_path = download_recording(normalized_url)
    except requests.RequestException as exc:
        logger.error("Failed to download recording: %s", exc)
        return jsonify({"error": "failed to download recording"}), 502

    try:
        transcript = transcribe_audio(file_path)
    except RuntimeError as exc:
        logger.error("Transcription error: %s", exc)
        return jsonify({"error": str(exc)}), 501
    except Exception as exc:  # noqa: BLE001 - surface unexpected errors as 502
        logger.error("Transcription failed: %s", exc)
        return jsonify({"error": "transcription failed"}), 502
    finally:
        try:
            os.remove(file_path)
        except OSError:
            logger.warning("Failed to remove temp file %s", file_path)

    return jsonify(
        {
            "transcript": transcript,
            "recording_sid": recording_sid,
            "from": from_number,
            "recording_duration": recording_duration,
            "audio_url": normalized_url,
        }
    )


def normalize_audio_url(audio_url: str) -> str:
    if audio_url.endswith((".mp3", ".wav", ".m4a", ".webm", ".mp4")):
        return audio_url
    return f"{audio_url}.mp3"


def download_recording(audio_url: str) -> str:
    auth = None
    if TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN:
        auth = (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)

    response = requests.get(audio_url, auth=auth, stream=True, timeout=20)
    response.raise_for_status()

    suffix = os.path.splitext(audio_url)[-1] or ".mp3"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        for chunk in response.iter_content(chunk_size=1024 * 1024):
            if chunk:
                tmp.write(chunk)
        return tmp.name


def transcribe_audio(file_path: str) -> str:
    if not OPENAI_API_KEY:
        raise RuntimeError("OPENAI_API_KEY is not set")

    client = OpenAI(api_key=OPENAI_API_KEY)
    with open(file_path, "rb") as audio_file:
        transcription = client.audio.transcriptions.create(
            model=OPENAI_TRANSCRIBE_MODEL,
            file=audio_file,
        )
    text = getattr(transcription, "text", None)
    return text if text is not None else str(transcription)

def _fetch_html(url: str) -> str:
    headers = {
        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Safari/537.36",
        "Accept-Language": "ru-RU,ru;q=0.9,en;q=0.8",
    }
    response = requests.get(url, headers=headers, timeout=10)
    response.raise_for_status()
    return response.text


def _extract_view_coordinates(search_html: str) -> Optional[tuple[float, float]]:
    # 2GIS search page triggers a request like:
    # https://jam.api.2gis.com/scores?view=71.451118,51.09100999999999,71.451118,51.09100999999999&z=11
    candidates = [
        search_html,
        html_lib.unescape(search_html),
        unquote(search_html),
    ]
    for text in candidates:
        match = re.search(
            r"jam\.api\.2gis\.com/scores\?view=([0-9.+-]+),([0-9.+-]+),([0-9.+-]+),([0-9.+-]+)",
            text,
        )
        if match:
            lon1, lat1, lon2, lat2 = match.groups()
            lon = float(lon1)
            lat = float(lat1)
            if lon1 != lon2 or lat1 != lat2:
                logger.warning("Scores view bbox mismatch: %s,%s vs %s,%s", lon1, lat1, lon2, lat2)
            return lat, lon

        match = re.search(
            r'["\']view["\']\s*[:=]\s*["\']?([0-9.+-]+)\s*,\s*([0-9.+-]+)\s*,\s*([0-9.+-]+)\s*,\s*([0-9.+-]+)',
            text,
        )
        if match:
            lon1, lat1, lon2, lat2 = match.groups()
            lon = float(lon1)
            lat = float(lat1)
            if lon1 != lon2 or lat1 != lat2:
                logger.warning("View bbox mismatch: %s,%s vs %s,%s", lon1, lat1, lon2, lat2)
            return lat, lon

        match = re.search(
            r'["\']center["\']\s*:\s*\[\s*([0-9.+-]+)\s*,\s*([0-9.+-]+)\s*\]',
            text,
        )
        if match:
            lon, lat = match.groups()
            return float(lat), float(lon)

        match = re.search(
            r'["\']point["\']\s*:\s*\{\s*["\']lon["\']\s*:\s*([0-9.+-]+)\s*,\s*["\']lat["\']\s*:\s*([0-9.+-]+)\s*\}',
            text,
        )
        if match:
            lon, lat = match.groups()
            return float(lat), float(lon)
    return None


@app.route("/geocode", methods=["GET"])
def geocode() -> Response:
    address = (request.args.get("address") or "").strip()
    if not address:
        return jsonify({"error": "address is required"}), 400

    search_url = f"https://2gis.kz/search/{quote(address, safe='')}"
    try:
        search_html = _fetch_html(search_url)
        coords = _extract_view_coordinates(search_html)
        if not coords:
            logger.warning("No scores view coordinates found for address=%s", address)
            return jsonify({"error": "coordinates not found"}), 502
    except requests.RequestException as exc:
        logger.error("2gis request failed: %s", exc)
        return jsonify({"error": "upstream request failed"}), 502
    except ValueError as exc:
        logger.error("Failed to parse coordinates: %s", exc)
        return jsonify({"error": "invalid coordinate format"}), 502

    lat, lon = coords
    return jsonify(
        {
            "address": address,
            "lat": lat,
            "lon": lon,
            "source_url": search_url,
        }
    )

@app.route("/health", methods=["GET"])
def health() -> Response:
    return Response("okj", status=200)



if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5001")), debug=True)
