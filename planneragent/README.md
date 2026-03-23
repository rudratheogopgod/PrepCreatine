# PrepCreatine Planner Agent — LangGraph System

A production-ready AI agent system that generates a personalized study plan for rigorous exams (e.g. JEE/NEET),
tracks progress, schedules sessions in Google Calendar, and adapts the plan
daily based on actual completion.

---

## Architecture

```
START
  │
  ▼
┌─────────────┐      writes      ┌──────────────────┐
│ Planner     │ ──────────────►  │ daily_plans (PG) │
│ Agent       │                  └──────────────────┘
└──────┬──────┘
       │
       ▼
┌─────────────┐      creates     ┌──────────────────┐
│ Scheduler   │ ──────────────►  │ Google Calendar  │
│ Agent       │                  └──────────────────┘
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Reminder    │  ◄── runs daily (cron / APScheduler)
│ Agent       │      prompts user for % completion
└──────┬──────┘
       │
       ▼
┌─────────────┐      writes      ┌──────────────────────┐
│ Tracker     │ ──────────────►  │ progress_tracking    │
│ Agent       │                  │ (PostgreSQL)         │
└──────┬──────┘                  └──────────────────────┘
       │
  incomplete?
   YES │                  NO
       ▼                   ▼
┌─────────────┐      ┌─────────────┐
│ Adjuster    │      │ advance_day │
│ Agent       │─────►│    node     │
└─────────────┘      └──────┬──────┘
                            │
                      week done?
                       YES  │   NO
                            ▼   ▼
                           END reminder
```

---

## Agent Responsibilities

| Agent | Node Name | Responsibilities |
|---|---|---|
| **Planner** | `planner` | Calls LLM with UPSC syllabus + user inputs → generates 4-week plan → saves to `study_plan` table |
| **Scheduler** | `scheduler` | Iterates each session → creates Google Calendar event → stores `event_id` back into state |
| **Reminder** | `reminder` | Builds daily check-in prompt → collects completion % per topic |
| **Tracker** | `tracker` | Persists progress to `progress_tracking` table → identifies incomplete topics |
| **Adjuster** | `adjuster` | Calls LLM → redistributes incomplete work into next day's schedule → updates DB + Calendar |

---

## LangGraph State Schema

```python
class AgentState(dict):
    # Inputs
    user_id: str
    target_exam_year: int
    daily_study_hours: float
    student_level: StudentLevel      # Beginner | Intermediate | Advanced
    plan_start_date: str             # "YYYY-MM-DD"

    # Plan
    full_plan: list[WeekPlan]
    current_week: int
    current_day: int

    # Progress
    progress_map: dict[str, float]   # "Subject::Topic" → 0–100

    # Scheduler
    scheduled_events: dict[str, Any]

    # Adjuster
    incomplete_topics: list[dict]
    adjusted_days: list[DayPlan]

    # Control
    next_action: str
    errors: list[str]
    messages: Annotated[list[str], operator.add]
```

---

## PostgreSQL Schema

```sql
-- Stores the generated plan
CREATE TABLE study_plan (
    id                SERIAL PRIMARY KEY,
    user_id           TEXT NOT NULL,
    week              INT  NOT NULL,
    day               INT  NOT NULL,
    date              DATE NOT NULL,
    subject           TEXT NOT NULL,
    topic             TEXT NOT NULL,
    hours             NUMERIC(4,2) NOT NULL,
    session_type      TEXT NOT NULL DEFAULT 'study',
    status            TEXT NOT NULL DEFAULT 'pending',
    calendar_event_id TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, week, day, subject, topic)
);

-- Stores daily progress
CREATE TABLE progress_tracking (
    id                 SERIAL PRIMARY KEY,
    user_id            TEXT         NOT NULL,
    topic              TEXT         NOT NULL,
    subject            TEXT         NOT NULL,
    completion_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    date               DATE         NOT NULL DEFAULT CURRENT_DATE,
    notes              TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

---

## Installation

```bash
# 1. Clone and create venv
python -m venv .venv && source .venv/bin/activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Configure secrets
cp .env.example .env
# Edit .env with your PostgreSQL URL, Anthropic key, and Google credentials path

