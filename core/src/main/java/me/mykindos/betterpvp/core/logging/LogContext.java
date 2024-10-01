package me.mykindos.betterpvp.core.logging;

import java.util.Objects;

public class LogContext {

    private LogContext() {}

    public static final String CLIENT = "Client";
    public static final String CLIENT_NAME = "ClientName";
    public static final String TARGET_CLIENT = "TargetClient";
    public static final String TARGET_CLIENT_NAME = "TargetClientName";

    public static final String LOCATION = "Location";
    public static final String CURRENT_LOCATION = "CurrentLocation";
    public static final String NEW_LOCATION = "NewLocation";
    public static final String CHUNK = "Chunk";

    public static final String BLOCK = "Block";

    // Clans
    public static final String CLAN = "Clan";
    public static final String CLAN_NAME = "ClanName";
    public static final String TARGET_CLAN = "TargetClan";
    public static final String TARGET_CLAN_NAME = "TargetClanName";
    public static final String CURRENT_CLAN_RANK = "CurrentClanRank";
    public static final String NEW_CLAN_RANK = "NewClanRank";

    // Items
    public static final String ITEM = "Item";
    public static final String ITEM_NAME = "ItemName";

    // Dungeons
    public static final String SOURCE = "Source";

    /**
     * Gets the alternative context (i.e. CLAN -> TARGET_CLAN)
     * @param context the context to get the alternative context for
     * @return the alternative context
     */
    public static String getAltContext(String context) {
        if (Objects.equals(context, LogContext.CLAN_NAME)) {
            return LogContext.TARGET_CLAN_NAME;
        }

        if (Objects.equals(context, LogContext.TARGET_CLAN_NAME)) {
            return LogContext.CLAN_NAME;
        }

        if (Objects.equals(context, LogContext.CLAN)) {
            return LogContext.TARGET_CLAN;
        }

        if (Objects.equals(context, LogContext.TARGET_CLAN)) {
            return LogContext.CLAN;
        }

        if (Objects.equals(context, LogContext.CLIENT_NAME)) {
            return LogContext.TARGET_CLIENT_NAME;
        }

        if (Objects.equals(context, LogContext.TARGET_CLIENT_NAME)) {
            return LogContext.CLIENT_NAME;
        }

        if (Objects.equals(context, LogContext.CLIENT)) {
            return LogContext.TARGET_CLIENT;
        }

        if (Objects.equals(context, LogContext.TARGET_CLIENT)) {
            return LogContext.CLIENT;
        }

        return null;
    }
}
