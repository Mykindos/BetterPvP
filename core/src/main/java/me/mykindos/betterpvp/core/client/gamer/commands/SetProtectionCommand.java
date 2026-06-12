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
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
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
        return "core.command.set-protection.description";
    }

    public String getUsage() {
        return getName() + " <player> <duration>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "core.prefix.protection", "core.command.setprotection.usage",
                    net.kyori.adventure.text.Component.text(getUsage()));
            return;
        }

        clientManager.search().offline(args[0]).thenAccept(clientOptional -> {
            if (clientOptional.isEmpty()) {
                UtilMessage.message(player, "core.prefix.protection", "core.command.protection.invalid_player",
                        net.kyori.adventure.text.Component.text(args[0], net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                return;
            }

            double duration;
            try {
                duration = Double.parseDouble(args[1]);
                if (duration < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ignored) {
                UtilMessage.message(player, "core.prefix.protection", "core.command.setprotection.invalid_duration",
                        net.kyori.adventure.text.Component.text(args[1], net.kyori.adventure.text.format.NamedTextColor.GREEN));
                return;
            }

            long newDuration = (long) (duration * 60 * 1000L);
            String timeString = UtilTime.getTime(newDuration, 1);
            Client target = clientOptional.get();

            target.getGamer().saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, newDuration);
            Player targetPlayer = Bukkit.getPlayer(target.getUniqueId());
            if (targetPlayer != null) {
                effectManager.removeEffect(targetPlayer, EffectTypes.PROTECTION);
                effectManager.addEffect(targetPlayer, EffectTypes.PROTECTION, newDuration);
                UtilMessage.message(targetPlayer, "core.prefix.protection", "core.command.setprotection.target_set",
                        net.kyori.adventure.text.Component.text(timeString, net.kyori.adventure.text.format.NamedTextColor.GREEN));
            }

            clientManager.sendMessageToRank("core.prefix.protection",
                    Translations.component("core.command.setprotection.broadcast",
                            net.kyori.adventure.text.Component.text(player.getName(), net.kyori.adventure.text.format.NamedTextColor.YELLOW),
                            net.kyori.adventure.text.Component.text(target.getName(), net.kyori.adventure.text.format.NamedTextColor.YELLOW),
                            net.kyori.adventure.text.Component.text(timeString, net.kyori.adventure.text.format.NamedTextColor.GREEN)
                    ),
                    Rank.TRIAL_MOD);
        });

    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
