package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.events.kill.PlayerSuicideEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
public class KillCommand extends Command {

    private final EffectManager effectManager;

    @Inject
    public KillCommand(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @Override
    public String getName() {
        return "kill";
    }

    @Override
    public String getDescription() {
        return "core.command.kill.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            if (effectManager.hasEffect(player, EffectTypes.FROZEN))
            {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.kill.self.blocked");
                return;
            }
            UtilServer.callEvent(new PlayerSuicideEvent(player, () -> {
                player.setHealth(0);
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.kill.self.success");
            }));

        } else {

            if (client.hasRank(Rank.ADMIN)) {
                if (args.length != 1) {
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.kill.other.usage");
                    return;
                }

                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {

                    target.setHealth(0);
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.kill.other.success",
                            Component.text(target.getName(), NamedTextColor.YELLOW));
                }
            }
        }
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ArgumentType.PLAYER.name();
        }

        return ArgumentType.NONE.name();
    }
}
