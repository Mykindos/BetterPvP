package me.mykindos.betterpvp.core.displayname;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;

/**
 * Default implementation of {@link DisplayNameProvider}.
 * <p>
 * This implementation is registered by Core as the default Guice binding
 * using an {@code OptionalBinder}. Server implementations may override this
 * binding to provide custom display name formatting.
 * <p>
 * If no overriding implementation is registered, entity names are formatted
 * using a yellow colour.
 */
public class CoreDisplayNameProvider implements DisplayNameProvider {

    /**
     * Gets the default formatted display name for an entity.
     *
     * @param entity the entity whose display name is being requested
     * @param viewer the entity viewing the display name
     * @return the entity's name formatted with the default colour
     */
    @Override
    public Component getDisplayNameAsComponent(final Entity entity, final Entity viewer) {
        return Component.text(entity.getName(), NamedTextColor.YELLOW);
    }

}