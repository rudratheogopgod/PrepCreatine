from __future__ import annotations
import json
from langchain_core.messages import HumanMessage, SystemMessage

from state import AgentState,StudySession
from db_tools import save_study_plan
from utils.utilities import _advance_day, _find_day_plan
import os
from langchain_groq import ChatGroq
from dotenv import load_dotenv
load_dotenv()

LLM = ChatGroq(api_key=os.getenv("API_GROQ") , model = "llama-3.3-70b-versatile" , temperature=0)


def adjuster_agent(state: AgentState) -> dict:
    """
    Uses LLM to redistribute incomplete topics into the next available day.
    Updates the full_plan in state and persists changes to PostgreSQL.
    """
    incomplete = state["incomplete_topics"]
    full_plan  = state["full_plan"]

    exam_name = state.get('exam_name', 'UPSC')
    system_prompt = f"""You are a {exam_name} study plan optimizer.
Given incomplete topics, insert them into the next day's schedule
without exceeding the daily study hour limit.
Respond ONLY with valid JSON."""

    # Find next day's plan
    current_week = state["current_week"]
    current_day  = state["current_day"]
    next_week, next_day = _advance_day(current_week, current_day)

    next_day_plan = _find_day_plan(full_plan, next_week, next_day)
    if next_day_plan is None:
        return {
            "next_action": "next_day",
            "messages": ["⚠️  Adjuster: No future day found for rescheduling."],
        }

    current_hours = sum(s.hours for s in next_day_plan.sessions)
    max_hours     = state["daily_study_hours"]
    available     = round(max_hours - current_hours, 2)

    user_prompt = f"""
Incomplete topics to reschedule:
{json.dumps(incomplete, indent=2)}

Next day ({next_day_plan.date}) current load: {current_hours} hrs
Maximum daily hours: {max_hours} hrs
Available capacity: {available} hrs

Return JSON:
{{
  "added_sessions": [
    {{
      "subject": "...",
      "topic": "... (continued)",
      "hours": 1.0,
      "session_type": "study"
    }}
  ],
  "adjustment_notes": "..."
}}
Fit as many incomplete topics as possible within available capacity.
Reduce hours proportionally if needed.
"""

    response = LLM.invoke([
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_prompt),
    ])

    adj_data   = json.loads(response.content.strip())
    new_sessions = [
        StudySession(
            subject=s["subject"],
            topic=s["topic"],
            hours=float(s["hours"]),
            session_type=s.get("session_type", "study"),
        )
        for s in adj_data.get("added_sessions", [])
    ]

    # Patch the plan in memory
    next_day_plan.sessions.extend(new_sessions)

    # Persist changes to DB
    db_rows = [
        {
            "user_id":      state["user_id"],
            "week":         next_week,
            "day":          next_day,
            "date":         next_day_plan.date,
            "subject":      s.subject,
            "topic":        s.topic,
            "hours":        s.hours,
            "session_type": s.session_type,
            "status":       "pending",
        }
        for s in new_sessions
    ]
    if db_rows:
        save_study_plan.invoke(json.dumps(db_rows))

    return {
        "full_plan":      full_plan,
        "adjusted_days":  [next_day_plan],
        "next_action":    "next_day",
        "messages": [
            f"🔄  Adjuster: Added {len(new_sessions)} catch-up sessions to "
            f"Week {next_week} Day {next_day}. Note: {adj_data.get('adjustment_notes', '')}"
        ],
    }
