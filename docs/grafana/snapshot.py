#!/usr/bin/env python3
"""
BetterPvP analytics snapshot script.

Queries the live betterpvp database and inserts a timestamped snapshot row
for each of the three analytics datasets:
  - role_matchup_snapshot
  - role_playtime_snapshot
  - skill_kdr_snapshot

Run this on a schedule (cron, Task Scheduler, Docker loop) — hourly is typical.

Environment variables:
  DB_HOST       (default: localhost)
  DB_PORT       (default: 5002)
  DB_NAME       (default: betterpvp)
  DB_USER       (default: user)
  DB_PASSWORD   (default: BetterPvP123!)
  REALM_IDS     (default: 6)  comma-separated list, e.g. "6,7,8"
"""

import os
import sys
from datetime import datetime, timezone

try:
    import psycopg2
except ImportError:
    print("psycopg2 not found. Install it with:  pip install psycopg2-binary")
    sys.exit(1)

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
DB_CONFIG = {
    "host":     os.getenv("DB_HOST",     "localhost"),
    "port":     int(os.getenv("DB_PORT", "5002")),
    "dbname":   os.getenv("DB_NAME",     "betterpvp"),
    "user":     os.getenv("DB_USER",     "user"),
    "password": os.getenv("DB_PASSWORD", "BetterPvP123!"),
}

REALM_IDS = [int(r.strip()) for r in os.getenv("REALM_IDS", "6").split(",")]


# ---------------------------------------------------------------------------
# Snapshot functions
# ---------------------------------------------------------------------------

def snapshot_role_matchup(cur, realm_id: int, ts: datetime) -> int:
    cur.execute("""
        WITH matchup_kills AS (
            SELECT
                ck.killer_class AS attacker,
                ck.victim_class AS defender,
                COUNT(*)        AS kills
            FROM kills k
            JOIN champions_kills ck ON k.id = ck.kill_id
            WHERE k.realm   = %(realm)s
              AND k.valid   = TRUE
              AND ck.killer_class <> ''
              AND ck.victim_class <> ''
            GROUP BY ck.killer_class, ck.victim_class
        )
        INSERT INTO role_matchup_snapshot
            (snapshot_time, realm, role, vs_role, kills, deaths, kdr)
        SELECT
            %(ts)s,
            %(realm)s,
            a.attacker,
            a.defender,
            a.kills,
            COALESCE(b.kills, 0),
            CASE
                WHEN COALESCE(b.kills, 0) = 0 THEN a.kills::NUMERIC
                ELSE ROUND(a.kills::NUMERIC / b.kills, 2)
            END
        FROM matchup_kills a
        LEFT JOIN matchup_kills b
               ON a.attacker = b.defender
              AND a.defender = b.attacker
    """, {"realm": realm_id, "ts": ts})
    return cur.rowcount


def snapshot_role_playtime(cur, realm_id: int, ts: datetime) -> int:
    cur.execute("""
        WITH role_time AS (
            SELECT
                StatData->'wrappedStat'->>'role' AS role,
                SUM(Stat)                        AS time_played_ms
            FROM client_stats
            WHERE StatType = 'CLANS_WRAPPER'
              AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_ROLE'
              AND StatData->'wrappedStat'->>'action'   = 'TIME_PLAYED'
              AND Realm = %(realm)s
            GROUP BY StatData->'wrappedStat'->>'role'
        ),
        role_kills AS (
            SELECT ck.killer_class AS role, COUNT(*) AS kills
            FROM kills k
            JOIN champions_kills ck ON k.id = ck.kill_id
            WHERE k.realm = %(realm)s AND k.valid = TRUE AND ck.killer_class <> ''
            GROUP BY ck.killer_class
        ),
        role_deaths AS (
            SELECT ck.victim_class AS role, COUNT(*) AS deaths
            FROM kills k
            JOIN champions_kills ck ON k.id = ck.kill_id
            WHERE k.realm = %(realm)s AND k.valid = TRUE AND ck.victim_class <> ''
            GROUP BY ck.victim_class
        )
        INSERT INTO role_playtime_snapshot
            (snapshot_time, realm, role, kills, deaths, kdr, time_played_ms)
        SELECT
            %(ts)s,
            %(realm)s,
            t.role,
            COALESCE(k.kills,  0),
            COALESCE(d.deaths, 0),
            CASE
                WHEN COALESCE(d.deaths, 0) = 0 THEN COALESCE(k.kills, 0)::NUMERIC
                ELSE ROUND(COALESCE(k.kills, 0)::NUMERIC / d.deaths, 2)
            END,
            t.time_played_ms
        FROM role_time t
        LEFT JOIN role_kills  k ON t.role = k.role
        LEFT JOIN role_deaths d ON t.role = d.role
    """, {"realm": realm_id, "ts": ts})
    return cur.rowcount


