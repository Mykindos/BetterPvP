package me.mykindos.betterpvp.progression.profession.fishing.loot;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemDropEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLootType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Random;

@Data
@CustomLog
public class TreasureType implements FishingLootType {

    private static final Random RANDOM = new Random();

    @Getter(AccessLevel.NONE)
    private final String key;
    @Getter(AccessLevel.NONE)
    private final ItemFactory itemFactory;

    private int frequency;
    private NamespacedKey itemKey;
    private int minAmount;
    private int maxAmount;

    @Override
    public @NotNull String getName() {
        return "Treasure";
    }

    public ItemStack generateItem(int count) {
        final BaseItem baseItem =  itemFactory.getItemRegistry().getItem(itemKey);
        Preconditions.checkArgument(baseItem != null, "Invalid item key: " + itemKey);
        final ItemStack itemStack = itemFactory.create(baseItem).createItemStack();
        itemStack.setAmount(count);
        return itemStack;
    }

    @Override
    public FishingLoot generateLoot() {
        return new FishingLoot() {
            @Override
            public @NotNull FishingLootType getType() {
                return TreasureType.this;
            }

            @Override
            public void processCatch(PlayerCaughtFishEvent event) {
                final int count = RANDOM.ints(minAmount, maxAmount + 1)
                        .findFirst()
                        .orElse(minAmount);
                final Item item = (Item) Objects.requireNonNull(event.getCaught());
                item.setItemStack(generateItem(count));
                UtilItem.reserveItem(item, event.getPlayer(), 10);
                UtilServer.callEvent(new SpecialItemDropEvent(item, "Fishing"));

                log.info("{} caught {}x {}.", event.getPlayer().getName(), count, itemKey.toString())
                        .addClientContext(event.getPlayer()).addLocationContext(item.getLocation()).submit();
            }
        };
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.frequency = config.getOrSaveInt("fishing.loot." + key + ".frequency", 1);
        this.minAmount = config.getOrSaveInt("fishing.loot." + key + ".minAmount", 1);
        this.maxAmount = config.getOrSaveInt("fishing.loot." + key + ".maxAmount", 1);
        final String itemKey = config.getOrSaveString("fishing.loot." + key + ".item", "minecraft:diamond");
        if (itemKey == null) {
            throw new IllegalArgumentException("Item key cannot be null!");
        }
        NamespacedKey key = NamespacedKey.fromString(itemKey);
        if (key == null) {
            throw new IllegalArgumentException("Invalid item key: " + itemKey);
        }
        this.itemKey = key;
    }
}
