package me.mykindos.betterpvp.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.champions.listeners.ChampionsListenerLoader;
import me.mykindos.betterpvp.champions.weapons.WeaponManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class ChampionsCommand extends Command implements IConsoleCommand {


    @Override
    public String getName() {
        return "champions";
    }

    @Override
    public String getDescription() {
        return "Base champions command";
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
    @SubCommand(ChampionsCommand.class)
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Champions champions;

        @Inject
        private ChampionsCommandLoader commandLoader;

        @Inject
        private ChampionsListenerLoader listenerLoader;

        @Inject
        private SkillManager skillManager;

        @Inject
        private ItemHandler itemHandler;

        @Inject
        private WeaponManager weaponManager;

        @Inject
        private BuildManager buildManager;

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the champions plugin";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            champions.reloadConfig();

            commandLoader.reload(champions.getClass().getPackageName());
            listenerLoader.reload(champions.getClass().getPackageName());
            skillManager.reloadSkills();
            weaponManager.reload();
            buildManager.reloadBuilds();

            itemHandler.loadItemData("champions");

            UtilMessage.message(sender, "Champions", "Successfully reloaded champions");
        }
    }
}
