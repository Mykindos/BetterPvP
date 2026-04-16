package me.mykindos.betterpvp.core.framework;

import net.kyori.adventure.text.Component;

/**
 * A {@link ServerType} that can appear as a selectable entry in the hub server selector.
 * Carries the server-name prefix used to match running instances and a display title for menus.
 */
public interface SelectableServerType extends ServerType {
    /** Prefix that running server names must start with to belong to this type (e.g. {@code "clans"}, {@code "champions"}). */
    String getServerNamePrefix();

    /** Component shown as the menu/GUI title for this server type. */
    Component getDisplayTitle();
}
