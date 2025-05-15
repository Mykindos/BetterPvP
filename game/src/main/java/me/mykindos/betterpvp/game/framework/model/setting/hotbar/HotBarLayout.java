package me.mykindos.betterpvp.game.framework.model.setting.hotbar;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the layout of a players hot bar.
 */
public class HotBarLayout {

    // Cannot have null values. Only includes items that are in the layout.
    // Key must range from 0-8
    private final Map<Integer, HotBarItem> layout;
    private final int maxTokens;
    @Getter
    private final RoleBuild build;

    /**
     * Creates a new HotBarLayout from an existing layout
     * @param layout The layout. It cannot contain null values, and the keys must range from 0-8
     * @param maxTokens The maximum amount of tokens a player can have
     */
    public HotBarLayout(RoleBuild build, Map<Integer, HotBarItem> layout, int maxTokens) {
        this.build = build;
        this.maxTokens = maxTokens;
        Preconditions.checkNotNull(layout);
        Preconditions.checkArgument(layout.size() <= 9, "Layout must contain 9 items");
        Preconditions.checkArgument(layout.keySet().stream().allMatch(i -> i >= 0 && i <= 8), "Keys must range from 0-8");
        Preconditions.checkArgument(layout.values().stream().allMatch(Objects::nonNull), "Values cannot be null");
        this.layout = layout;
    }

    /**
     * Creates a new HotBarLayout from an existing layout
     * @param hotBarLayout The layout to copy
     */
    public HotBarLayout(HotBarLayout hotBarLayout) {
        this.build = hotBarLayout.build;
        this.maxTokens = hotBarLayout.maxTokens;
        this.layout = new HashMap<>(hotBarLayout.layout);
    }

    /**
     * Creates a new HotBarLayout
     * @param maxTokens The maximum amount of tokens a player can have
     */
    public HotBarLayout(RoleBuild build, int maxTokens) {
        this.build = build;
        this.maxTokens = maxTokens;
        this.layout = new HashMap<>();
    }

    public int getRemainingTokens() {
        return maxTokens - layout.values().stream().mapToInt(HotBarItem::getTokenCost).sum();
    }

    public boolean canAddItem(HotBarItem item) {
        return getRemainingTokens() >= item.getTokenCost();
    }

    /**
     * Sets the item in the specified slot
     * @param slot The slot to set the item in
     * @param item The item to set
     */
    public void setSlot(int slot, HotBarItem item) {
        Preconditions.checkNotNull(item);
        Preconditions.checkArgument(slot >= 0 && slot <= 8, "Slot must range from 0-8");
        layout.put(slot, item);
    }

    /**
     * Removes the item in the specified slot
     * @param slot The slot to remove the item from
     *             If the slot does not contain an item, this method does nothing.
     */
    public void removeSlot(int slot) {
        Preconditions.checkArgument(slot >= 0 && slot <= 8, "Slot must range from 0-8");
        layout.remove(slot);
    }

    /**
     * Gets the item in the specified slot
     * @param slot The slot to get the item from
     * @return An optional containing the item, if it exists.
     */
    public Optional<HotBarItem> getSlot(int slot) {
        Preconditions.checkArgument(slot >= 0 && slot <= 8, "Slot must range from 0-8");
        return Optional.ofNullable(layout.get(slot));
    }

    /**
     * Get the entire layout.
     * @return The layout. This layout is guaranteed to:
     *         <ul>
     *             <li>Contain, at most, 9 items</li>
     *             <li>Have keys that range from 0-8</li>
     *             <li>Have values that are not null</li>
     *         </ul>
     */
    public Map<Integer, HotBarItem> getLayout() {
        return Collections.unmodifiableMap(layout);
    }

    public void copy(HotBarLayout updated) {
        Preconditions.checkNotNull(updated);
        Preconditions.checkArgument(layout.size() <= 9, "Layout must range from 0-8");
        Preconditions.checkArgument(updated.maxTokens == maxTokens, "Max tokens must be the same");
        layout.clear();
        layout.putAll(updated.layout);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        HotBarLayout that = (HotBarLayout) o;
        return maxTokens == that.maxTokens && layout.equals(that.layout) && build.getId() == that.build.getId();
    }

    @Override
    public int hashCode() {
        int result = layout.hashCode();
        result = 31 * result + maxTokens;
        return result;
    }
}
