package me.mykindos.betterpvp.game.framework.model.chat;

import me.mykindos.betterpvp.game.framework.AbstractGame;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GenericColorProvider implements PlayerColorProvider {
    @Override
    public @NotNull TextColor getColor(Player player, AbstractGame<?, ?> game) {
        return NamedTextColor.YELLOW;
    }
}
