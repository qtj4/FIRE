from __future__ import annotations

import os
import logging
import tempfile
import json
from typing import Any, Optional
import html as html_lib
from urllib.parse import quote, unquote
import re
from uuid import UUID
import requests
from flask import Flask, Response, request, jsonify
from openai import OpenAI

app = Flask(__name__)

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger("twilio-voice")

BACKEND_URL = "http://localhost:5000/receive-recording"
MAX_RECORD_SECONDS = int(os.getenv("MAX_RECORD_SECONDS", "60"))
VOICE_LANG = os.getenv("VOICE_LANG", "ru-RU")
VOICE_NAME = os.getenv("VOICE_NAME", "alice")
TWILIO_ACCOUNT_SID = "AC6f2894a55dd4a13ed1cbc5fdec666bfa"
TWILIO_AUTH_TOKEN = "143cb02c6c0555ea1044a432f7282842"
OPENAI_API_KEY = "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"
OPENAI_TRANSCRIBE_MODEL = os.getenv("OPENAI_TRANSCRIBE_MODEL", "gpt-4o-mini-transcribe")
OPENAI_CHAT_MODEL = os.getenv("OPENAI_CHAT_MODEL", "gpt-4o-mini")
TICKET_WEBHOOK_URL = "http://2.133.130.153:5678/webhook/ticket"
INTAKE_QUEUE_URL = os.getenv("INTAKE_QUEUE_URL", "http://2.133.130.153:8082/api/v1/intake/queue")
TICKET_API_KEY = "reqres_bfea449a338147e6aff41a56a3067e4e"
TICKET_CSV_TEMPLATE = os.getenv(
    "TICKET_CSV_TEMPLATE",
    "a154a8e6-439d-4a7b-86e8-56ef94b18ee2,Мужской,1999-12-31 0:00,{text},VIP,Казахстан,Астана,ул Кабанбай Батыр,40",
)

