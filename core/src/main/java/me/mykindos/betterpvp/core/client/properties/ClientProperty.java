package me.mykindos.betterpvp.core.client.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ClientProperty {

    SIDEBAR_ENABLED,
    TIPS_ENABLED,
    DROP_PROTECTION_ENABLED,
    COOLDOWN_DISPLAY,
    CHAT_ENABLED,
    LUNAR,
    LAST_MESSAGED,
    TIME_PLAYED,
    MAP_POINTS_OF_INTEREST,
    MAP_PLAYER_NAMES,
    COOLDOWN_SOUNDS_ENABLED,
    TERRITORY_POPUPS_ENABLED,
    DUNGEON_INCLUDE_ALLIES,
    MEDIA_CHANNEL,
    SHOW_TAG,
    /**
     * The last time in unix time this client was connected
     * Updates on client quit
     */
    LAST_LOGIN

}
