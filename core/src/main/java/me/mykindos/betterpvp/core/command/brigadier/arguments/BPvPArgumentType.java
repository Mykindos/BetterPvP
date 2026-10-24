package me.mykindos.betterpvp.core.command.brigadier.arguments;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BPvPArgumentType<T, N> implements CustomArgumentType<@NotNull T, @NotNull N> {
    @Getter
    private final String name;
    protected BPvPArgumentType(String name) {
        this.name = name;
    }

    /**
     * @param source the command source
     * @return the online player executing the command, or {@code null} when the executor is console or not a player
     */
    protected static @Nullable Player executingPlayer(CommandSourceStack source) {
        final Entity executor = source.getExecutor();
        return executor == null ? null : Bukkit.getPlayer(executor.getUniqueId());
    }
}
