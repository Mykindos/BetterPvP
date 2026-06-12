package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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

@Singleton
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
        return "core.command.freeze.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        execute(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (args.length < 1) {
            UtilMessage.message(sender, "core.prefix.freeze", "core.command.freeze.usage");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            UtilMessage.message(sender, "core.prefix.freeze", "core.command.freeze.target_not_found", args[0]);
            return;
        }

        if (effectManager.hasEffect(target, EffectTypes.FROZEN)) {
            UtilMessage.message(target, "core.prefix.freeze", "core.command.freeze.unfrozen_target");

            effectManager.removeEffect(target, EffectTypes.FROZEN);
            clientManager.sendMessageToRank("core.prefix.freeze",
                    me.mykindos.betterpvp.core.locale.Translations.component("core.command.freeze.unfrozen_staff",
                            net.kyori.adventure.text.Component.text(sender.getName(), net.kyori.adventure.text.format.NamedTextColor.YELLOW),
                            net.kyori.adventure.text.Component.text(target.getName(), net.kyori.adventure.text.format.NamedTextColor.YELLOW)),
                    Rank.TRIAL_MOD);
        } else {
            UtilMessage.message(target, "core.prefix.freeze", "core.command.freeze.frozen_target");

            effectManager.addEffect(target, null, EffectTypes.FROZEN, "Freeze", 1, 1000, true, true);
            clientManager.sendMessageToRank("core.prefix.freeze",
                    me.mykindos.betterpvp.core.locale.Translations.component("core.command.freeze.frozen_staff",
                            net.kyori.adventure.text.Component.text(sender.getName(), net.kyori.adventure.text.format.NamedTextColor.YELLOW),
                            net.kyori.adventure.text.Component.text(target.getName(), net.kyori.adventure.text.format.NamedTextColor.YELLOW)),
                    Rank.TRIAL_MOD);
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
