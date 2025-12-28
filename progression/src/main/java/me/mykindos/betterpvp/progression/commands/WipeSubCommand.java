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
        return "";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.simpleMessage(player, "Command", "Usage: /progression wipe <player>");
            return;
        }

        String targetName = args[0];
        clientManager.search().offline(targetName).thenAcceptAsync(targetOptional -> {
            if(targetOptional.isEmpty()) {
                UtilMessage.simpleMessage(player, "Command", "Cannot find a player with the name <yellow>%s</yellow>", targetName);
                return;
            }

            Client target = targetOptional.get();
            professionProfileManager.getRepository().deleteBuildsForClient(target);

            professionProfileManager.getObject(target.getUniqueId()).ifPresent(profile -> {
                profile.getProfessionDataMap().forEach((profession, data) -> {
                    data.getBuild().getSkills().clear();
                });
            });

            UtilMessage.simpleMessage(player, "Command", "Wiped <yellow>%s</yellow>'s progression data", target.getName());
        });
    }
}
