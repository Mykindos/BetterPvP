package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.site.Presence;
import me.mykindos.betterpvp.core.world.site.Site;
import me.mykindos.betterpvp.core.world.site.SiteRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists everybody online, grouped by the site they are in.
 * <p>
 * Deliberately network-wide rather than presence-scoped: a player cannot see or speak to somebody on another site,
 * but knowing how busy the server is has never been something to hide.
 */
@Singleton
public class ListCommand extends Command {

    private final ClientManager clientManager;
    private final SiteRegistry registry;
    private final Presence presence;

    @Inject
    public ListCommand(ClientManager clientManager, SiteRegistry registry, Presence presence) {
        this.clientManager = clientManager;
        this.registry = registry;
        this.presence = presence;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "core.command.list.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "core.prefix.list", "core.command.list.online_count",
                Component.text(clientManager.getOnline().size()));

        for (Site site : registry.all()) {
            final List<Player> present = presence.onSite(site);
            if (!present.isEmpty()) {
                UtilMessage.message(player, "core.prefix.list", "core.command.list.group",
                        site.getDisplayName(), names(present));
            }
        }

        final List<Player> elsewhere = presence.offSite();
        if (!elsewhere.isEmpty()) {
            UtilMessage.message(player, "core.prefix.list", "core.command.list.group",
                    Component.translatable("core.command.list.elsewhere"), names(elsewhere));
        }
    }

    private Component names(List<Player> players) {
        final List<Component> names = new ArrayList<>();
        for (Player online : players) {
            final Client onlineClient = clientManager.search().online(online);
            names.add(Component.text(online.getName(), onlineClient.getRank().getColor()));
        }
        return Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)), names);
    }
}
