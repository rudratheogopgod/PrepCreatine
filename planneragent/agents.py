"""
UPSC AI Study Planner — Five LangGraph Agents
Each agent is a callable that receives AgentState and returns a state patch.
"""
from __future__ import annotations
import json
from datetime import datetime, timedelta
from typing import Any

from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage, SystemMessage

from state import AgentState, DayPlan, StudySession, WeekPlan, StudentLevel
from db_tools import (
    save_study_plan, save_progress,
    get_incomplete_topics, DB_TOOLS,
)
from calendar_tools import create_calendar_event, CALENDAR_TOOLS

# ── Shared LLM ─────────────────────────────────────────────────────────────

LLM = ChatAnthropic(model="claude-opus-4-5", temperature=0.2)

# ── UPSC Syllabus reference (condensed) ──────────────────────────────────

UPSC_SYLLABUS = {
    "Polity": [
        "Constitution – Preamble & Schedules",
        "Fundamental Rights",
        "Directive Principles",
        "Parliament & State Legislature",
        "President & Governor",
        "Judiciary & Supreme Court",
        "Federalism",
        "Local Self Government",
        "Constitutional Amendments",
        "Emergency Provisions",
    ],
    "History": [
        "Ancient India – Indus Valley & Vedic",
        "Ancient India – Maurya & Gupta",
        "Medieval India – Sultanate & Mughal",
        "Revolt of 1857",
        "Indian National Movement – Early Phase",
        "Gandhi & Mass Movements",
        "Independence & Partition",
        "Post-Independence Consolidation",
        "Art & Culture",
    ],
    "Geography": [
        "Physical Geography – Landforms",
        "Climate & Monsoon",
        "Rivers & Water Bodies",
        "Natural Resources",
        "Human Geography",
        "Economic Geography",
        "World Geography",
    ],
    "Economy": [
        "Basic Concepts – GDP, Growth, Inflation",
        "Indian Economic Planning",
        "Agriculture & Food Security",
        "Industry & Manufacturing",
        "Banking & Monetary Policy",
        "Fiscal Policy & Budget",
        "International Trade & WTO",
        "Poverty & Social Sector",
    ],
    "Environment": [
        "Ecology & Biodiversity",
        "Climate Change & UNFCCC",
        "Pollution & Waste",
        "Conservation",
        "Environmental Laws",
    ],
    "Science & Technology": [
        "General Science – Physics Basics",
        "General Science – Chemistry Basics",
        "Space & ISRO",
        "Biotechnology",
        "Defence Technology",
        "IT & Cybersecurity",
    ],
    "Current Affairs": [
        "National Affairs",
        "International Relations",
        "Government Schemes",
        "Reports & Indices",
        "Science in News",
    ],
}

# ── Topic difficulty multipliers ──────────────────────────────────────────

LEVEL_TOPIC_COUNT = {
    StudentLevel.BEGINNER:     6,
    StudentLevel.INTERMEDIATE: 8,
    StudentLevel.ADVANCED:     10,
}


# ═══════════════════════════════════════════════════════════════════════════
# 1. PLANNER AGENT
# ═══════════════════════════════════════════════════════════════════════════

def planner_agent(state: AgentState) -> dict:
    """
    Generates a complete weekly study plan using the LLM.
    Stores the plan in state['full_plan'] and persists rows to PostgreSQL.
    """
    level = state["student_level"]
    hours = state["daily_study_hours"]
    topics_per_day = LEVEL_TOPIC_COUNT[level]

    exam_name = state.get("exam_name", "UPSC")
    system_prompt = f"""You are an expert {exam_name} coaching strategist.
Generate a detailed, realistic weekly study plan in strict JSON.
Respond ONLY with valid JSON — no explanation, no markdown fences."""

    user_prompt = f"""
Student level : {level.value}
Daily hours   : {hours} hours
Start date    : {state['plan_start_date']}
Target year   : {state['target_exam_year']}
Exam          : {exam_name}

Syllabus retrieved from web search:
{state.get('syllabus', 'No syllabus found. Please use your internal knowledge.')}

Based on the provided syllabus, generate the topics.
Generate a 4-week plan. Each day (Mon-Sat) should have {topics_per_day} sessions.
Day 7 (Sunday) = revision + weekly test.

Return JSON matching this exact schema:
{{
  "weeks": [
    {{
      "week": 1,
      "days": [
        {{
          "day": 1,
          "date": "YYYY-MM-DD",
          "is_revision_day": false,
          "is_test_day": false,
          "sessions": [
            {{
              "subject": "Polity",
              "topic": "Fundamental Rights",
              "hours": 2.0,
              "session_type": "study"
            }}
          ]
        }}
      ]
    }}
  ]
}}

Rules:
- Total daily study hours ≈ {hours}
- Include 1 revision session (1 hr) every study day
- Sunday: is_revision_day=true, is_test_day=true, session_type="revision"/"test"
- Distribute subjects evenly; no subject repeated on the same day
- Beginner: simpler topics first; Advanced: include more analytical topics
"""

    response = LLM.invoke([
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_prompt),
    ])

    raw_json = response.content.strip()
    plan_data = json.loads(raw_json)

    # ── Parse JSON → WeekPlan dataclass list ──────────────────────────────
    full_plan: list[WeekPlan] = []
    db_rows: list[dict] = []

    for w_raw in plan_data["weeks"]:
        week_obj = WeekPlan(week=w_raw["week"])

        for d_raw in w_raw["days"]:
            sessions = [
                StudySession(
                    subject=s["subject"],
                    topic=s["topic"],
                    hours=float(s["hours"]),
                    session_type=s.get("session_type", "study"),
                )
                for s in d_raw["sessions"]
            ]
            day_obj = DayPlan(
                week=w_raw["week"],
                day=d_raw["day"],
                date=d_raw["date"],
                sessions=sessions,
                is_revision_day=d_raw.get("is_revision_day", False),
                is_test_day=d_raw.get("is_test_day", False),
            )
            week_obj.days.append(day_obj)

            # Build flat rows for PostgreSQL
            for sess in sessions:
                db_rows.append({
                    "user_id":      state["user_id"],
                    "week":         w_raw["week"],
                    "day":          d_raw["day"],
                    "date":         d_raw["date"],
                    "subject":      sess.subject,
                    "topic":        sess.topic,
                    "hours":        sess.hours,
                    "session_type": sess.session_type,
                    "status":       "pending",
                })

        full_plan.append(week_obj)

    # ── Persist to DB ─────────────────────────────────────────────────────
    save_study_plan.invoke(json.dumps(db_rows))

    return {
        "full_plan":   full_plan,
        "next_action": "schedule",
        "messages":    [f"✅  Planner: Generated {len(full_plan)}-week plan with {len(db_rows)} sessions."],
    }


