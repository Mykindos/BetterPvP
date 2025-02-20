package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
/**
 * Prompts the sender with a list of allied Clans to the executor, guarantees a valid Clan return, but not a valid ally
 */
@CustomLog
@Singleton
public class AllyClanArgument extends ClanArgument {
    @Inject
    protected AllyClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Ally Clan";
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        //TODO executor isnt updated when typing out command, might be better to rename this and specifically target sender
        if (!(context.getSource() instanceof CommandSourceStack sourceStack)) return super.listSuggestions(context, builder);
        Optional<Clan> executorClanOptional = clanManager.getClanByPlayer(sourceStack.getExecutor().getUniqueId());
        if (executorClanOptional.isEmpty()) return super.listSuggestions(context, builder);;
        Clan executorClan = executorClanOptional.get();

        log.info("Ally Clan Suggestions").submit();

        clanManager.getObjects().values().stream()
                .filter(executorClan::isAllied)
                .map(Clan::getName)
                .filter(name -> name.contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

}
