package me.mykindos.betterpvp.core.framework.sidebar;

/**
 * How a player's clan/info readout is presented.
 *
 * <ul>
 *   <li>{@link #DISABLED} — nothing is shown.</li>
 *   <li>{@link #SIDEBAR} — the scoreboard sidebar on the right of the screen.</li>
 *   <li>{@link #HUD} — a composited component on the {@code BossBarOverlay} at the top of the screen.</li>
 * </ul>
 *
 * Stored on the client as the enum {@link #name()} via {@code ClientProperty.SIDEBAR_MODE}.
 */
public enum SidebarMode {

    DISABLED,
    SIDEBAR,
    HUD;

    /**
     * Parses a stored property value into a mode, tolerating the legacy boolean values
     * ({@code "true"}/{@code "false"}) the old {@code SIDEBAR_ENABLED} property used as well
     * as missing/unknown values.
     *
     * @param raw the stored value, or {@code null} if unset
     * @return the resolved mode, defaulting to {@link #HUD}
     */
    public static SidebarMode parse(Object raw) {
        if (raw == null) {
            return HUD;
        }
        final String value = raw.toString();
        if (value.equalsIgnoreCase("true")) {
            return HUD;
        }
        if (value.equalsIgnoreCase("false")) {
            return DISABLED;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ex) {
            return HUD;
        }
    }
}
