package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.command.loader.CoreCommandLoader;
import me.mykindos.betterpvp.core.content.manifest.ManifestPublisher;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;
import me.mykindos.betterpvp.core.resourcepack.ResourcePackHandler;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        return "core.command.core.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
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
        private CoreCommandLoader commandLoader;

        @Inject
        private CoreListenerLoader listenerLoader;

        @Inject
        private TipManager tipManager;

        @Inject
        private ResourcePackHandler resourcePackHandler;

        @Inject
        private SchematicService schematicService;

        @Inject
        private RuleManager ruleManager;

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
        return "core.command.reload.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            core.reload();
            core.getReloadables().forEach(Reloadable::reload);

            commandLoader.reload(core.getClass().getPackageName());
            tipManager.reloadTips(core);
            resourcePackHandler.reload();
            ruleManager.reload(core);
            schematicService.clearCache();

            UtilMessage.message(sender, "core.prefix.core", "core.command.core.reload.success");
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }


    }

    @Singleton
    @SubCommand(CoreCommand.class)
    private static class PublishManifestCommand extends Command implements IConsoleCommand {

        @Inject
        private ManifestPublisher manifestPublisher;

        @Inject
        private ClientManager clientManager;

        @Override
        public String getName() {
            return "publishmanifest";
        }

        @Override
        public String getDescription() {
            return "Re-publish the game content manifest to the database";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            clientManager.sendMessageToRank(
                    "Core",
                    Component.empty()
                            .append(Component.text(sender.getName(), NamedTextColor.YELLOW))
                            .append(Component.text(" is publishing the game manifest... This can take up to a few minutes.")),
                    Rank.ADMIN
            );
            manifestPublisher.publish();
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }
}
