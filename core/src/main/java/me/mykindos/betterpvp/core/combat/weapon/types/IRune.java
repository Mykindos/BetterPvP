package me.mykindos.betterpvp.core.combat.weapon.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import me.mykindos.betterpvp.core.items.type.IBPvPItem;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.Tag;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public interface IRune extends IBPvPItem {
    String[] getItemFilter();

    String getCategory();

    List<Component> getRuneLoreDescription(ItemMeta meta);

    List<Component> getItemLoreDescription(PersistentDataContainer pdc, ItemStack itemStack);

    <T extends Number> T getRollFromItem(PersistentDataContainer pdc, NamespacedKey key, PersistentDataType<T, T> type);

    <T extends Number> T getRollFromMeta(ItemMeta meta, NamespacedKey key, PersistentDataType<T, T> type, T defaultValue);

    int getTier();

    /**
     * @return The namespaced key that is applied when the rune is added to an item
     */
    NamespacedKey getAppliedNamespacedKey();

    boolean canApplyToItem(ItemMeta runeMeta, ItemMeta itemMeta);

    void applyToItem(ItemMeta runeMeta, ItemMeta itemMeta);

    boolean itemMatchesFilter(Material material);

    boolean hasRune(ItemMeta meta);

    NamespacedKey getRuneOfSameType(ItemMeta itemMeta);

    @Override
    List<Component> getLore(ItemMeta meta);

    String getStarPrefix(int tier);

    String getConfigName();

    @Getter
    class RuneData {
        private final NamespacedKey type;
        private final int tier;
        private final Map<String, String> data;
        public RuneData(NamespacedKey type, ItemStack itemStack) {
            this.type = type;
            data = new HashMap<>();
            CraftPersistentDataContainer dataContainer = (CraftPersistentDataContainer) itemStack.getItemMeta().getPersistentDataContainer().get(type, PersistentDataType.TAG_CONTAINER);
            this.tier = Objects.requireNonNull(Objects.requireNonNull(dataContainer).get(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER));

            for (Map.Entry<String, Tag> entry : Objects.requireNonNull(dataContainer).getRaw().entrySet()) {
                String key = entry.getKey();
                Tag value = entry.getValue();
                if (key.equalsIgnoreCase(RuneNamespacedKeys.TIER.asString()) ||
                key.equalsIgnoreCase(RuneNamespacedKeys.OWNING_RUNE.asString())) continue;
                data.put(key, value.toString());
            }

        }

        /**
         * Format this rune's data as a single string
         * @return
         */
        public String getDataString() {
            List<String> runeValues = getData().entrySet().stream()
                    .map((entry) -> entry.getKey() + ": " + entry.getValue())
                    .toList();
            return String.join(" | ", runeValues);
        }

        @Override
        public String toString() {
            return "RuneData{" +
                    "type=" + type +
                    ", tier=" + tier +
                    ", data=" + data +
                    '}';
        }
    }
}
