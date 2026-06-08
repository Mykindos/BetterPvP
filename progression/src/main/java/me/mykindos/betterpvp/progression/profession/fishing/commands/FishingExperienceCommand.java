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
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        return "progression.command.fishing-experience.description";
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
        private final ProfessionProfileManager professionProfileManager;

        @Inject
        private SetFishingExperienceCommand(ClientManager clientManager, FishingHandler fishingHandler, ProfessionProfileManager professionProfileManager) {
            this.clientManager = clientManager;
            this.fishingHandler = fishingHandler;
            this.professionProfileManager = professionProfileManager;
        }

        @Override
        public String getName() {
            return "set";
        }

        @Override
        public String getDescription() {
        return "progression.command.set-fishing-experience.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                UtilMessage.message(sender, "core.prefix.fishing", "progression.command.fishingexp.usage");
                return;
            }

            clientManager.search().offline(args[0]).thenAcceptAsync(targetOptional -> {
                if (targetOptional.isEmpty()) {
                    UtilMessage.message(sender, "core.prefix.fishing", "progression.command.exp.player-not-found",
                            Component.text(args[0], NamedTextColor.YELLOW));
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
                    UtilMessage.message(sender, "core.prefix.fishing", "progression.command.exp.invalid-amount",
                            Component.text(args[1], NamedTextColor.GREEN));
                    return;
                }
                ProfessionData professionData = fishingHandler.getProfessionData(target.getUniqueId());
                double oldExperience = professionData.getExperience();
                professionData.setExperience(newExperience);

                professionProfileManager.getRepository().saveExperience(target.getUniqueId(), professionData.getProfession(), professionData.getExperience());

                UtilMessage.message(sender, "core.prefix.fishing", "progression.command.fishingexp.set",
                        Component.text(target.getName(), NamedTextColor.YELLOW),
                        Component.text(newExperience, NamedTextColor.GREEN),
                        Component.text(oldExperience, NamedTextColor.WHITE));
            });
        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }
}
