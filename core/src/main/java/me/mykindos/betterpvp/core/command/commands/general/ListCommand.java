package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ListCommand extends Command {

    private final EffectManager effectManager;

    @Inject
    public ListCommand(EffectManager effectManager) {
        this.effectManager = effectManager;
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
        List<Player> playerList = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (!effectManager.hasEffect(player, EffectTypes.VANISH, "commandVanish")) {
            playerList.removeIf(target -> target != player && effectManager.hasEffect(target, EffectTypes.VANISH, "commandVanish"));
        }
        
        String players = playerList.stream().map(target -> String.format("<yellow>%s", target.getName())).collect(Collectors.joining("<gray>, ");
        players = String.format("<gray>[%s<gray>]", players);

        UtilMessage.message(player, "List", "There are currently <alt2>" + playerList.size() + "</alt2> players online.");
        UtilMessage.message(player, "List", players);

    }
}
