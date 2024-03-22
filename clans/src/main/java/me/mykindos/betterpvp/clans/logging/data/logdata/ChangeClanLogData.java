package me.mykindos.betterpvp.clans.logging.data.logdata;

import me.mykindos.betterpvp.clans.logging.data.ClanData;
import me.mykindos.betterpvp.clans.logging.data.ClanMemberData;
import org.jetbrains.annotations.Nullable;


public class ChangeClanLogData {
    public final ClanData clanData;
    public final ClanMemberData clanMemberData;
    public final long time;
    public final ChangeClanLogType type;

    public final ClanMemberData kicker;

    ChangeClanLogData(ClanData clanData, ClanMemberData clanMemberData, long time, ChangeClanLogType type, @Nullable ClanMemberData kicker) {
        this.clanData = clanData;
        this.clanMemberData = clanMemberData;
        this.time = time;
        this.type = type;
        this.kicker = kicker;
    }

    /**
     * Represents a log in where the member changes their Clan
     */
    ChangeClanLogData(ClanData clanData, ClanMemberData clanMemberData, long time, ChangeClanLogType type) {
       this(clanData, clanMemberData, time, type, null);
    }

    public enum ChangeClanLogType {
        JOIN,
        LEAVE,
        KICK
    }
}


