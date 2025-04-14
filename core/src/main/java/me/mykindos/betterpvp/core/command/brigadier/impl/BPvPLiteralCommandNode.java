package me.mykindos.betterpvp.core.command.brigadier.impl;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.concurrent.CompletableFuture;
import lombok.CustomLog;

@CustomLog
public class BPvPLiteralCommandNode extends LiteralCommandNode<CommandSourceStack> {
    public BPvPLiteralCommandNode(LiteralCommandNode<CommandSourceStack> sourceNode) {
        super(sourceNode.getLiteral(), sourceNode.getCommand(), sourceNode.getRequirement(), sourceNode.getRedirect(), sourceNode.getRedirectModifier(), sourceNode.isFork());
        for (CommandNode<CommandSourceStack> child : sourceNode.getChildren()) {
            this.addChild(child);
        }
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        log.info("Suggesting for {}", this.getLiteral()).submit();
        if (canUse(context.getSource())) {
            log.info("Can use {}", this.getLiteral()).submit();
            return super.listSuggestions(context, builder);
        }
        log.info("Cannot use {}", this.getLiteral()).submit();
        return Suggestions.empty();
    }
}
