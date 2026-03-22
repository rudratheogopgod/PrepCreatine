from __future__ import annotations
import json
from datetime import datetime, timedelta
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage

from state import AgentState

from calendar_tools import create_calendar_event


def scheduler_agent(state: AgentState) -> dict:
    """
    For each session in the plan, creates a Google Calendar event.
    Slots sessions back-to-back starting at 06:00 IST each day.
    """
    scheduled_events: dict[str, Any] = {}
    updated_plan = state["full_plan"]

    for week_plan in updated_plan:
        for day_plan in week_plan.days:
            current_start = datetime.strptime(f"{day_plan.date} 06:00", "%Y-%m-%d %H:%M")

            for sess in day_plan.sessions:
                title = f"{sess.subject} – {sess.topic}"
                if sess.session_type == "revision":
                    title = f"[Revision] {sess.topic}"
                elif sess.session_type == "test":
                    title = f"[Weekly Test] Week {day_plan.week}"

                payload = json.dumps({
                    "title":       title,
                    "date":        day_plan.date,
                    "start_time":  current_start.strftime("%H:%M"),
                    "hours":       sess.hours,
                    "description": (
                        f"{state.get('exam_name', 'Study Plan')} | Week {day_plan.week} Day {day_plan.day} | "
                        f"{sess.session_type.title()}"
                    ),
                })

                result_raw = create_calendar_event.invoke(payload)
                result     = json.loads(result_raw)

                event_id = result["event_id"]
                sess.calendar_event_id = event_id
                scheduled_events[event_id] = {
                    "week":    day_plan.week,
                    "day":     day_plan.day,
                    "date":    day_plan.date,
                    "subject": sess.subject,
                    "topic":   sess.topic,
                    "link":    result.get("html_link", ""),
                }

                # Advance clock by session length + 10-min break
                current_start += timedelta(hours=sess.hours, minutes=10)

    return {
        "full_plan":       updated_plan,
        "scheduled_events": scheduled_events,
        "next_action":     "remind",
        "messages":        [f"📅  Scheduler: {len(scheduled_events)} events created in Google Calendar."],
    }
