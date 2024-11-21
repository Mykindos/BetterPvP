package me.mykindos.betterpvp.progression.profession.fishing.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class FishingExperienceCommand extends Command implements IConsoleCommand {

    public FishingExperienceCommand() {
    }

    @Override
    public String getName() {
        return "fishingexp";
    }

    @Override
    public String getDescription() {
        return "Base fishing experience command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        execute(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

    }

    @Singleton
    @SubCommand(FishingExperienceCommand.class)
    private static class SetFishingExperienceCommand extends Command implements IConsoleCommand {
        private final ClientManager clientManager;
        private final FishingHandler fishingHandler;

        @Inject
        private SetFishingExperienceCommand(ClientManager clientManager, FishingHandler fishingHandler) {
            this.clientManager = clientManager;
            this.fishingHandler = fishingHandler;
        }

        @Override
        public String getName() {
            return "set";
        }

        @Override
        public String getDescription() {
            return "set a players woodcutting experience";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                UtilMessage.message(sender, "Fishing", "Usage: /fishingexp set <player> <experience>");
                return;
            }

            clientManager.search().offline(args[0], targetOptional -> {
                if (targetOptional.isEmpty()) {
                    UtilMessage.message(sender, "Fishing", "Cannot find a player with the name <yellow>%s</yellow>", args[0]);
                    return;
                }
                Client target = targetOptional.get();

                double newExperience;
                try {
                    newExperience = Double.parseDouble(args[1]);
                    if (newExperience < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ignored) {
                    UtilMessage.message(sender, "Fishing", "<green>%s</green> is an invalid amount of experience", args[1]);
                    return;
                }
                ProfessionData professionData = fishingHandler.getProfessionData(target.getUniqueId());
                double oldExperience = professionData.getExperience();
                professionData.setExperience(newExperience);
                UtilMessage.message(sender, "Fishing", "Set <yellow>%s</yellow>'s fishing experience to <green>%s</green> (was <white>%s</white>)",
                        target.getName(), newExperience, oldExperience);
            }, true);
        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }
}
