from __future__ import annotations
from state import AgentState, StudySession


def reminder_agent(state: AgentState) -> dict:
    """
    Prepares the daily progress check.

    Behaviour:
    - If state["progress_map"] is already populated (real progress injected
      by the PrepCreatine frontend via POST /api/planner/today/session-complete
      → Java → POST /track-progress → this node), uses that directly.
    - If progress_map is empty (standalone CLI / demo run), simulates progress
      using a fixed random seed so results are reproducible.
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

    # Check if real progress was injected by the PrepCreatine backend
    existing_progress = state.get("progress_map", {})

    if existing_progress:
        # Real progress provided — format a confirmation message and pass through
        lines = [
            f"📝  Progress received — Week {today_plan.week},"
            f" Day {today_plan.day} ({today_plan.date})"
        ]
        for key, pct in existing_progress.items():
            subject_topic = key.replace("::", " – ")
            lines.append(f"  ✓ {subject_topic}: {pct:.0f}%")
        return {
            "progress_map": existing_progress,
            "next_action":  "track",
            "messages":     ["\n".join(lines)],
        }
    else:
        # No real progress — simulate for standalone demo/testing
        simulated = _simulate_user_progress(study_sessions)
        lines = [
            f"📝  Simulated Progress Check — Week {today_plan.week},"
            f" Day {today_plan.day} ({today_plan.date})"
        ]
        for key, pct in simulated.items():
            lines.append(f"  {key.replace('::', ' – ')}: {pct:.0f}%")
        return {
            "progress_map": simulated,
            "next_action":  "track",
            "messages":     ["\n".join(lines), "ℹ️  Progress simulated (no real data provided)."],
        }


def _simulate_user_progress(sessions: list[StudySession]) -> dict[str, float]:
    """Simulate realistic mixed progress for standalone demo/testing."""
    import random
    random.seed(42)
    return {
        f"{s.subject}::{s.topic}": random.choice([100, 100, 80, 60, 40])
        for s in sessions
    }