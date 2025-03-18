package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Singleton
@CustomLog
public class BrigadierSkillsCommand extends BrigadierCommand {

    private final RoleManager roleManager;
    private final BuildManager buildManager;

    private Rank seeOtherPlayerSkillsRank;

    @Inject
    public BrigadierSkillsCommand(ClientManager clientManager, RoleManager roleManager, BuildManager buildManager) {
        super(clientManager);
        this.roleManager = roleManager;
        this.buildManager = buildManager;
        getAliases().addAll(List.of("skill", "class"));
    }

    @Override
    public String getName() {
        return "skills";
    }

    @Override
    public String getDescription() {
        return "View your current skills";
    }

    @Override
    public void setConfig(ExtendedYamlConfiguration config) {
        super.setConfig(config);
        String path = getPath() + ".seeOtherPlayerSkillsRank";
        this.seeOtherPlayerSkillsRank = Rank.valueOf(config.getOrSaveString(path, "ADMIN").toUpperCase());

    }

    /**
     * Define the command, using normal rank based permissions
     *
     * @return the builder for the command
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName())
                .executes(context -> {
                    final Player player = getPlayerFromExecutor(context);
                    final Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId().toString());
                    if (gamerBuildsOptional.isPresent()) {
                        final GamerBuilds builds = gamerBuildsOptional.get();

                        final Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId());
                        if (roleOptional.isEmpty()) {
                            UtilMessage.message(player, "Skills", "You do not have a set equipped!");
                            return Command.SINGLE_SUCCESS;
                        }

                        final Role role = roleOptional.get();
                        final RoleBuild build = builds.getActiveBuilds().get(role.getName());
                        if (build != null) {
                            UtilMessage.message(player, "Skills", UtilMessage.deserialize("Your Build:").appendNewline().append(build.getBuildComponent()));
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(IBrigadierCommand.argument("player", ArgumentTypes.player(), source -> this.senderHasRank(source, this.seeOtherPlayerSkillsRank))
                        .executes(context -> {
                            final Player target = context.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
                            final CommandSender sender = context.getSource().getSender();
                            final Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(target.getUniqueId().toString());
                            if (gamerBuildsOptional.isEmpty()) {
                                UtilMessage.message(sender, "Skills", UtilMessage.deserialize("<yellow>%s</yellow> does not have any builds", target.getName()));
                                return Command.SINGLE_SUCCESS;
                            }
                            final Optional<Role> roleOptional = roleManager.getObject(target.getUniqueId());
                            if (roleOptional.isEmpty()) {
                                UtilMessage.message(sender, "Skills", UtilMessage.deserialize("<yellow>%s</yellow> does not have a set equipped", target.getName()));;
                                return Command.SINGLE_SUCCESS;
                            }
                            final GamerBuilds builds = gamerBuildsOptional.get();
                            final Role role = roleOptional.get();
                            final RoleBuild build = builds.getActiveBuilds().get(role.getName());
                            if (build != null) {
                                UtilMessage.message(sender, "Skills", UtilMessage.deserialize("<yellow>%s</yellow>'s Build:", target.getName()).appendNewline().append(build.getBuildComponent()));
                                return Command.SINGLE_SUCCESS;
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }
}
