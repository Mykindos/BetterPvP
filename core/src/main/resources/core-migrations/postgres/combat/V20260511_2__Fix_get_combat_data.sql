CREATE OR REPLACE FUNCTION get_combat_data(client_uuid VARCHAR(36), realm_param INTEGER)
    RETURNS TABLE(kills INTEGER, deaths INTEGER, assists INTEGER, rating INTEGER, killstreak INTEGER, highest_killstreak INTEGER)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
SELECT COALESCE(SUM(k.kills_count), 0)::INTEGER    AS kills,
    COALESCE(SUM(d.deaths_count), 0)::INTEGER   AS deaths,
    COALESCE(SUM(ac.assists_count), 0)::INTEGER AS assists,
    MAX(cs.rating)                              AS rating,
       MAX(cs.killstreak)                          AS killstreak,
       MAX(cs.highest_killstreak)                  AS highest_killstreak
FROM combat_stats AS cs
         LEFT JOIN (SELECT killer, COUNT(*) AS kills_count
                    FROM kills
                    where realm = realm_param
                    GROUP BY killer) AS k ON cs.client = k.killer
         LEFT JOIN (SELECT victim, COUNT(*) AS deaths_count
                    FROM kills
                    where realm = realm_param
                    GROUP BY victim) AS d ON cs.client = d.victim
         LEFT JOIN (SELECT contributor, COUNT(*) AS assists_count
                    FROM kill_contributions
                    GROUP BY contributor) AS ac ON cs.client = ac.contributor
         INNER JOIN clients AS c ON cs.client = c.id
WHERE c.uuid = client_uuid
  AND cs.realm = realm_param
  AND cs.valid = TRUE;
END;
$$;