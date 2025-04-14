package me.mykindos.betterpvp.core.command.brigadier.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.SingleRedirectModifier;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.function.Predicate;

public class BPvPLiteralArgumentBuilder extends LiteralArgumentBuilder<CommandSourceStack> {
    public BPvPLiteralArgumentBuilder(String literal) {
        super(literal);
    }

    @Override
    protected BPvPLiteralArgumentBuilder getThis() {
        return (BPvPLiteralArgumentBuilder) super.getThis();
    }

    @Override
    public BPvPLiteralArgumentBuilder then(ArgumentBuilder<CommandSourceStack, ?> argument) {
        return (BPvPLiteralArgumentBuilder) super.then(argument);
    }

    @Override
    public BPvPLiteralArgumentBuilder then(CommandNode<CommandSourceStack> argument) {
        return (BPvPLiteralArgumentBuilder) super.then(argument);
    }

    @Override
    public BPvPLiteralCommandNode build() {
        return new BPvPLiteralCommandNode(super.build());
    }

    @Override
    public BPvPLiteralArgumentBuilder requires(Predicate<CommandSourceStack> requirement) {
        return (BPvPLiteralArgumentBuilder) super.requires(requirement);
    }

    @Override
    public BPvPLiteralArgumentBuilder redirect(CommandNode<CommandSourceStack> target) {
        return (BPvPLiteralArgumentBuilder) super.redirect(target);
    }

    @Override
    public BPvPLiteralArgumentBuilder redirect(CommandNode<CommandSourceStack> target, SingleRedirectModifier<CommandSourceStack> modifier) {
        return (BPvPLiteralArgumentBuilder) super.redirect(target, modifier);
    }

    @Override
    public BPvPLiteralArgumentBuilder fork(CommandNode<CommandSourceStack> target, RedirectModifier<CommandSourceStack> modifier) {
        return (BPvPLiteralArgumentBuilder) super.fork(target, modifier);
    }

    @Override
    public BPvPLiteralArgumentBuilder forward(CommandNode<CommandSourceStack> target, RedirectModifier<CommandSourceStack> modifier, boolean fork) {
        return (BPvPLiteralArgumentBuilder) super.forward(target, modifier, fork);
    }

    @Override
    public BPvPLiteralArgumentBuilder executes(Command<CommandSourceStack> command) {
        return (BPvPLiteralArgumentBuilder) super.executes(command);
    }
}
