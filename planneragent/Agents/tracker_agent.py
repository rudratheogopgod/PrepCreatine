from __future__ import annotations
import json

from state import AgentState
from db_tools import save_progress
from calendar_tools import create_calendar_event, CALENDAR_TOOLS

def tracker_agent(state: AgentState) -> dict:
    """
    Persists user progress to PostgreSQL and identifies incomplete topics.
    """
    week_idx = state["current_week"] - 1
    day_idx  = state["current_day"] - 1
    today    = state["full_plan"][week_idx].days[day_idx]

    incomplete_topics: list[dict] = []

    for key, pct in state["progress_map"].items():
        subject, topic = key.split("::", 1)
        date = today.date

        # Save to DB
        save_progress.invoke(json.dumps({
            "user_id":            state["user_id"],
            "topic":              topic,
            "subject":            subject,
            "completion_percent": pct,
            "date":               date,
            "notes":              f"Week {today.week}, Day {today.day}",
        }))

        if pct < 100:
            # Calculate remaining hours
            session = next(
                (s for s in today.sessions if s.topic == topic),
                None,
            )
            if session:
                remaining_hours = round(session.hours * (1 - pct / 100), 2)
                incomplete_topics.append({
                    "topic":           topic,
                    "subject":         subject,
                    "completion_pct":  pct,
                    "remaining_hours": remaining_hours,
                    "original_week":   today.week,
                    "original_day":    today.day,
                })

    next_action = "adjust" if incomplete_topics else "next_day"

    return {
        "incomplete_topics": incomplete_topics,
        "next_action":       next_action,
        "messages": [
            f"💾  Tracker: Saved {len(state['progress_map'])} progress entries. "
            f"{len(incomplete_topics)} incomplete topics found."
        ],
    }
