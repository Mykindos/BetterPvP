package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

abstract class CustomEffectSubCommand extends Command {
    @Inject
    protected EffectManager effectManager;

    @Inject
    protected ClientManager gamerManager;

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();

        if (args.length == 0) return super.processTabComplete(sender, args);


        String lowercaseArg = args[args.length - 1].toLowerCase();
        if (getArgumentType(args.length).equals("EFFECT")) {
            tabCompletions.addAll(EffectTypes.getEffectTypes().stream().map(type -> type.getName().replace(" ", "_"))
                    .filter(name -> name.toLowerCase().startsWith(lowercaseArg)).toList());
        }
        
        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }

}