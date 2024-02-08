package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.combat.weapon.WeaponManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.command.loader.CoreCommandLoader;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;
import me.mykindos.betterpvp.core.resourcepack.ResourcePackHandler;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class CoreCommand extends Command implements IConsoleCommand {

    @Override
    public String getName() {
        return "core";
    }

    @Override
    public String getDescription() {
        return "Base core command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

    }

    @Override
    public void execute(CommandSender sender, String[] args) {

    }

    @Override
    public Rank getRequiredRank() {
        return Rank.OWNER;
    }

    @Singleton
    @SubCommand(CoreCommand.class)
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Core core;

        @Inject
        private CoreCommandLoader commandLoader;

        @Inject
        private CoreListenerLoader listenerLoader;

        @Inject
        private TipManager tipManager;

        @Inject
        private ResourcePackHandler resourcePackHandler;

        @Inject
        private WeaponManager weaponManager;

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the core plugin";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            core.reload();
            commandLoader.reload(core.getClass().getPackageName());
            tipManager.reloadTips();
            resourcePackHandler.reload();
            weaponManager.reload(core);

            UtilMessage.message(sender, "Core", "Successfully reloaded core");
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.OWNER;
        }


    }
}
