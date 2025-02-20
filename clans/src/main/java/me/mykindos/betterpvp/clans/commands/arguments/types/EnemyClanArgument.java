package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
/**
 * Prompts the sender with a list of enemy Clans to the executor, guarantees a valid Clan return, but not a valid enemy
 */
@Singleton
public class EnemyClanArgument extends ClanArgument {
    @Inject
    protected EnemyClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Enemy Clan";
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        //TODO executor isnt updated when typing out command, might be better to rename this and specifically target sender
        if (!(context.getSource() instanceof CommandSourceStack sourceStack)) return super.listSuggestions(context, builder);
        Optional<Clan> executorClanOptional = clanManager.getClanByPlayer(sourceStack.getExecutor().getUniqueId());
        if (executorClanOptional.isEmpty()) return super.listSuggestions(context, builder);;
        Clan executorClan = executorClanOptional.get();

        clanManager.getObjects().values().stream()
                .filter(executorClan::isEnemy)
                .map(Clan::getName)
                .filter(name -> name.toLowerCase().contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

}
