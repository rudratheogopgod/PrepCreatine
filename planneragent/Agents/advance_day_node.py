from __future__ import annotations

from state import AgentState
from utils.utilities import _advance_day


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
