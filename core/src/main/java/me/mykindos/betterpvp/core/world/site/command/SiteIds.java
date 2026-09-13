package me.mykindos.betterpvp.core.world.site.command;

import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class SiteIds {

    private SiteIds() {
    }

    /**
     * Resolves a typed id prefix against the live instances, telling the player when it matches none or several.
     */
    static @NotNull Optional<SiteInstance> resolve(@NotNull Player player, @NotNull SiteInstances instances,
                                                   @NotNull String prefix) {
        final List<SiteInstance> matches = instances.matchIdPrefix(prefix);
        if (matches.isEmpty()) {
            UtilMessage.simpleMessage(player, "Sites", Component.text("No instance matching ", NamedTextColor.RED)
                    .append(Component.text(prefix, NamedTextColor.YELLOW)));
            return Optional.empty();
        }

        if (matches.size() > 1) {
            final String ids = matches.stream().map(SiteInstance::getShortId).collect(Collectors.joining(", "));
            UtilMessage.simpleMessage(player, "Sites", Component.text("Ambiguous, matches: ", NamedTextColor.RED)
                    .append(Component.text(ids, NamedTextColor.YELLOW)));
            return Optional.empty();
        }

        return Optional.of(matches.getFirst());
    }
}