# 4. Set up Google Calendar OAuth
# Download credentials.json from Google Cloud Console (Calendar API enabled)
# The first run will open a browser for authorization

# 5. Create the database
createdb upsc_planner

# 6. Run
python graph.py \
  --user-id  "student_001" \
  --year     2026 \
  --hours    7 \
  --level    "Intermediate" \
  --start-date "2025-06-02"
```

---

## Example Execution Flow

```
[planner]   ✅  Generated 4-week plan with 168 sessions.
[scheduler] 📅  168 events created in Google Calendar.
[reminder]  📝  End-of-Day Progress Check — Week 1, Day 1 (2025-06-02)
            Please enter your completion % for each topic (0–100):
              1. Polity – Fundamental Rights:   ___%
              2. History – Revolt of 1857:       ___%
              3. Economy – GDP Concepts:         ___%
              4. Current Affairs – National:     ___%
[tracker]   💾  Saved 4 progress entries. 2 incomplete topics found.
[adjuster]  🔄  Added 2 catch-up sessions to Week 1 Day 2.
[advance]   ➡️   Moving to Week 1, Day 2.
[reminder]  📝  End-of-Day Progress Check — Week 1, Day 2 ...
...
[advance]   🏁  All days complete — plan finished!

════════════════════════
📊  Summary: 4 weeks | 196 study sessions
📅  Calendar events: 196
```

---

## Example Daily Schedule Generated

```
Week 1 — Day 1 (Monday, 2025-06-02)
┌────────────────────────────────────────────────────────┐
│ 06:00 – 08:00  Polity – Fundamental Rights       2 hrs │
│ 08:10 – 10:10  History – Revolt of 1857          2 hrs │
│ 10:20 – 11:20  Economy – GDP Basics              1 hr  │
│ 11:30 – 12:30  Current Affairs – National        1 hr  │
│ 13:30 – 14:30  Revision – Weekly topics          1 hr  │
└────────────────────────────────────────────────────────┘
Total: 7 hrs  |  Each block = 1 Google Calendar event
```

---

## Adaptive Planning Example

If student reports:
- Polity: 40%  →  remaining 1.2 hrs added to Day 2
- History: 60% →  remaining 0.8 hrs added to Day 2

Day 2 automatically gets two new sessions prepended:
```
[Continued] Polity – Fundamental Rights    1.2 hrs
[Continued] History – Revolt of 1857       0.8 hrs
```
Both sessions also get new Google Calendar events.

---

## Weekly Review (Every Sunday)

```
Week 1 — Day 7 (Sunday)
┌──────────────────────────────────────────────────┐
│ 07:00 – 10:00  Revision — Week 1 Topics  3 hrs   │
│ 10:30 – 12:30  Weekly Mock Test          2 hrs   │
└──────────────────────────────────────────────────┘
```

---

## File Structure

```
upsc_planner/
├── state.py          # AgentState, WeekPlan, DayPlan, StudySession
├── db_tools.py       # PostgreSQL LangChain tools
├── calendar_tools.py # Google Calendar LangChain tools
├── agents.py         # All 5 agent functions + advance_day node
├── graph.py          # LangGraph graph wiring + CLI entry point
├── requirements.txt
└── .env.example
```

---

## Production Considerations

- **Cron trigger**: Run `reminder_agent` daily at 20:00 IST via APScheduler or a cron job; the rest of the loop (tracker → adjuster → advance) fires immediately after.
- **Async**: Replace `psycopg2` with `asyncpg` and use `langgraph.graph.AsyncStateGraph` for non-blocking DB calls.
- **Multi-user**: Each user gets their own `user_id` — all DB queries are scoped by it.
- **Observability**: Enable LangSmith tracing with `LANGCHAIN_TRACING_V2=true`.
- **Error handling**: Wrap each agent in a try/except that appends to `state["errors"]` and sets `next_action = "error"` for a dedicated error-handling node.
