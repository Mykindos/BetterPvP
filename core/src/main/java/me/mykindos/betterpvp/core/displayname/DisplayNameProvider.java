package me.mykindos.betterpvp.core.displayname;

import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;

/**
 * Provides the formatted display name for an entity.
 * <p>
 * Core registers a default implementation through Guice which formats entity
 * names using a yellow colour. Server implementations may override this
 * binding to provide custom formatting without requiring shared modules to
 * know which server type is currently running.
 * <p>
 * The returned display name represents the authoritative formatted name for
 * an entity and should be used wherever a formatted entity name is required.
 */
public interface DisplayNameProvider {

    /**
     * Gets the formatted display name for an entity from the perspective of a
     * specific viewer.
     * <p>
     * The {@code viewer} allows implementations to return different display
     * names depending on who is viewing the entity, such as ally, enemy or
     * team-specific formatting.
     *
     * @param entity the entity whose display name is being requested
     * @param viewer the entity viewing the display name
     * @return the formatted display name as an Adventure {@link Component}
     */
    Component getDisplayNameAsComponent(Entity entity, Entity viewer);

    /**
     * Gets the formatted display name as a MiniMessage serialized string.
     * <p>
     * This is a convenience method for APIs and systems that require
     * MiniMessage strings instead of Adventure components.
     *
     * @param entity the entity whose display name is being requested
     * @param viewer the entity viewing the display name
     * @return the formatted display name serialized as MiniMessage
     */
    default String getDisplayNameAsString(final Entity entity, final Entity viewer) {
        return UtilMessage.serialize(this.getDisplayNameAsComponent(entity, viewer));
    }
}