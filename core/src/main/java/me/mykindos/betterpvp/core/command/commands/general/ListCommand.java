package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ListCommand extends Command {
    private final ClientManager clientManager;

    @Inject
    public ListCommand(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "get a list of all players online";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        int size = clientManager.getOnline().size();

        List<Component> players = new ArrayList<>();
        for (Client onlineClient : clientManager.getOnline()) {
            players.add(Component.text(onlineClient.getName(), onlineClient.getRank().getColor()));
        }

        Component list = Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)), players.toArray(new Component[0]));
        UtilMessage.message(player, "List", "There are currently <alt2>" + size + "</alt2> players online.");
        UtilMessage.message(player, "List", list);

    }
}
