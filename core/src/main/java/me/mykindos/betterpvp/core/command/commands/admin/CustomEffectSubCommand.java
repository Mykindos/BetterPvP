package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class CustomEffectSubCommand extends Command {
    @Inject
    protected EffectManager effectManager;

    @Inject
    protected GamerManager gamerManager;

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();

        if (args.length == 0) return super.processTabComplete(sender, args);;

        String lowercaseArg = args[args.length - 1].toLowerCase();
        switch(getArgumentType(args.length)) {
            case "EFFECT" ->
                    tabCompletions.addAll(Arrays.stream(EffectType.values()).map(EffectType::toString).filter(name -> name.toLowerCase().startsWith(lowercaseArg)).toList());
        }
        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }

}