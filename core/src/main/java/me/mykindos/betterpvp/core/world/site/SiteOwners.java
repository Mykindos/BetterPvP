package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who owns which instance of an owned site, answered by whichever module owns that site.
 * <p>
 * A site with nothing registered has no instance anybody can be sent to, which is the right answer rather than an
 * error: the module that would have known is not loaded.
 */
@Singleton
public class SiteOwners {

    private final Map<String, SiteOwnership> ownership = new ConcurrentHashMap<>();

    public void register(@NotNull String siteId, @NotNull SiteOwnership owner) {
        ownership.put(siteId.toLowerCase(Locale.ROOT), owner);
    }

    /** The key naming a player's own instance of a site, or empty when they have none to go to. */
    public @NotNull Optional<SiteKey> keyFor(@NotNull Site site, @NotNull Player player) {
        if (site.getPolicy().getLifecycle() != SitePolicy.Lifecycle.OWNED) {
            return Optional.of(site.key());
        }

        final SiteOwnership owner = ownership.get(site.getId().toLowerCase(Locale.ROOT));
        if (owner == null) {
            return Optional.empty();
        }

        final OptionalLong resolved = owner.ownerOf(player);
        return resolved.isPresent()
                ? Optional.of(SiteKey.of(site.getId(), resolved.getAsLong()))
                : Optional.empty();
    }

    /**
     * What an owner has chosen their world be built from, or empty when they have chosen nothing. Empty leaves a
     * world already on disk alone rather than standing in for the site's own default.
     */
    public @NotNull Optional<String> templateFor(@NotNull Site site, @NotNull SiteKey key) {
        final SiteOwnership owner = ownership.get(site.getId().toLowerCase(Locale.ROOT));
        return owner == null ? Optional.empty() : owner.templateFor(key);
    }
}
