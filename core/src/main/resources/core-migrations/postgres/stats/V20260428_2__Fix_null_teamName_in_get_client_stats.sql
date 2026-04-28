CREATE OR REPLACE FUNCTION get_client_stats(
    client_param BIGINT
)
    RETURNS TABLE(
        Realm Integer,
        StatType VARCHAR(127),
        Data JSONB,
        Stat BIGINT
            ) LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        cs.Realm,
        cs.StatType,
        cs.StatData
            || CASE
                WHEN gd.id IS NOT NULL THEN
                    jsonb_build_object(
                        'gameName', gd.Game,
                        'mapName',  gd.Map,
                        'teamName', COALESCE(gt.Team, 'UNKNOWN')
                    )
                ELSE '{}'::jsonb
            END AS StatData,
        cs.Stat
    FROM client_stats cs
    LEFT JOIN game_data gd
        ON gd.id = (cs.StatData ->> 'gameId')::bigint
    LEFT JOIN game_teams gt
        ON gt.id = gd.id
        AND gt.client = cs.client
    WHERE cs.Client = client_param;
END;
$$;

