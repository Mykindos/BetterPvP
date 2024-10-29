package me.mykindos.betterpvp.core.client.gamer.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;

@Singleton
public class SetProtectionCommand extends Command {
    private final EffectManager effectManager;
    private final ClientManager clientManager;

    @Inject
    public SetProtectionCommand(EffectManager effectManager, ClientManager clientManager) {
        this.effectManager = effectManager;
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "setprotection";
    }

    @Override
    public String getDescription() {
        return "Set a players PvP protection in minutes";
    }

    public String getUsage() {
        return getName() + " <player> <duration>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "Protection", "Usage: %s", getUsage());
            return;
        }

        clientManager.search().offline(args[0], clientOptional -> {
            if (clientOptional.isEmpty()) {
                UtilMessage.message(player, "Protection", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid Player.", args[0]));
                return;
            }

            double duration;
            try {
                duration = Double.parseDouble(args[1]);
                if (duration < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ignored) {
                UtilMessage.message(player, "Protection", UtilMessage.deserialize("<green>%s</green> is not a valid duration.", args[1]));
                return;
            }

            long newDuration = (long) (duration * 60 * 1000L);
            Client target = clientOptional.get();
            target.getGamer().saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, newDuration);
            if (target.isOnline()) {
                effectManager.removeEffect(player, EffectTypes.PROTECTION);
                effectManager.addEffect(player, EffectTypes.PROTECTION, newDuration);
            }
            String timeString = UtilTime.getTime(newDuration, 1);
            UtilMessage.message(player, "Protection", "Your protection timer was set for <green>%s</green>", timeString);
            clientManager.sendMessageToRank("Protection",
                    UtilMessage.deserialize("<yellow>%s</yellow> set <yellow>%s</yellow>'s protection timer for <green>%s</green>",
                            player.getName(), target.getName(), timeString),
                    Rank.HELPER);
        });

    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