ASSISTANT_SYSTEM_PROMPT = """
Ты AI-ассистент аналитического дашборда тикетов. Пользователь пишет запрос на русском, а ты возвращаешь JSON без markdown.
Нужно выбрать виджеты для UI по уже доступным данным.

Верни JSON-объект:
{
  "reply": "краткий ответ на русском",
  "widgets": [
    {
      "kind": "bar|doughnut|list|stat",
      "source": "byCity|byType|bySentiment|byOffice|byLanguage|topCities|ticketsTotal|avgPriority|vipShare|inRouting",
      "title": "заголовок",
      "orientation": "horizontal|vertical",
      "topN": 3,
      "helper": "необязательное пояснение"
    }
  ]
}

Ограничения:
- Максимум 4 виджета.
- Для kind=stat source должен быть одним из: ticketsTotal, avgPriority, vipShare, inRouting.
- Для kind=bar/doughnut/list source должен быть одним из: byCity, byType, bySentiment, byOffice, byLanguage, topCities.
- orientation указывать только для kind=bar.
- Если запрос неясен, предложи 1-2 подходящих виджета (например byType и byCity).
- Ничего кроме JSON.
""".strip()


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

    normalized_url = normalize_audio_url(recording_url)
    logger.info(
        "Recording received sid=%s duration=%s from=%s url=%s",
        recording_sid,
        recording_duration,
        from_number,
        normalized_url,
    )

    transcript = send_to_backend(
        normalized_url,
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


@app.after_request
def add_cors_headers(response: Response) -> Response:
    response.headers["Access-Control-Allow-Origin"] = os.getenv("CORS_ALLOW_ORIGIN", "*")
    response.headers["Access-Control-Allow-Methods"] = "GET,POST,PUT,PATCH,DELETE,OPTIONS"
    response.headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization, X-Requested-With"
    return response


@app.route("/api/assistant/dashboard", methods=["POST", "OPTIONS"])
def assistant_dashboard() -> Response:
    if request.method == "OPTIONS":
        return Response(status=204)

    payload = request.get_json(silent=True) or {}
    query = str(payload.get("query", "")).strip()
    history = payload.get("history")
    if not query:
        return jsonify({"error": "query is required"}), 400

    plan = generate_dashboard_assistant_plan(query, history if isinstance(history, list) else [])
    return jsonify(plan)


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

    ticket_status, queue_status = send_ticket_webhook(transcript)

    return jsonify(
        {
            "transcript": transcript,
            "recording_sid": recording_sid,
            "from": from_number,
            "recording_duration": recording_duration,
            "audio_url": normalized_url,
            "ticket_webhook_status": ticket_status,
            "ticket_queue_status": queue_status,
        }
    )


def normalize_audio_url(audio_url: str) -> str:
    if audio_url.endswith((".mp3", ".wav", ".m4a", ".webm", ".mp4")):
        return audio_url
    if "?" in audio_url:
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


def csv_escape(value: str) -> str:
    if any(ch in value for ch in [",", "\"", "\n", "\r"]):
        return "\"" + value.replace("\"", "\"\"") + "\""
    return value


def send_ticket_webhook(transcript: str) -> tuple[Optional[int], Optional[int]]:
    if not TICKET_WEBHOOK_URL:
        logger.info("TICKET_WEBHOOK_URL not set, skipping ticket webhook")
        return None, None

    safe_text = csv_escape(transcript)
    csv_payload = TICKET_CSV_TEMPLATE.replace("{text}", safe_text)
    headers = {"Accept": "application/json"}
    if TICKET_API_KEY:
        headers["x-api-key"] = TICKET_API_KEY

    try:
        response = requests.post(
            TICKET_WEBHOOK_URL,
            headers=headers,
            files={"csv": (None, csv_payload)},
            timeout=20,
        )
        response.raise_for_status()
        body_preview = response.text[:1000] if response.text else ""
        logger.info("Ticket webhook delivered: %s body=%s", response.status_code, body_preview)
        webhook_body: Optional[Any] = None
        try:
            webhook_body = response.json()
        except ValueError:
            logger.warning("Ticket webhook returned non-JSON body, using fallback payload for queue")
        queue_payload = build_intake_queue_payload(webhook_body, transcript)
        queue_status = send_to_intake_queue(queue_payload)
        return response.status_code, queue_status
    except requests.RequestException as exc:
        error_body = ""
        if getattr(exc, "response", None) is not None:
            try:
                error_body = exc.response.text[:1000]
            except Exception:
                error_body = ""
        if error_body:
            logger.error("Ticket webhook failed: %s body=%s", exc, error_body)
        else:
            logger.error("Ticket webhook failed: %s", exc)
        return None, None


def build_intake_queue_payload(webhook_body: Optional[Any], transcript: str) -> dict[str, Any]:
    source = _extract_payload_source(webhook_body)

    type_ = _pick_first(source, "type", "ticketType", "category") or "Неработоспособность приложения"
    sentiment = _pick_first(source, "sentiment", "tone") or "Негативный"
    priority = _to_int(_pick_first(source, "priority", "priority_score"), default=8)
    language = _pick_first(source, "language", "lang", "detected_language") or "RU"
    summary = _pick_first(source, "summary", "description", "text") or transcript or "Клиент оставил голосовое обращение"
    geo_normalized = (
        _pick_first(source, "geo_normalized", "geoNormalized", "geo", "address") or "string"
    )

    payload: dict[str, Any] = {
        "type": str(type_),
        "sentiment": str(sentiment),
        "priority": priority,
        "language": str(language),
        "summary": str(summary),
        "geo_normalized": str(geo_normalized),
    }

    client_guid = _pick_first(source, "clientGuid", "client_guid")
    if client_guid:
        normalized_client_guid = _normalize_uuid(str(client_guid))
        if normalized_client_guid:
            payload["clientGuid"] = normalized_client_guid

    return payload


def send_to_intake_queue(payload: dict[str, Any]) -> Optional[int]:
    if not INTAKE_QUEUE_URL:
        logger.info("INTAKE_QUEUE_URL not set, skipping queue send")
        return None

    try:
        response = requests.post(
            INTAKE_QUEUE_URL,
            json=payload,
            headers={"Accept": "application/json"},
            timeout=20,
        )
        response.raise_for_status()
        logger.info("Queue delivered: %s body=%s", response.status_code, response.text[:1000] if response.text else "")
        return response.status_code
    except requests.RequestException as exc:
        error_body = ""
        if getattr(exc, "response", None) is not None:
            try:
                error_body = exc.response.text[:1000]
            except Exception:
                error_body = ""
        if error_body:
            logger.error("Queue send failed: %s body=%s payload=%s", exc, error_body, payload)
        else:
            logger.error("Queue send failed: %s payload=%s", exc, payload)
        return None


def _extract_payload_source(webhook_body: Optional[Any]) -> dict[str, Any]:
    if isinstance(webhook_body, list):
        for item in webhook_body:
            if isinstance(item, dict):
                return item
        return {}
    if not isinstance(webhook_body, dict):
        return {}

    for key in ("data", "result", "ticket", "payload"):
        nested = webhook_body.get(key)
        if isinstance(nested, dict):
            return nested
        if isinstance(nested, list):
            for item in nested:
                if isinstance(item, dict):
                    return item
    return webhook_body


def _pick_first(data: dict[str, Any], *keys: str) -> Optional[Any]:
    for key in keys:
        if key in data and data[key] not in (None, ""):
            return data[key]
    return None


def _to_int(value: Any, default: int) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _normalize_uuid(value: str) -> Optional[str]:
    try:
        return str(UUID(value))
    except (ValueError, TypeError):
        logger.warning("Invalid clientGuid from webhook response: %s", value)
        return None


def generate_dashboard_assistant_plan(query: str, history: list[Any]) -> dict[str, Any]:
    if not OPENAI_API_KEY:
        return fallback_dashboard_assistant_plan(query)

    safe_history = _sanitize_assistant_history(history)
    messages = [{"role": "system", "content": ASSISTANT_SYSTEM_PROMPT}, *safe_history, {"role": "user", "content": query}]
    try:
        client = OpenAI(api_key=OPENAI_API_KEY)
        completion = client.chat.completions.create(
            model=OPENAI_CHAT_MODEL,
            temperature=0.2,
            response_format={"type": "json_object"},
            messages=messages,
        )
        content = completion.choices[0].message.content or "{}"
        parsed = json.loads(content)
        return _sanitize_dashboard_assistant_output(parsed, query)
    except Exception as exc:  # noqa: BLE001 - fallback keeps endpoint resilient
        logger.error("Dashboard assistant failed: %s", exc)
        return fallback_dashboard_assistant_plan(query)


def _sanitize_assistant_history(history: list[Any]) -> list[dict[str, str]]:
    safe_messages: list[dict[str, str]] = []
    for item in history[-8:]:
        if not isinstance(item, dict):
            continue
        role = item.get("role")
        content = item.get("content")
        if role not in ("user", "assistant") or not isinstance(content, str):
            continue
        normalized_content = content.strip()
        if not normalized_content:
            continue
        safe_messages.append({"role": role, "content": normalized_content[:1000]})
    return safe_messages


def _sanitize_dashboard_assistant_output(raw: Any, query: str) -> dict[str, Any]:
    if not isinstance(raw, dict):
        return fallback_dashboard_assistant_plan(query)

    reply = raw.get("reply")
    reply_text = str(reply).strip() if reply is not None else ""

    sanitized_widgets: list[dict[str, Any]] = []
    widgets = raw.get("widgets")
    if isinstance(widgets, list):
        for item in widgets:
            normalized = _sanitize_widget_spec(item)
            if normalized:
                sanitized_widgets.append(normalized)
            if len(sanitized_widgets) >= 4:
                break

    if not sanitized_widgets:
        return fallback_dashboard_assistant_plan(query)

    if not reply_text:
        titles = ", ".join(widget["title"] for widget in sanitized_widgets)
        reply_text = f"Построил виджеты: {titles}."

    return {"reply": reply_text[:600], "widgets": sanitized_widgets}


def _sanitize_widget_spec(raw: Any) -> Optional[dict[str, Any]]:
    if not isinstance(raw, dict):
        return None

    kind = str(raw.get("kind", "")).strip().lower()
    source = str(raw.get("source", "")).strip()
    title = str(raw.get("title", "")).strip()
    if kind not in ("bar", "doughnut", "list", "stat"):
        return None
    if not title:
        return None

    stat_sources = {"ticketsTotal", "avgPriority", "vipShare", "inRouting"}
    series_sources = {"byCity", "byType", "bySentiment", "byOffice", "byLanguage", "topCities"}
    if kind == "stat":
        if source not in stat_sources:
            return None
    else:
        if source not in series_sources:
            return None

    spec: dict[str, Any] = {"kind": kind, "source": source, "title": title[:80]}

    helper = raw.get("helper")
    if isinstance(helper, str) and helper.strip():
        spec["helper"] = helper.strip()[:120]

    if kind == "bar":
        orientation = str(raw.get("orientation", "")).strip().lower()
        if orientation in ("horizontal", "vertical"):
            spec["orientation"] = orientation

    top_n = raw.get("topN")
    try:
        if top_n is not None:
            spec["topN"] = max(1, min(15, int(top_n)))
    except (TypeError, ValueError):
        pass

    return spec


def fallback_dashboard_assistant_plan(query: str) -> dict[str, Any]:
    normalized = (
        query.lower()
        .replace(".", " ")
        .replace(",", " ")
        .replace("?", " ")
        .replace("!", " ")
        .replace(":", " ")
    )
    wants_city = any(token in normalized for token in ("город", "регион", "област", "географ"))
    wants_type = any(token in normalized for token in ("тип", "категор"))
    wants_sentiment = any(token in normalized for token in ("тональн", "эмоци", "негатив", "позитив"))
    wants_office = any(token in normalized for token in ("офис", "подраздел"))
    wants_vip = "vip" in normalized or "премиум" in normalized
    wants_priority = "приоритет" in normalized
    wants_queue = any(token in normalized for token in ("очеред", "маршрутизац"))
    wants_volume = any(token in normalized for token in ("больше", "топ", "лидер", "максим"))

    widgets: list[dict[str, Any]] = []
    if wants_type:
        widgets.append({"kind": "bar", "source": "byType", "title": "Типы обращений", "orientation": "horizontal"})
    if wants_city:
        widgets.append({"kind": "bar", "source": "byCity", "title": "География обращений"})
    if wants_sentiment:
        widgets.append({"kind": "doughnut", "source": "bySentiment", "title": "Тональность обращений"})
    if wants_office:
        widgets.append({"kind": "list", "source": "byOffice", "title": "Распределение по офисам"})
    if wants_vip:
        widgets.append({"kind": "stat", "source": "vipShare", "title": "Доля VIP обращений", "helper": "От всех обращений"})
    if wants_priority:
        widgets.append({"kind": "stat", "source": "avgPriority", "title": "Средний приоритет", "helper": "По шкале 1-10"})
    if wants_queue:
        widgets.append({"kind": "stat", "source": "inRouting", "title": "В маршрутизации", "helper": "Ожидают назначения"})
    if wants_volume:
        widgets.append({"kind": "list", "source": "topCities", "title": "Топ города по обращениям", "topN": 3})

    if not widgets:
        widgets = [
            {"kind": "bar", "source": "byType", "title": "Типы обращений", "orientation": "horizontal"},
            {"kind": "bar", "source": "byCity", "title": "География обращений"},
        ]

    widgets = widgets[:4]
    titles = ", ".join(widget["title"] for widget in widgets)
    return {
        "reply": f"Собрал виджеты по запросу: {titles}. Если нужно, уточните срез (город, офис, тональность, VIP, очередь).",
        "widgets": widgets,
    }


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
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")), debug=True)
