package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
/**
 * Prompts the sender with a list of allied Clans to the executor, guarantees a valid Clan return, but not a valid ally
 */
@Singleton
public class TrustedClanArgument extends ClanArgument {
    @Inject
    protected TrustedClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Trusted Clan";
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        //TODO executor isnt updated when typing out command, might be better to rename this and specifically target sender
        if (!(context.getSource() instanceof CommandSourceStack sourceStack)) return super.listSuggestions(context, builder);
        Optional<Clan> executorClanOptional = clanManager.getClanByPlayer(sourceStack.getExecutor().getUniqueId());
        if (executorClanOptional.isEmpty()) return super.listSuggestions(context, builder);;
        Clan executorClan = executorClanOptional.get();

        clanManager.getObjects().values().stream()
                .filter(clan -> {
                    Optional<ClanAlliance> clanAllianceOptional = clan.getAlliance(executorClan);
                    return (clanAllianceOptional.isPresent() && clanAllianceOptional.get().isTrusted());
                })
                .map(Clan::getName)
                .filter(name -> name.toLowerCase().contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

}
