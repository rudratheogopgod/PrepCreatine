"""
UPSC AI Study Planner — LangGraph State Schema
"""
from __future__ import annotations
from typing import Annotated, Any
from dataclasses import dataclass, field
from enum import Enum
import operator


class StudentLevel(str, Enum):
    BEGINNER     = "Beginner"
    INTERMEDIATE = "Intermediate"
    ADVANCED     = "Advanced"


class PlanStatus(str, Enum):
    PENDING    = "pending"
    IN_PROGRESS = "in_progress"
    COMPLETE   = "complete"
    INCOMPLETE = "incomplete"


@dataclass
class StudySession:
    """A single study block within a day."""
    subject: str
    topic: str
    hours: float
    session_type: str = "study"          # "study" | "revision" | "test"
    calendar_event_id: str | None = None
    status: PlanStatus = PlanStatus.PENDING
    completion_percent: float = 0.0


@dataclass
class DayPlan:
    """All sessions for a single day."""
    week: int
    day: int                              # 1–7
    date: str                             # ISO date string, e.g. "2025-06-02"
    sessions: list[StudySession] = field(default_factory=list)
    is_revision_day: bool = False
    is_test_day: bool = False


@dataclass
class WeekPlan:
    week: int
    days: list[DayPlan] = field(default_factory=list)
    weekly_test_scheduled: bool = False
    weekly_revision_scheduled: bool = False


# ── LangGraph typed state ──────────────────────────────────────────────────

class AgentState(dict):
    """
    Shared mutable state flowing through every LangGraph node.
    Keys are typed below for IDE support; LangGraph treats this as a plain dict.
    """

    # ── User inputs ────────────────────────────────────────────────────────
    user_id: str
    exam_name: str
    target_exam_year: int
    daily_study_hours: float
    student_level: StudentLevel
    plan_start_date: str                  # ISO "YYYY-MM-DD"
    syllabus: str

    # ── Generated plan ─────────────────────────────────────────────────────
    full_plan: list[WeekPlan]             # all weeks
    current_week: int
    current_day: int

    # ── Progress tracking ──────────────────────────────────────────────────
    # topic → completion_percent (0–100)
    progress_map: dict[str, float]

    # ── Scheduler outputs ──────────────────────────────────────────────────
    # calendar_event_id → session metadata
    scheduled_events: dict[str, Any]

    # ── Adjuster outputs ───────────────────────────────────────────────────
    incomplete_topics: list[dict]         # [{topic, subject, remaining_hours}]
    adjusted_days: list[DayPlan]          # patched future days

    # ── Control flow ───────────────────────────────────────────────────────
    next_action: str                      # drives conditional edges
    errors: list[str]
    messages: Annotated[list[str], operator.add]   # append-only log


def make_initial_state(
    user_id: str,
    exam_name: str,
    target_exam_year: int,
    daily_study_hours: float,
    student_level: str,
    plan_start_date: str,
) -> AgentState:
    return AgentState(
        user_id=user_id,
        exam_name=exam_name,
        target_exam_year=target_exam_year,
        daily_study_hours=daily_study_hours,
        student_level=StudentLevel(student_level),
        plan_start_date=plan_start_date,
        syllabus="",
        full_plan=[],
        current_week=1,
        current_day=1,
        progress_map={},
        scheduled_events={},
        incomplete_topics=[],
        adjusted_days=[],
        next_action="generate_plan",
        errors=[],
        messages=[],
    )
