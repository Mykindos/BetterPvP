package me.mykindos.betterpvp.progression.profession.mining.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class MiningExperienceCommand extends Command implements IConsoleCommand {

    public MiningExperienceCommand() {
    }

    @Override
    public String getName() {
        return "miningexp";
    }

    @Override
    public String getDescription() {
        return "Base mining experience command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        execute(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

    }

    @Singleton
    @SubCommand(MiningExperienceCommand.class)
    private static class SetMiningExperienceCommand extends Command implements IConsoleCommand {
        private final ClientManager clientManager;
        private final MiningHandler miningHandler;

        @Inject
        private SetMiningExperienceCommand(ClientManager clientManager, MiningHandler miningHandler) {
            this.clientManager = clientManager;
            this.miningHandler = miningHandler;
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
                UtilMessage.message(sender, "Mining", "Usage: /miningexp set <player> <experience>");
                return;
            }

            clientManager.search().offline(args[0], targetOptional -> {
                if (targetOptional.isEmpty()) {
                    UtilMessage.message(sender, "Mining", "Cannot find a player with the name <yellow>%s</yellow>", args[0]);
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
                    UtilMessage.message(sender, "Mining", "<green>%s</green> is an invalid amount of experience", args[1]);
                    return;
                }
                ProfessionData professionData = miningHandler.getProfessionData(target.getUniqueId());
                double oldExperience = professionData.getExperience();
                professionData.setExperience(newExperience);
                UtilMessage.message(sender, "Mining", "Set <yellow>%s</yellow>'s mining experience to <green>%s</green> (was <white>%s</white>)",
                        target.getName(), newExperience, oldExperience);
            }, true);
        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }
}
