package me.mykindos.betterpvp.core.client.stats.impl.core;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.utilitiy.Relation;
import me.mykindos.betterpvp.core.client.stats.impl.utilitiy.Type;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

@Builder
@EqualsAndHashCode
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class DamageReasonStat implements IBuildableStat {
    public static final String TYPE = "DAMAGE_REASON";

    public static DamageReasonStat fromData(String statType, JSONObject data) {
        DamageReasonStat.DamageReasonStatBuilder builder = builder();
        Preconditions.checkArgument(statType.equals(TYPE));
        builder.relation(Relation.valueOf(data.getString("relation")));
        builder.type(Type.valueOf(data.getString("type")));
        builder.damageCause(EntityDamageEvent.DamageCause.valueOf(data.getString("damageCause")));
        builder.reason(data.optString("reason"));
        return builder.build();
    }


    @NotNull
    private Relation relation;
    @NotNull
    private Type type;
    @NotNull
    private EntityDamageEvent.DamageCause damageCause;
    @NotNull
    private String reason;


    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Long getStat(StatContainer statContainer, String periodKey) {
        return statContainer.getProperty(periodKey, this);
    }

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
                .putOnce("relation", relation.name())
                .putOnce("type", type.name())
                .putOnce("damageCause", damageCause.name())
                .putOnce("reason", reason);
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return true;
    }

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof DamageReasonStat other)) return false;
        if (relation != other.relation ||
                type != other.type ||
                damageCause != other.damageCause) return false;
        return (Strings.isNullOrEmpty(reason) || reason.equals(other.reason));
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        //todo
        return this;
    }

    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        final DamageReasonStat other = fromData(statType, data);
        this.relation = other.relation;
        this.type = other.type;
        this.damageCause = other.damageCause;
        this.reason = other.reason;
        return this;
    }

}
