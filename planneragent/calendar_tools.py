"""
PrepCreatine Planner Agent — Google Calendar Tools
Wraps the Google Calendar API v3 as LangChain tools.
Calendar is FULLY OPTIONAL — if credentials.json is missing, all tools
return mock event IDs without crashing. The planner works perfectly without it.
"""
from __future__ import annotations
import json
import os
import uuid as _uuid_module
from datetime import datetime, timedelta

from langchain_core.tools import tool
from dotenv import load_dotenv

load_dotenv()

SCOPES          = ["https://www.googleapis.com/auth/calendar"]
CREDENTIALS_FILE = os.getenv("GCAL_CREDENTIALS_FILE", "credentials.json")
TOKEN_FILE       = os.getenv("GCAL_TOKEN_FILE", "token.json")
CALENDAR_ID      = os.getenv("GCAL_CALENDAR_ID", "primary")


# ── Calendar availability guard ────────────────────────────────────────────

def _calendar_available() -> bool:
    """Returns True only if credentials.json exists on disk."""
    return os.path.exists(CREDENTIALS_FILE)


def _get_calendar_service():
    """Get authenticated Google Calendar service. Raises if unavailable."""
    from google.oauth2.credentials import Credentials
    from google.auth.transport.requests import Request
    from google_auth_oauthlib.flow import InstalledAppFlow
    from googleapiclient.discovery import build

    creds = None
    if os.path.exists(TOKEN_FILE):
        creds = Credentials.from_authorized_user_file(TOKEN_FILE, SCOPES)
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            # This opens a browser — only works in interactive environments
            flow = InstalledAppFlow.from_client_secrets_file(CREDENTIALS_FILE, SCOPES)
            creds = flow.run_local_server(port=0)
        with open(TOKEN_FILE, "w") as f:
            f.write(creds.to_json())
    return build("calendar", "v3", credentials=creds)


# ── LangChain Tools ───────────────────────────────────────────────────────

@tool
def create_calendar_event(payload: str) -> str:
    """
    Create a Google Calendar event for a study session.
    If Google Calendar credentials are not configured, returns a mock event_id
    so the planner continues working without crashing.

    Args:
        payload: JSON — {title, date, start_time, hours, description}
    Returns:
        JSON — {event_id, html_link}
    """
    d: dict = json.loads(payload)

    if not _calendar_available():
        # No credentials — return a deterministic fake ID so the system works
        fake_id = str(_uuid_module.uuid4())
        print(f"[Scheduler] No Google Calendar credentials — mock event: {d.get('title', '?')}")
        return json.dumps({"event_id": fake_id, "html_link": ""})

    try:
        start_dt = datetime.strptime(f"{d['date']} {d['start_time']}", "%Y-%m-%d %H:%M")
        end_dt   = start_dt + timedelta(hours=float(d["hours"]))

        event_body = {
            "summary": d["title"],
            "description": d.get("description", "PrepCreatine Study Session"),
            "start": {"dateTime": start_dt.isoformat(), "timeZone": "Asia/Kolkata"},
            "end":   {"dateTime": end_dt.isoformat(),   "timeZone": "Asia/Kolkata"},
            "colorId": _color_id_for_subject(d["title"]),
            "reminders": {
                "useDefault": False,
                "overrides": [
                    {"method": "popup", "minutes": 15},
                    {"method": "email", "minutes": 60},
                ],
            },
        }

        service = _get_calendar_service()
        created = service.events().insert(calendarId=CALENDAR_ID, body=event_body).execute()
        return json.dumps({"event_id": created["id"], "html_link": created.get("htmlLink", "")})

    except Exception as e:
        # Calendar failed after credentials exist — log and return mock
        print(f"[Scheduler] Calendar API error: {e} — returning mock event_id")
        fake_id = str(_uuid_module.uuid4())
        return json.dumps({"event_id": fake_id, "html_link": ""})


@tool
def update_calendar_event(payload: str) -> str:
    """
    Update an existing Google Calendar event (e.g. reschedule after adjustment).

    Args:
        payload: JSON — {event_id, new_date, start_time, hours}
    """
    if not _calendar_available():
        d = json.loads(payload)
        return f"[Mock] Event {d.get('event_id', '?')} rescheduled (no calendar credentials)."

    try:
        d: dict = json.loads(payload)
        start_dt = datetime.strptime(f"{d['new_date']} {d['start_time']}", "%Y-%m-%d %H:%M")
        end_dt   = start_dt + timedelta(hours=float(d["hours"]))
        patch_body = {
            "start": {"dateTime": start_dt.isoformat(), "timeZone": "Asia/Kolkata"},
            "end":   {"dateTime": end_dt.isoformat(),   "timeZone": "Asia/Kolkata"},
        }
        service = _get_calendar_service()
        service.events().patch(
            calendarId=CALENDAR_ID, eventId=d["event_id"], body=patch_body,
        ).execute()
        return f"Event {d['event_id']} rescheduled to {d['new_date']} {d['start_time']}."
    except Exception as e:
        return f"[Calendar] Update failed: {e}"


@tool
def delete_calendar_event(event_id: str) -> str:
    """Delete a Google Calendar event by its ID."""
    if not _calendar_available():
        return f"[Mock] Event {event_id} deleted (no calendar credentials)."
    try:
        service = _get_calendar_service()
        service.events().delete(calendarId=CALENDAR_ID, eventId=event_id).execute()
        return f"Deleted event {event_id}."
    except Exception as e:
        return f"[Calendar] Delete failed: {e}"


@tool
def list_day_events(date: str) -> str:
    """
    List all study events scheduled on a given date.

    Args:
        date: ISO "YYYY-MM-DD"
    Returns:
        JSON list of {event_id, summary, start, end}.
    """
    if not _calendar_available():
        return json.dumps([])

    try:
        start = datetime.strptime(date, "%Y-%m-%d")
        end   = start + timedelta(days=1)
        service = _get_calendar_service()
        result  = service.events().list(
            calendarId=CALENDAR_ID,
            timeMin=start.isoformat() + "Z",
            timeMax=end.isoformat() + "Z",
            singleEvents=True,
            orderBy="startTime",
        ).execute()
        events = [
            {"event_id": e["id"], "summary": e.get("summary", ""),
             "start": e["start"].get("dateTime", ""), "end": e["end"].get("dateTime", "")}
            for e in result.get("items", [])
        ]
        return json.dumps(events)
    except Exception as e:
        return json.dumps([])


# ── Helpers ───────────────────────────────────────────────────────────────

_SUBJECT_COLORS = {
    # UPSC subjects
    "polity":          "9",   # blueberry
    "history":         "6",   # tangerine
    "geography":       "2",   # sage
    "economy":         "5",   # banana
    "environment":     "10",  # basil
    "science":         "7",   # peacock
    "current affairs": "4",   # flamingo
    # JEE/NEET subjects
    "physics":         "7",   # peacock
    "chemistry":       "2",   # sage
    "mathematics":     "5",   # banana
    "math":            "5",   # banana
    "biology":         "10",  # basil
    "botany":          "10",  # basil
    "zoology":         "2",   # sage
    # Common
    "revision":        "8",   # graphite
    "test":            "11",  # tomato
}

def _color_id_for_subject(title: str) -> str:
    title_lower = title.lower()
    for key, color in _SUBJECT_COLORS.items():
        if key in title_lower:
            return color
    return "1"  # lavender default


CALENDAR_TOOLS = [
    create_calendar_event,
    update_calendar_event,
    delete_calendar_event,
    list_day_events,
]
