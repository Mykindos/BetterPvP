package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.commands.menu.KitMenu;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.bukkit.entity.Player;

@Singleton
public class KitCommand extends Command {

    @Inject
    private CooldownManager cooldownManager;

    @Inject
    private RoleManager roleManager;

    @Inject
    private ItemFactory itemFactory;

    @Inject
    @Config(path = "command.kit.kitCooldown", defaultValue = "true")
    private boolean kitCooldown;

    @Inject
    @Config(path = "command.kit.kitCooldownMinutes", defaultValue = "60.0")
    private double kitCooldownMinutes;

    @Override
    public String getName() {
        return "kit";
    }

    @Override
    public String getDescription() {
        return "Equip a kit";
    }



    @Override
    public void execute(Player player, Client client, String... args) {
        final KitMenu kitMenu = new KitMenu(roleManager, itemFactory, true);
        if (client.hasRank(Rank.ADMIN)) {
            kitMenu.show(player);
            return;
        }

        if (cooldownManager.use(player, "Kit", kitCooldownMinutes * 60.0, true, false, false, false)){
            kitMenu.show(player);
        }
    }
}
