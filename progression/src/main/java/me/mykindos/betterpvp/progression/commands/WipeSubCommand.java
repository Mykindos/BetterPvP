package me.mykindos.betterpvp.progression.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import me.mykindos.betterpvp.progression.profile.repository.ProfessionProfileRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ProgressionCommand.class)
public class WipeSubCommand extends Command {

    private final ClientManager clientManager;
    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public WipeSubCommand(ClientManager clientManager, ProfessionProfileManager professionProfileManager, ProfessionProfileRepository professionProfileRepository) {
        this.clientManager = clientManager;
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "wipe";
    }

    @Override
    public String getDescription() {
        return "progression.command.wipe.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.message(player, "core.prefix.command", "progression.command.wipe.usage");
            return;
        }

        String targetName = args[0];
        clientManager.search().offline(targetName).thenAcceptAsync(targetOptional -> {
            if(targetOptional.isEmpty()) {
                UtilMessage.message(player, "core.prefix.command", "progression.command.wipe.player-not-found",
                        Component.text(targetName, NamedTextColor.YELLOW));
                return;
            }

            Client target = targetOptional.get();
            professionProfileManager.getRepository().deleteBuildsForClient(target);

            professionProfileManager.getObject(target.getUniqueId()).ifPresent(profile -> {
                profile.getProfessionDataMap().forEach((profession, data) -> {
                    data.getBuild().getNodes().clear();
                });
            });

            UtilMessage.message(player, "core.prefix.command", "progression.command.wipe.success",
                    Component.text(target.getName(), NamedTextColor.YELLOW));
        });
    }
}
