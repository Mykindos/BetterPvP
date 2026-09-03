package me.mykindos.betterpvp.core.item.component.impl.currency.disc;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@EqualsAndHashCode
public class CurrencyDiscComponent implements ItemComponent, LoreComponent {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "currency_disc");

    private final int value;

    @Override
    public List<Component> getLines(ItemInstance item) {
        return List.of(
                Component.text("Worth:", NamedTextColor.GRAY).appendSpace().append(Component.text("$" + this.value, NamedTextColor.GOLD)),
                Component.space(),
                Component.text("An exchangeable record valued for", NamedTextColor.WHITE),
                Component.text("its fixed worth, ensuring balanced", NamedTextColor.WHITE),
                Component.text("transactions in commerce.", NamedTextColor.WHITE)
        );
    }

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return KEY;
    }

    @Override
    public ItemComponent copy() {
        return new CurrencyDiscComponent(this.value);
    }

    @Override
    public int getRenderPriority() {
        return 1002;
    }
}