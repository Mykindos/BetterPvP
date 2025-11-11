package me.mykindos.betterpvp.core.item.component.impl.fuel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Component that defines an item as fuel for smelters.
 * Stores the burn time (in milliseconds) and maximum temperature the fuel can reach.
 */
@Data
@EqualsAndHashCode
public class FuelComponent implements ItemComponent, LoreComponent {
    
    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "fuel");
    
    private final long burnTime; // Burn time in milliseconds
    private final float maxTemperature; // Maximum temperature in Celsius
    
    /**
     * Creates a new FuelComponent.
     * @param burnTime The burn time in milliseconds
     * @param maxTemperature The maximum temperature this fuel can reach in Celsius
     */
    public FuelComponent(long burnTime, float maxTemperature) {
        this.burnTime = burnTime;
        this.maxTemperature = maxTemperature;
    }
    
    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return KEY;
    }
    
    @Override
    public ItemComponent copy() {
        return new FuelComponent(burnTime, maxTemperature);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        int seconds = (int) (burnTime / 1000);
        return List.of(
                Component.text("Fuel", TextColor.color(255, 66, 66), TextDecoration.BOLD),
                Component.text("Burns for ", TextColor.color(171, 171, 171))
                        .append(Component.text(seconds + " seconds", TextColor.color(255, 156, 51))),
                Component.text("Heats to ", TextColor.color(171, 171, 171))
                        .append(Component.text((int) maxTemperature + " °C", TextColor.color(255, 85, 28)))
        );
    }

    @Override
    public int getRenderPriority() {
        return 1001;
    }
}