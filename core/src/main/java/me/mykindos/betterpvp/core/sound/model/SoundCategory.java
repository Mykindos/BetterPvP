package me.mykindos.betterpvp.core.sound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Categories of sounds that players can individually toggle
 */
@Getter
@AllArgsConstructor
public enum SoundCategory {

    /**
     * Background music and musical tracks
     */
    MUSIC("Music"),

    /**
     * Ambient environmental sounds
     */
    AMBIENT("Ambient"),

    /**
     * UI and menu interaction sounds
     */
    UI("UI");

    private final String displayName;

}
