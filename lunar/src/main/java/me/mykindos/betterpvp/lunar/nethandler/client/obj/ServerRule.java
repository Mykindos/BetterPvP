package me.mykindos.betterpvp.lunar.nethandler.client.obj;

import lombok.Getter;

public enum ServerRule {

    /**
     * Whether or not minimap is allowed
     * Expected value: (String) NEUTRAL or FORCED_OFF
     */
    MINIMAP_STATUS("minimapStatus", String.class),

    /**
     * Whether or not the server will store waypoints, instead of the client
     */
    SERVER_HANDLES_WAYPOINTS("serverHandlesWaypoints", Boolean.class),

    /**
     * A warning message will be shown when attempting to disconnect if the current
     * game is competitive.
     */
    COMPETITIVE_GAME("competitiveGame", Boolean.class),

    /**
     * If this server forces shaders to be disabled
     */
    SHADERS_DISABLED("shadersDisabled", Boolean.class),

    /**
     * If the server runs legacy enchanting (pre 1.8)
     */
    LEGACY_ENCHANTING("legacyEnchanting", Boolean.class),

    /**
     * If this server has enabled voice chat
     */
    VOICE_ENABLED("voiceEnabled", Boolean.class),

    /**
     * Whether to revert combat mechanics to 1.7
     */
    LEGACY_COMBAT("legacyCombat", Boolean.class);

    @Getter private String id;
    @Getter private Class type;

    ServerRule(String id, Class type) {
        this.id = id;
        this.type = type;
    }

    public static ServerRule getRule(String id) {
        for (ServerRule existing : ServerRule.values()) {
            if (existing.id.equals(id)) {
                return existing;
            }
        }

        return null;
    }

}
