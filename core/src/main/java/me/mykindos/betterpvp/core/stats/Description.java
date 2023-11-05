package me.mykindos.betterpvp.core.stats;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;

@Value
@Builder
public class Description {

    @NotNull ItemStack icon;
    @NotNull @Singular
    Map<String, Component> lines;
    @Nullable
    Consumer<Player> clickFunction;

}
