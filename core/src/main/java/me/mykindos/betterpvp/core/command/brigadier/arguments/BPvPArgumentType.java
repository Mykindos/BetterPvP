package me.mykindos.betterpvp.core.command.brigadier.arguments;

import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import lombok.Getter;

public abstract class BPvPArgumentType<T, N> implements CustomArgumentType<T, N> {
    @Getter
    private final String name;

    protected BPvPArgumentType(String name) {
        this.name = name;
    }
}
