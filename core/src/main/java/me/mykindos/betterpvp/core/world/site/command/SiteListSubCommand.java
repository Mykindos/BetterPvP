package me.mykindos.betterpvp.core.world.site.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.site.Site;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /site list}: every configured site, and under each one its live instances.
 */
@Singleton
@SubCommand(SiteCommand.class)
public class SiteListSubCommand extends Command {

    private final SiteRegistry registry;
    private final SiteInstances instances;

    @Inject
    public SiteListSubCommand(@NotNull SiteRegistry registry, @NotNull SiteInstances instances) {
        this.registry = registry;
        this.instances = instances;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List every site and its live instances";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (registry.all().isEmpty()) {
            UtilMessage.simpleMessage(player, "Sites", Component.text("No sites are configured."));
            return;
        }

        for (Site site : registry.all()) {
            UtilMessage.simpleMessage(player, "Sites", Component.text(site.getId(), NamedTextColor.WHITE)
                    .append(Component.text(" " + site.getPolicy().getLifecycle(), NamedTextColor.YELLOW))
                    .append(Component.text(" min " + site.getPolicy().getMin()
                            + " max " + site.getPolicy().getMax()
                            + " cap " + site.getPolicy().getCapacity(), NamedTextColor.GRAY)));

            for (SiteInstance instance : instances.forKey(site.key())) {
                UtilMessage.simpleMessage(player, "Sites", Component.text("  " + instance.getShortId(), NamedTextColor.AQUA)
                        .append(Component.text(" " + instance.getWorldName(), NamedTextColor.GRAY))
                        .append(Component.text(" " + instance.getState(), NamedTextColor.YELLOW))
                        .append(Component.text(" occupants: " + instance.getOccupants().size(), NamedTextColor.GRAY)));
            }
        }
    }
}
