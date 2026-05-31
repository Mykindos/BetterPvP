# BetterPvP Grafana Integration

## Overview

This directory contains everything needed to push your Champions analytics and weapon
damage data into Grafana.

---

## Part 1 — Champions Analytics (Role Matchup, Playtime, Skill KDR)

### How it works

A Python script (`snapshot.py`) connects to PostgreSQL and **inserts a timestamped snapshot**
of each of the three stat queries into dedicated tables. You schedule this script to run
periodically (hourly, daily, etc.). Grafana queries the snapshot tables, giving you:

- Current values (latest snapshot)
- Historical trends (how KDR / playtime changed over time)

You can also query the live tables directly for the "current state" panels — the
`panels/` directory contains both live and snapshot variants.

### Setup Steps

#### 1. Apply the schema

```bash
psql -h localhost -p 5002 -U user -d betterpvp -f docs/grafana/schema.sql
```

#### 2. Seed initial snapshot

```bash
pip install psycopg2-binary
python docs/grafana/snapshot.py
```

Environment variables (all optional, defaults shown):
| Variable | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5002` |
| `DB_NAME` | `betterpvp` |
| `DB_USER` | `user` |
| `DB_PASSWORD` | `BetterPvP123!` |
| `REALM_IDS` | `6` (comma-separated list for multiple realms) |

#### 3. Schedule the snapshot script

**Linux/macOS — cron (hourly):**
```cron
0 * * * * cd /path/to/BetterPvP && python docs/grafana/snapshot.py >> /var/log/betterpvp_snapshot.log 2>&1
```

**Windows — Task Scheduler:**
Create a task that runs `python.exe docs\grafana\snapshot.py` hourly.

**Docker Compose add-on** (add to `docker/docker-compose.yml`):
```yaml
  snapshot-cron:
    image: python:3.12-slim
    volumes:
      - ./docs/grafana:/app
    working_dir: /app
    command: >
      sh -c "pip install psycopg2-binary -q &&
             while true; do python snapshot.py; sleep 3600; done"
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: betterpvp
      DB_USER: user
      DB_PASSWORD: BetterPvP123!
      REALM_IDS: "6"
    depends_on:
      postgres:
        condition: service_healthy
```

#### 4. Add PostgreSQL as a Grafana data source

In Grafana → Connections → Data sources → Add new → **PostgreSQL**:

| Field | Value |
|---|---|
| Host | `localhost:5002` (or `postgres:5432` if Grafana is also in Docker) |
| Database | `betterpvp` |
| User | `user` |
| Password | `BetterPvP123!` |
| TLS/SSL | Disabled |

Name it **"BetterPvP"** — the dashboard JSON references this name.

#### 5. Import the dashboard

Grafana → Dashboards → Import → Upload `docs/grafana/dashboards/champions_analytics.json`.
When prompted, select the **BetterPvP** PostgreSQL data source.

---

## Part 2 — Weapon Damage Range Visualization

### Can Grafana replace your Google Sheet candlestick chart?

**Yes.** Grafana's Bar Chart panel is the most natural fit:
- X axis = weapon configuration (e.g. "Ancient Sword +2B" for 2 Brutality runes)
- Three overlaid series: **Min**, **Base**, **Max** damage
- A second panel shows **DPS** (using `DEFAULT_DELAY = 400ms` → 2.5 base hits/sec)

For a true candlestick look, Grafana 10+ supports the Candlestick panel with a custom
X-axis field (non-time), mapping **Low = min**, **Open = base**, **Close = base**, **High = max**.

### Setup Steps

#### 1. Seed the weapon config table

```bash
psql -h localhost -p 5002 -U user -d betterpvp -f docs/grafana/schema.sql     # if not already done
psql -h localhost -p 5002 -U user -d betterpvp -f docs/grafana/weapon_seed.sql
```

Re-run `weapon_seed.sql` any time you change `weapon.yml` values.

#### 2. Import the dashboard

Grafana → Dashboards → Import → Upload `docs/grafana/dashboards/weapon_damage.json`.

### Panel query details

See `docs/grafana/panels/weapon_damage.sql` for the full query. Key notes:

- **Brutality rune bonus** is hard-coded to `1.0` per rune (from `BrutalityRune.java`).
  Update `weapon_damage_config.brutality_bonus` if you change the rune config.
- **Max sockets** per weapon is defined in `weapon_damage_config.max_sockets`.
  Update this column if you change socket counts in-game.
- **DPS formula**: `damage × (1000 / (400 / (1 + attack_speed_base)))` — based on
  `DEFAULT_DELAY = 400L` in `DamageCause.java`.

---

## Directory Structure

```
docs/grafana/
├── README.md                        ← This file
├── schema.sql                       ← Create snapshot + weapon config tables
├── snapshot.py                      ← Snapshot automation script
├── weapon_seed.sql                  ← Insert/update weapon config data
├── panels/
│   ├── role_matchup_kdr.sql         ← Grafana panel query (live + snapshot)
│   ├── role_playtime.sql            ← Grafana panel query (live + snapshot)
│   ├── skill_kdr.sql                ← Grafana panel query (live + snapshot)
│   └── weapon_damage.sql           ← Grafana panel query (damage + DPS)
└── dashboards/
    ├── champions_analytics.json     ← Importable Grafana dashboard
    └── weapon_damage.json           ← Importable Grafana dashboard
```

