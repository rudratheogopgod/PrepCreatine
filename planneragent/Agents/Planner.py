"""
UPSC AI Study Planner — Five LangGraph Agents
Each agent is a callable that receives AgentState and returns a state patch.
"""
from __future__ import annotations
import json

from langchain_core.messages import HumanMessage, SystemMessage

from state import AgentState, DayPlan, StudySession, WeekPlan, StudentLevel
from db_tools import save_study_plan
import os
from langchain_groq import ChatGroq
from dotenv import load_dotenv
load_dotenv()

LLM = ChatGroq(api_key=os.getenv("API_GROQ") , model = "llama-3.3-70b-versatile" , temperature=0)
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


LEVEL_TOPIC_COUNT = {
    StudentLevel.BEGINNER:     6,
    StudentLevel.INTERMEDIATE: 8,
    StudentLevel.ADVANCED:     10,
}

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