# ═══════════════════════════════════════════════════════════════════════════
# 2. SCHEDULER AGENT
# ═══════════════════════════════════════════════════════════════════════════

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


# ═══════════════════════════════════════════════════════════════════════════
# 3. REMINDER AGENT
# ═══════════════════════════════════════════════════════════════════════════

def reminder_agent(state: AgentState) -> dict:
    """
    Builds the daily check-in prompt and collects user progress responses.
    In production this fires via a scheduler/cron; here it simulates one day.
    """
    week_idx = state["current_week"] - 1
    day_idx  = state["current_day"] - 1

    try:
        today_plan = state["full_plan"][week_idx].days[day_idx]
    except IndexError:
        return {
            "next_action": "complete",
            "messages":    ["🏁  All weeks complete — study plan finished!"],
        }

    study_sessions = [s for s in today_plan.sessions if s.session_type == "study"]

    # Build the check-in message shown to the user
    lines = [f"📝  End-of-Day Progress Check — Week {today_plan.week}, Day {today_plan.day} ({today_plan.date})"]
    lines.append("Please enter your completion % for each topic (0–100):\n")
    for i, sess in enumerate(study_sessions, 1):
        lines.append(f"  {i}. {sess.subject} – {sess.topic}:  ___%")

    reminder_message = "\n".join(lines)

    # ── Simulate progress for demo (replace with real user input loop) ────
    simulated_progress = _simulate_user_progress(study_sessions)

    return {
        "progress_map":  simulated_progress,
        "next_action":   "track",
        "messages":      [reminder_message, "ℹ️   Reminder: Progress collected (simulated)."],
    }


def _simulate_user_progress(sessions: list[StudySession]) -> dict[str, float]:
    """Simulate realistic mixed progress for demo purposes."""
    import random
    random.seed(42)
    return {
        f"{s.subject}::{s.topic}": random.choice([100, 100, 80, 60, 40])
        for s in sessions
    }


# ═══════════════════════════════════════════════════════════════════════════
# 4. TRACKER AGENT
# ═══════════════════════════════════════════════════════════════════════════

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


# ═══════════════════════════════════════════════════════════════════════════
# 5. ADJUSTER AGENT
# ═══════════════════════════════════════════════════════════════════════════

def adjuster_agent(state: AgentState) -> dict:
    """
    Uses LLM to redistribute incomplete topics into the next available day.
    Updates the full_plan in state and persists changes to PostgreSQL.
    """
    incomplete = state["incomplete_topics"]
    full_plan  = state["full_plan"]

    exam_name = state.get("exam_name", "UPSC")
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


# ── Helper utilities ──────────────────────────────────────────────────────

def _advance_day(week: int, day: int) -> tuple[int, int]:
    if day < 7:
        return week, day + 1
    return week + 1, 1


def _find_day_plan(plan: list[WeekPlan], week: int, day: int) -> DayPlan | None:
    for wp in plan:
        if wp.week == week:
            for dp in wp.days:
                if dp.day == day:
                    return dp
    return None


# ── Day-counter node ──────────────────────────────────────────────────────

def advance_day_node(state: AgentState) -> dict:
    """Increment current_day / current_week and decide if plan is done."""
    week, day = _advance_day(state["current_week"], state["current_day"])
    total_weeks = len(state["full_plan"])

    if week > total_weeks:
        return {
            "current_week": week,
            "current_day":  day,
            "next_action":  "complete",
            "messages":     ["🏁  All days complete — plan finished!"],
        }

    return {
        "current_week": week,
        "current_day":  day,
        "next_action":  "remind",
        "messages":     [f"➡️   Moving to Week {week}, Day {day}."],
    }
