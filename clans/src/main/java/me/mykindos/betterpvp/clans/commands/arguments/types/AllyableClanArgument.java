package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Prompts the sender with a list of allyable Clans to the executor, guarantees a valid Clan return, but not a valid allyable clan
 */
@Singleton
public class AllyableClanArgument extends ClanArgument {
    @Inject
    protected AllyableClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Allyable Clan";
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        //TODO executor isnt updated when typing out command, might be better to rename this and specifically target sender
        if (!(context.getSource() instanceof final CommandSourceStack sourceStack))
            return super.listSuggestions(context, builder);
        final Optional<Clan> executorClanOptional = clanManager.getClanByPlayer(Objects.requireNonNull(sourceStack.getExecutor()).getUniqueId());
        if (executorClanOptional.isEmpty()) return super.listSuggestions(context, builder);
        final Clan executorClan = executorClanOptional.get();

        clanManager.getObjects().values().stream()
                .filter(clan -> clanManager.canAlly(executorClan, clan))
                .map(Clan::getName)
                .filter(name -> name.toLowerCase().contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}