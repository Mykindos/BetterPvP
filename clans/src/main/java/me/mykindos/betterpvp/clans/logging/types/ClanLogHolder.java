package me.mykindos.betterpvp.clans.logging.types;

import lombok.Data;
import me.mykindos.betterpvp.clans.logging.types.formatted.FormattedClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.KillClanLog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ClanLogHolder {
    private final UUID clanID;
    private final List<FormattedClanLog> clanLogs = new ArrayList<FormattedClanLog>();
    private boolean clanLogsUpdated = false;
    private final List<KillClanLog> clanKillLogs = new ArrayList<KillClanLog>();
    private boolean killLogsUpdated = false;
}
