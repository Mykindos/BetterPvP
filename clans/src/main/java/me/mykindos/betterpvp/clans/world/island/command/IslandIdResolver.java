package me.mykindos.betterpvp.clans.world.island.command;

import me.mykindos.betterpvp.clans.world.island.IslandInstance;
import me.mykindos.betterpvp.clans.world.island.IslandInstanceManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves an id prefix typed on {@code /island tp} or {@code /island delete} against
 * {@link IslandInstanceManager#matchIdPrefix(String)}, messaging the player when the prefix matches no instance or
 * more than one.
 */
final class IslandIdResolver {

    private IslandIdResolver() {
    }

    static @NotNull Optional<IslandInstance> resolve(@NotNull Player player, @NotNull IslandInstanceManager instanceManager,
                                                       @NotNull String prefix) {
        final List<IslandInstance> matches = instanceManager.matchIdPrefix(prefix);
        if (matches.isEmpty()) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("No island instance matching ", NamedTextColor.RED)
                    .append(Component.text(prefix, NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.RED)));
            return Optional.empty();
        }

        if (matches.size() > 1) {
            final String ids = matches.stream().map(instance -> instance.getId().toString()).collect(Collectors.joining(", "));
            UtilMessage.simpleMessage(player, "Islands", Component.text("Instance id prefix ", NamedTextColor.RED)
                    .append(Component.text(prefix, NamedTextColor.YELLOW))
                    .append(Component.text(" is ambiguous, matches: ", NamedTextColor.RED))
                    .append(Component.text(ids, NamedTextColor.YELLOW)));
            return Optional.empty();
        }

        return Optional.of(matches.getFirst());
    }

}
