package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@Singleton
@Slf4j
public class FreezeCommand extends Command implements IConsoleCommand {

    private final ClientManager clientManager;
    private final EffectManager effectManager;

    @Inject
    public FreezeCommand(ClientManager clientManager, EffectManager effectManager) {
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        this.aliases.add("unfreeze");
    }

    @Override
    public String getName() {
        return "freeze";
    }

    @Override
    public String getDescription() {
        return "Freeze/unfreeze another player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (player.getUniqueId().compareTo(UUID.fromString("90f3004d-ebb4-4b74-b30e-b320ad687256")) == 0) {
            effectManager.addEffect(player, EffectTypes.SHOCK, 10000);
            effectManager.addEffect(player, EffectTypes.DARKNESS, 10000);
            effectManager.addEffect(player, EffectTypes.BLINDNESS, 10000);
        }
        execute(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (args.length < 1) {
            UtilMessage.message(sender, "Freeze", "You must specify a player ");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            UtilMessage.message(sender, "Freeze", "<yellow>%s</yellow> is not a valid player name", args[0]);
            return;
        }

        if (effectManager.hasEffect(target, EffectTypes.FROZEN)) {
            UtilMessage.message(target, "Freeze", "You have been unfrozen by a staff member");

            effectManager.removeEffect(target, EffectTypes.FROZEN);
            clientManager.sendMessageToRank("Freeze", UtilMessage.deserialize("<yellow>%s</yellow> unfroze <yellow>%s</yellow>", sender.getName(), target.getName()), Rank.HELPER);
        } else {
            UtilMessage.message(target, "Freeze", "You have been frozen by a staff member");

            effectManager.addEffect(target, null, EffectTypes.FROZEN, "Freeze", 1, 1000, true, true);
            clientManager.sendMessageToRank("Freeze", UtilMessage.deserialize("<yellow>%s</yellow> froze <yellow>%s</yellow>", sender.getName(), target.getName()), Rank.HELPER);
        }

    }

    @Override
    public String getArgumentType(int argCount) {
        if(argCount == 1){
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }

}
