package me.mykindos.betterpvp.progression.tree.fishing.loot;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Data
public class SwimmerType implements FishingLootType {

    private int frequency;
    private EntityType entityType;
    @Getter(AccessLevel.NONE)
    private final String key;

    @Override
    public @NotNull String getName() {
        return "Swimmer";
    }

    @Override
    public FishingLoot generateLoot() {
        return new FishingLoot() {
            @Override
            public @NotNull FishingLootType getType() {
                return SwimmerType.this;
            }

            @Override
            public ItemStack processCatch(PlayerFishEvent event) {
                final Entity item = Objects.requireNonNull(event.getCaught());
                final Location location = item.getLocation();
                item.remove();
                final Vector direction = event.getPlayer().getLocation().subtract(location)
                        .toVector()
                        .normalize()
                        .multiply(1.5)
                        .add(new Vector(0, 0.25, 0));

                location.getWorld().spawnEntity(location, entityType, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                    if (entity instanceof Zombie zombie) {
                        zombie.setShouldBurnInDay(false);
                    }
                    if (entity instanceof Skeleton skeleton) {
                        skeleton.setShouldBurnInDay(false);
                    }
                    entity.setVelocity(direction);
                    entity.getPersistentDataContainer().set(ProgressionNamespacedKeys.FISHING_SWIMMER, PersistentDataType.BOOLEAN, true);
                });
                return null;
            }
        };
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        this.frequency = config.getOrSaveInt("fishing.loot." + key + ".frequency", 1);
        final String type = config.getOrSaveString("fishing.loot." + key + ".entity", "PIG");
        if (type == null) {
            throw new IllegalArgumentException("Entity type cannot be null!");
        }

        try {
            this.entityType = EntityType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid entity type: " + type, e);
        }
    }
}
