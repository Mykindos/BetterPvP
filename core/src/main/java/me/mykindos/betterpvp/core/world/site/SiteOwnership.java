package me.mykindos.betterpvp.core.world.site;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * What the module owning a site knows about it that core cannot work out.
 * <p>
 * An owned site has one instance per owner and {@link SiteKey} carries that owner as a plain number, so core never
 * learns what an owner is. This is the whole of what it asks in return.
 */
public interface SiteOwnership {

    /** Which owner a player belongs to, or empty when they belong to none and so have nowhere of their own to go. */
    @NotNull OptionalLong ownerOf(@NotNull Player player);

    /**
     * The template an owner's world is built from, when the owner gets to choose.
     * <p>
     * Empty leaves the site's configured template standing. Answering differently from what a world was last built
     * from is what makes it get rebuilt.
     */
    default @NotNull Optional<String> templateFor(@NotNull SiteKey key) {
        return Optional.empty();
    }
}
