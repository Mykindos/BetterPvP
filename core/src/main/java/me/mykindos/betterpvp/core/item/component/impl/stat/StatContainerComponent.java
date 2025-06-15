package me.mykindos.betterpvp.core.item.component.impl.stat;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Container for item statistics. Serialization is handled by StatContainerSerializer.
 */
public class StatContainerComponent extends AbstractItemComponent implements LoreComponent {

    private final List<ItemStat<?>> baseStats;
    private final List<ItemStat<?>> modifierStats;

    /**
     * Creates a new StatContainerComponent with the given base and modifier stats.
     * 
     * @param base     The base stats
     * @param modifier The modifier stats
     */
    @Contract(pure = false)
    public StatContainerComponent(List<ItemStat<?>> base, List<ItemStat<?>> modifier) {
        super("stats");
        this.baseStats = new ArrayList<>(base);
        this.modifierStats = new ArrayList<>(modifier);
    }

    /**
     * Creates a new StatContainerComponent with the given base stats and no modifier stats.
     * 
     * @param base The base stats
     */
    @Contract(pure = false)
    public StatContainerComponent(List<ItemStat<?>> base) {
        this(base, new ArrayList<>());
    }

    /**
     * Creates a new empty StatContainerComponent.
     */
    @Contract(pure = false)
    public StatContainerComponent() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Gets all stats as a merged list, where base stats are overridden by modifier stats of the same type.
     * 
     * @return A new list containing merged stats
     */
    @Contract(pure = true, value = "-> new")
    public final @NotNull List<ItemStat<?>> getStats() {
        // Merge base and modifier stats by type
        List<ItemStat<?>> merged = new ArrayList<>(baseStats);
        for (ItemStat<?> modifier : modifierStats) {
            boolean mergedFlag = false;
            for (int i = 0; i < merged.size(); i++) {
                ItemStat<?> base = merged.get(i);
                if (base.getClass() == modifier.getClass()) {
                    // Merge using the merge method
                    merged.set(i, base.merge(modifier));
                    mergedFlag = true;
                    break;
                }
            }
            if (!mergedFlag) {
                merged.add(modifier);
            }
        }
        return merged;
    }

    /**
     * Checks if this container has a stat of the given type.
     * 
     * @param clazz The stat class to check for
     * @param <T>   The stat type
     * @return true if this container has a stat of the given type, false otherwise
     */
    @Contract(pure = true)
    public <T extends ItemStat<?>> boolean hasStat(@NotNull Class<T> clazz) {
        return getStats().stream()
                .anyMatch(clazz::isInstance);
    }

    /**
     * Gets a stat of the given type if present.
     * 
     * @param clazz The stat class to get
     * @param <T>   The stat type
     * @return An Optional containing the stat if found, empty otherwise
     */
    @Contract(pure = true, value = "_ -> new")
    public <T extends ItemStat<?>> @NotNull Optional<T> getStat(@NotNull Class<T> clazz) {
        return getStats().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst();
    }

    /**
     * Gets a defensive copy of the base stats.
     * 
     * @return A new list containing all base stats
     */
    @Contract(pure = true, value = "-> new")
    public @NotNull List<ItemStat<?>> getBaseStats() {
        return new ArrayList<>(baseStats);
    }

    /**
     * Gets a defensive copy of the modifier stats.
     * 
     * @return A new list containing all modifier stats
     */
    @Contract(pure = true, value = "-> new")
    public @NotNull List<ItemStat<?>> getModifierStats() {
        return new ArrayList<>(modifierStats);
    }

    /**
     * Adds or replaces a base stat.
     * If a stat of the same type already exists, it will be replaced.
     * 
     * @param stat The stat to add or replace
     * @return This component instance for method chaining
     */
    @Contract(mutates = "this", value = "_ -> this")
    public @NotNull StatContainerComponent withBaseStat(@NotNull ItemStat<?> stat) {
        boolean replaced = false;
        for (int i = 0; i < baseStats.size(); i++) {
            if (baseStats.get(i).getClass() == stat.getClass()) {
                baseStats.set(i, stat);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            baseStats.add(stat);
        }
        return this;
    }

    /**
     * Removes a base stat by type.
     * 
     * @param statClass The class of the stat to remove
     * @return This component instance for method chaining
     */
    @Contract(mutates = "this", value = "_ -> this")
    public @NotNull StatContainerComponent withoutBaseStat(@NotNull Class<? extends ItemStat<?>> statClass) {
        baseStats.removeIf(statClass::isInstance);
        return this;
    }

    /**
     * Adds or replaces a modifier stat.
     * If a stat of the same type already exists, it will be replaced.
     * 
     * @param stat The modifier stat to add or replace
     * @return This component instance for method chaining
     */
    @Contract(mutates = "this", value = "_ -> this")
    public @NotNull StatContainerComponent withModifierStat(@NotNull ItemStat<?> stat) {
        boolean replaced = false;
        for (int i = 0; i < modifierStats.size(); i++) {
            if (modifierStats.get(i).getClass() == stat.getClass()) {
                modifierStats.set(i, stat);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            modifierStats.add(stat);
        }
        return this;
    }

    /**
     * Removes a modifier stat by type.
     * 
     * @param statClass The class of the modifier stat to remove
     * @return This component instance for method chaining
     */
    @Contract(mutates = "this", value = "_ -> this")
    public @NotNull StatContainerComponent withoutModifierStat(@NotNull Class<? extends ItemStat<?>> statClass) {
        modifierStats.removeIf(statClass::isInstance);
        return this;
    }

    /**
     * Generates lore lines for this component.
     *
     * @param item The item this component is attached to
     * @return A list of lore lines
     */
    @Contract(value = "_ -> new", pure = true)
    @Override
    public @NotNull List<Component> getLines(ItemInstance item) {
        List<Component> lines = new ArrayList<>();
        for (ItemStat<?> stat : getStats()) {
            final String value = stat.stringValue();
            final Component line = Component.text(value, stat.getValueColor())
                    .appendSpace()
                    .append(Component.text(stat.getShortName(), NamedTextColor.GRAY));
            lines.add(line);
        }
        return lines;
    }

    @Contract(pure = true)
    @Override
    public int getRenderPriority() {
        return Integer.MAX_VALUE;
    }

    /**
     * Creates a deep copy of this component.
     * 
     * @return A new StatContainerComponent with copies of all stats
     */
    @Contract(pure = true, value = "-> new")
    @Override
    public @NotNull ItemComponent copy() {
        List<ItemStat<?>> copiedBaseStats = new ArrayList<>();
        for (ItemStat<?> stat : baseStats) {
            copiedBaseStats.add(stat.copy());
        }
        List<ItemStat<?>> copiedModifierStats = new ArrayList<>();
        for (ItemStat<?> stat : modifierStats) {
            copiedModifierStats.add(stat.copy());
        }
        return new StatContainerComponent(copiedBaseStats, copiedModifierStats);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StatContainerComponent that = (StatContainerComponent) o;
        return Objects.equals(baseStats, that.baseStats) && Objects.equals(modifierStats, that.modifierStats);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        // both lists do not have the same hashcode despite being equal, so we sum their elements' hashcodes
        result = 31 * result + (baseStats != null ? baseStats.stream().mapToInt(ItemStat::hashCode).sum() : 0);
        result = 31 * result + (modifierStats != null ? modifierStats.stream().mapToInt(ItemStat::hashCode).sum() : 0);
        return result;
    }
}
