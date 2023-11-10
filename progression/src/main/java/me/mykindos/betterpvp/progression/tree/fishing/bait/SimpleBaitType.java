package me.mykindos.betterpvp.progression.tree.fishing.bait;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.item.skull.SkullBuilder;
import me.mykindos.betterpvp.progression.tree.fishing.model.BaitType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

@Data
public abstract class SimpleBaitType implements BaitType {

    @Getter(AccessLevel.NONE)
    protected final String key;
    private String name;
    private double radius;
    private double expiration;
    private String texture;

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.texture = config.getOrSaveObject("fishing.bait." + key + ".texture", "https://google.com/", String.class);
        this.radius = config.getOrSaveObject("fishing.bait." + key + ".radius", 2.5D, Double.class);
        this.expiration = config.getOrSaveObject("fishing.bait." + key + ".seconds", 10.0D, Double.class);
        this.name = config.getOrSaveObject("fishing.bait." + key + ".name", "Bait", String.class);
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public double getExpiration() {
        return expiration;
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) double getRadius() {
        return radius;
    }

    @Override
    public ItemStack getRawItem() {
        return new SkullBuilder(texture).build();
    }
}
