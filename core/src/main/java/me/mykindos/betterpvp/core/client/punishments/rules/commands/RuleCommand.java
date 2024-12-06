package me.mykindos.betterpvp.core.client.punishments.rules.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.menu.RuleMenu;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

@Singleton
public class RuleCommand extends Command {
    private final RuleManager ruleManager;

    @Inject
    public RuleCommand(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }


    @Override
    public String getName() {
        return "rules";
    }

    @Override
    public String getDescription() {
        return "Lists the rules of the server";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new RuleMenu(ruleManager, null).show(player);
    }
}
