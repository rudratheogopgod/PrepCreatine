from __future__ import annotations

from state import DayPlan, WeekPlan

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