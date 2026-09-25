package me.mykindos.betterpvp.core.tips.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.tips.Tip;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import me.mykindos.betterpvp.core.utilities.model.ChatIcon;


@Singleton
public class SendTipCommand extends Command {

    private final TipManager tipManager;

    @Inject
    public SendTipCommand(TipManager tipManager) {
        this.tipManager = tipManager;
    }

    @Override
    public String getName() {
        return "sendtip";
    }

    @Override
    public String getDescription() {
        return "core.command.send-tip.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "core.prefix.command", "core.command.sendtip.usage");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            UtilMessage.message(player, "core.prefix.command", "core.command.sendtip.target.invalid", Component.text(args[0]));
            return;
        }

        String tipName = args[1].toLowerCase();
        Optional<Tip> tipOptional = tipManager.getObject(tipName);
        if (tipOptional.isEmpty()) {
            UtilMessage.message(player, "core.prefix.command", "core.command.sendtip.tip.invalid", Component.text(args[1]));
            return;
        }

        Tip tip = tipOptional.get();
        UtilMessage.spaced(target, ChatIcon.TIP.line(tip.getComponent()));
        new SoundEffect(Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f, 1.0f).play(target);

        UtilMessage.message(player, "core.prefix.command", "core.command.sendtip.sent", Component.text(tip.getName()), Component.text(target.getName()));
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            return tipManager.getObjects().keySet().stream()
                    .filter(name -> name.startsWith(partial))
                    .sorted()
                    .toList();
        }
        return super.processTabComplete(sender, args);
    }
}

