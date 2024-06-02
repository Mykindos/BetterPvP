package me.mykindos.betterpvp.shops.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.commands.loader.ShopsCommandLoader;
import me.mykindos.betterpvp.shops.listener.ShopsListenerLoader;
import me.mykindos.betterpvp.shops.shops.ShopManager;
import me.mykindos.betterpvp.shops.shops.shopkeepers.ShopkeeperManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

@Singleton
public class ShopsCommand extends Command implements IConsoleCommand {

    public ShopsCommand() {
        aliases.add("shop");
    }

    @Override
    public String getName() {
        return "shops";
    }

    @Override
    public String getDescription() {
        return "Shops base command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {}

    @Override
    public void execute(CommandSender sender, String[] args) {}

    @Override
    public boolean informInsufficientRank() {
        return true;
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }



    @Singleton
    @SubCommand(ShopsCommand.class)
    private static class ShopSpawnCommand extends Command {

        @Inject
        private  ShopkeeperManager shopkeeperManager;

        @Override
        public String getName() {
            return "spawn";
        }

        @Override
        public String getDescription() {
            return "Spawn a shopkeeper";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if(args.length <= 1) {
                UtilMessage.message(player, "Shops", "Usage: /shop spawn <entityType> <shop name>");
                return;
            }
            
            shopkeeperManager.saveShopkeeper(args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)), player.getLocation());
            shopkeeperManager.loadShopsFromConfig();
        }
    }

    @Singleton
    @SubCommand(ShopsCommand.class)
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Shops shops;

        @Inject
        private ShopsCommandLoader commandLoader;

        @Inject
        private ShopsListenerLoader listenerLoader;

        @Inject
        private ShopkeeperManager shopkeeperManager;

        @Inject
        private ShopManager shopManager;


        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the shops plugin";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            shops.reload();

            shopManager.loadShopItems();
            commandLoader.reload(shops.getClass().getPackageName());
            shopkeeperManager.loadShopsFromConfig();

            UtilMessage.message(sender, "Clans", "Successfully reloaded shops");
        }
    }

}