def snapshot_skill_kdr(cur, realm_id: int, ts: datetime) -> int:
    cur.execute("""
        WITH skill_kills AS (
            SELECT
                StatData->'wrappedStat'->>'skillName' AS skill_name,
                SUM(Stat)                             AS kills
            FROM client_stats
            WHERE StatType = 'CLANS_WRAPPER'
              AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
              AND StatData->'wrappedStat'->>'action'   = 'KILL'
              AND StatData->'wrappedStat'->>'skillName' IS NOT NULL
              AND StatData->'wrappedStat'->>'skillName' != ''
              AND Realm = %(realm)s
            GROUP BY StatData->'wrappedStat'->>'skillName'
        ),
        skill_deaths AS (
            SELECT
                StatData->'wrappedStat'->>'skillName' AS skill_name,
                SUM(Stat)                             AS deaths
            FROM client_stats
            WHERE StatType = 'CLANS_WRAPPER'
              AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
              AND StatData->'wrappedStat'->>'action'   = 'DEATH'
              AND StatData->'wrappedStat'->>'skillName' IS NOT NULL
              AND StatData->'wrappedStat'->>'skillName' != ''
              AND Realm = %(realm)s
            GROUP BY StatData->'wrappedStat'->>'skillName'
        ),
        skill_time AS (
            SELECT
                StatData->'wrappedStat'->>'skillName' AS skill_name,
                SUM(Stat)                             AS time_played_ms
            FROM client_stats
            WHERE StatType = 'CLANS_WRAPPER'
              AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
              AND StatData->'wrappedStat'->>'action'   = 'TIME_PLAYED'
              AND StatData->'wrappedStat'->>'skillName' IS NOT NULL
              AND StatData->'wrappedStat'->>'skillName' != ''
              AND Realm = %(realm)s
            GROUP BY StatData->'wrappedStat'->>'skillName'
        )
        INSERT INTO skill_kdr_snapshot
            (snapshot_time, realm, skill_name, kills, deaths, kdr, time_played_ms)
        SELECT
            %(ts)s,
            %(realm)s,
            k.skill_name,
            k.kills,
            COALESCE(d.deaths, 0),
            CASE
                WHEN COALESCE(d.deaths, 0) = 0 THEN k.kills::NUMERIC
                ELSE ROUND(k.kills::NUMERIC / d.deaths, 2)
            END,
            COALESCE(t.time_played_ms, 0)
        FROM skill_kills k
        LEFT JOIN skill_deaths d ON k.skill_name = d.skill_name
        LEFT JOIN skill_time   t ON k.skill_name = t.skill_name
    """, {"realm": realm_id, "ts": ts})
    return cur.rowcount


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    ts = datetime.now(tz=timezone.utc)
    print(f"[{ts.isoformat()}] Starting snapshot for realms: {REALM_IDS}")

    conn = psycopg2.connect(**DB_CONFIG)
    conn.autocommit = False

    try:
        with conn.cursor() as cur:
            for realm_id in REALM_IDS:
                n1 = snapshot_role_matchup(cur, realm_id, ts)
                n2 = snapshot_role_playtime(cur, realm_id, ts)
                n3 = snapshot_skill_kdr(cur, realm_id, ts)
                print(f"  realm={realm_id}: "
                      f"{n1} matchup rows, {n2} playtime rows, {n3} skill rows")
        conn.commit()
        print(f"[{ts.isoformat()}] Snapshot committed successfully.")
    except Exception as exc:
        conn.rollback()
        print(f"ERROR: {exc}", file=sys.stderr)
        sys.exit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    main()

