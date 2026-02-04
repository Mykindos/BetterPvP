package me.mykindos.betterpvp.core.client.stats.impl.core.item;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;

@Builder
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class ItemStat implements IBuildableStat {
    public static final String TYPE = "ITEM_STAT";

    public static ItemStat fromData(String statType, JSONObject data) {
        ItemStat.ItemStatBuilder builder = ItemStat.builder();
        Preconditions.checkArgument(statType.equals(TYPE));
        builder.itemStack(ItemStack.deserialize(data.getJSONObject("itemStack").toMap()));
        builder.action(Action.valueOf(data.getString("action")));
        return builder.build();
    }

    @Nullable
    //TODO generic without an item
    private ItemStack itemStack;
    private Action action;

    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statType the statname
     * @param data
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        ItemStat other = fromData(statType, data);
        this.itemStack = other.itemStack;
        this.action = other.action;
        return this;
    }

    private boolean filterDamageCause(Map.Entry<IStat, Long> entry) {
        ItemStat other = (ItemStat) entry.getKey();
        return action.equals(other.action);
    }

    /**
     * Get the stat represented by this object from the statContainer.
     * period object must be the correct type as defined by the type
     *
     * @param statContainer the statContainer to source the value from
     * @param type          what type of period is being fetched from
     * @param period        The period being fetched from, must be {@link Realm} or {@link Season} if type is not ALL
     * @return the stat value represented by this stat
     */
    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        if (itemStack == null) {
            return getFilteredStat(statContainer, type, period, this::filterDamageCause);
        }
        return statContainer.getProperty(type, period, this);
    }

    /**
     * What type of stat this is, a LONG (default), DOUBLE, OR DURATION
     *
     * @return the type of stat
     */
    @Override
    public @NotNull StatValueType getStatValueType() {
        return StatValueType.LONG;
    }

    /**
     * Get the name that is stored in the DB
     *
     * @return
     */
    @Override
    public @NotNull String getStatType() {
        return TYPE;
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return new JSONObject()
                .putOpt("itemStack", itemStack == null ? null : itemStack.serialize())
                .putOnce("action", action.name());
    }

    /**
     * Get the simple name of this stat, without qualifications (if present)
     * <p>
     * i.e. Time Played, Flags Captured
     *
     * @return the simple name
     */
    @Override
    public String getSimpleName() {
        return "Item " + action.name();
    }

    /**
     * Get the qualified name of the stat, if one exists.
     * Should usually end with the {@link IStat#getSimpleName()}
     * <p>
     * i.e. Domination Time Played, Capture the Flag CTF_Oakvale Flags Captured
     *
     * @return the qualified name
     */
    @Override
    public String getQualifiedName() {
        if (itemStack != null) {
            return UtilItem.getItemIdentifier(itemStack) + " " + getSimpleName();
        }
        return getSimpleName();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return itemStack != null;
    }

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof ItemStat other)) return false;
        if ((action != other.action)) return false;
        return (itemStack != null && itemStack.equals(other.itemStack));
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return ItemStat.builder()
                .action(this.action)
                .build();
    }


    public enum Action {
        CRAFT,
        REFORGE,
        ATTUNE,
        ANVIL_PRIMARY,
        ANVIL_SECONDARY,
    }



}
