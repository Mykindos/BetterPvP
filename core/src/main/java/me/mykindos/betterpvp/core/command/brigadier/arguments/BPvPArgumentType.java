package me.mykindos.betterpvp.core.command.brigadier.arguments;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import lombok.Getter;

public abstract class BPvPArgumentType<T, N> implements CustomArgumentType<T, N> {
    public static final SimpleCommandExceptionType INSUFFICIENT_PERMISSIONS = new SimpleCommandExceptionType(new LiteralMessage("You have insufficient permissions to use this command"));
    @Getter
    private final String name;
    protected BPvPArgumentType(String name) {
        this.name = name;
    }
}
