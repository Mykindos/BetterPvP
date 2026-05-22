package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCoreCommandLoader;
import me.mykindos.betterpvp.core.command.loader.CoreCommandLoader;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;
import me.mykindos.betterpvp.core.resourcepack.ResourcePackHandler;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Recipe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

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
        final File serverFolder = Bukkit.getServer().getPluginsFolder().getParentFile();
        final File recipes = new File(serverFolder, "recipes.txt");

        try {
            recipes.createNewFile();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(recipes, false))) {
                final Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();

                while (recipeIterator.hasNext()) {
                    final Recipe recipe = recipeIterator.next();

                    if (recipe instanceof CraftingRecipe craftingRecipe) {
                        final NamespacedKey key = craftingRecipe.getKey();

                        writer.write(key.toString());
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Singleton
    @SubCommand(CoreCommand.class)
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Core core;

        @Inject
        private BrigadierCoreCommandLoader brigadierCoreCommandLoader;

        @Inject
        private CoreCommandLoader commandLoader;

        @Inject
        private CoreListenerLoader listenerLoader;

        @Inject
        private TipManager tipManager;

        @Inject
        private ResourcePackHandler resourcePackHandler;

        @Inject
        private RuleManager ruleManager;

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
            core.getReloadables().forEach(Reloadable::reload);

            brigadierCoreCommandLoader.reload();
            commandLoader.reload(core.getClass().getPackageName());
            tipManager.reloadTips(core);
            resourcePackHandler.reload();
            ruleManager.reload(core);

            UtilMessage.message(sender, "Core", "Successfully reloaded core");
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }


    }
}
