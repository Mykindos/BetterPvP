package me.mykindos.betterpvp.core.tips.commands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.tips.TipEvent;
import net.kyori.adventure.text.Component;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

public class TipCommand extends Command {

    private final ClientManager clientManager;

    private final Core core;

    @Inject
    public TipCommand(Core core, ClientManager clientManager) {
        this.clientManager = clientManager;
        this.core = core;
        this.aliases.add("tips");
    }

    @Override
    public String getName() {
        return "tip";
    }

    @Override
    public String getDescription() {
        return "core.command.tip.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        int amount = 1;
        if (args.length > 0) {
            try {
                amount = Integer.parseInt(args[0]);
                if (amount <= 0) {
                    throw new NumberFormatException("Number must be greater than 0");
                } else if(amount > 20){
                    UtilMessage.message(player, "core.prefix.command", "core.command.tip.limit");
                    return;
                }
            } catch (NumberFormatException e) {
                UtilMessage.message(player, "core.prefix.command", "core.command.tip.invalid_int", Component.text(args[0]));
            }
        }
        for (int i = 0; i < Math.min(amount, 20); i++) {
            UtilServer.runTaskAsync(core, () -> UtilServer.callEvent(new TipEvent(player, gamer)));
        }
    }
}
