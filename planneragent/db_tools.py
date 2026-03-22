"""
PrepCreatine Planner Agent — PostgreSQL database tools
Uses psycopg2. All DDL + CRUD helpers live here.
Tables are prefixed with agent_ to avoid conflict with PrepCreatine's own tables.
"""
from __future__ import annotations
import json
import os
from contextlib import contextmanager
from typing import Generator

import psycopg2
import psycopg2.extras
from langchain_core.tools import tool

from state import DayPlan, StudySession, PlanStatus
from dotenv import load_dotenv

load_dotenv()
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://localhost:5432/prepcreatine")


# ── Connection helper ──────────────────────────────────────────────────────

@contextmanager
def get_conn() -> Generator:
    conn = psycopg2.connect(DATABASE_URL)
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


# ── DDL — run once ─────────────────────────────────────────────────────────

CREATE_TABLES_SQL = """
CREATE TABLE IF NOT EXISTS agent_study_plan (
    id                SERIAL PRIMARY KEY,
    user_id           TEXT        NOT NULL,
    week              INT         NOT NULL,
    day               INT         NOT NULL,
    date              DATE        NOT NULL,
    subject           TEXT        NOT NULL,
    topic             TEXT        NOT NULL,
    hours             NUMERIC(4,2) NOT NULL,
    session_type      TEXT        NOT NULL DEFAULT 'study',
    status            TEXT        NOT NULL DEFAULT 'pending',
    calendar_event_id TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, week, day, subject, topic)
);

CREATE TABLE IF NOT EXISTS agent_progress_tracking (
    id                 SERIAL PRIMARY KEY,
    user_id            TEXT         NOT NULL,
    topic              TEXT         NOT NULL,
    subject            TEXT         NOT NULL,
    completion_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    date               DATE         NOT NULL DEFAULT CURRENT_DATE,
    notes              TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_plan_user_week
    ON agent_study_plan(user_id, week, day);
CREATE INDEX IF NOT EXISTS idx_agent_progress_user_topic
    ON agent_progress_tracking(user_id, topic);
"""


def initialize_db() -> None:
    """Create agent tables if they don't exist."""
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(CREATE_TABLES_SQL)
    print("✅  Agent database tables ready (agent_study_plan, agent_progress_tracking).")


# ── LangChain Tools ────────────────────────────────────────────────────────

@tool
def save_study_plan(payload: str) -> str:
    """
    Persist a list of study sessions to the agent_study_plan table.

    Args:
        payload: JSON string — list of dicts with keys:
                 user_id, week, day, date, subject, topic,
                 hours, session_type, status
    Returns:
        Confirmation message.
    """
    sessions: list[dict] = json.loads(payload)
    insert_sql = """
        INSERT INTO agent_study_plan
            (user_id, week, day, date, subject, topic, hours, session_type, status)
        VALUES
            (%(user_id)s, %(week)s, %(day)s, %(date)s, %(subject)s,
             %(topic)s, %(hours)s, %(session_type)s, %(status)s)
        ON CONFLICT (user_id, week, day, subject, topic) DO UPDATE
            SET hours        = EXCLUDED.hours,
                session_type = EXCLUDED.session_type,
                status       = EXCLUDED.status;
    """
    with get_conn() as conn:
        with conn.cursor() as cur:
            psycopg2.extras.execute_batch(cur, insert_sql, sessions)
    return f"Saved {len(sessions)} sessions to agent_study_plan."


@tool
def update_session_status(payload: str) -> str:
    """
    Update the status and calendar_event_id for a study session row.

    Args:
        payload: JSON — {user_id, week, day, subject, topic,
                         status, calendar_event_id (optional)}
    """
    d: dict = json.loads(payload)
    sql = """
        UPDATE agent_study_plan
           SET status             = %(status)s,
               calendar_event_id  = COALESCE(%(event_id)s, calendar_event_id)
         WHERE user_id = %(user_id)s
           AND week    = %(week)s
           AND day     = %(day)s
           AND subject = %(subject)s
           AND topic   = %(topic)s;
    """
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, {**d, "event_id": d.get("calendar_event_id")})
    return "Session status updated."


@tool
def save_progress(payload: str) -> str:
    """
    Insert a progress record into agent_progress_tracking.

    Args:
        payload: JSON — {user_id, topic, subject, completion_percent, date, notes?}
    """
    d: dict = json.loads(payload)
    sql = """
        INSERT INTO agent_progress_tracking
            (user_id, topic, subject, completion_percent, date, notes)
        VALUES
            (%(user_id)s, %(topic)s, %(subject)s, %(completion_percent)s,
             %(date)s, %(notes)s);
    """
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, {**d, "notes": d.get("notes", "")})
    return "Progress saved."


@tool
def get_incomplete_topics(user_id: str) -> str:
    """
    Fetch all topics with completion_percent < 100 for a user.

    Returns:
        JSON list of {topic, subject, completion_percent, date}.
    """
    sql = """
        SELECT DISTINCT ON (topic)
               topic, subject, completion_percent, date::text
          FROM agent_progress_tracking
         WHERE user_id = %s
           AND completion_percent < 100
         ORDER BY topic, date DESC;
    """
    with get_conn() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql, (user_id,))
            rows = cur.fetchall()
    return json.dumps([dict(r) for r in rows])


@tool
def get_week_plan(payload: str) -> str:
    """
    Retrieve study plan rows for a given user + week from agent_study_plan.

    Args:
        payload: JSON — {user_id, week}
    Returns:
        JSON list of session dicts.
    """
    d: dict = json.loads(payload)
    sql = """
        SELECT week, day, date::text, subject, topic, hours,
               session_type, status, calendar_event_id
          FROM agent_study_plan
         WHERE user_id = %(user_id)s
           AND week    = %(week)s
         ORDER BY day, hours DESC;
    """
    with get_conn() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql, d)
            rows = cur.fetchall()
    return json.dumps([dict(r) for r in rows])


# expose as a flat list for agent tool registration
DB_TOOLS = [
    save_study_plan,
    update_session_status,
    save_progress,
    get_incomplete_topics,
    get_week_plan,
]
