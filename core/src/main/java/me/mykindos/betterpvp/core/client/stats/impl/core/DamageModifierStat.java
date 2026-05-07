package me.mykindos.betterpvp.core.client.stats.impl.core;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.Relation;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseRegistry;
import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

@Builder
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class DamageModifierStat implements IBuildableStat {
    public static final String STAT_TYPE = "DAMAGE_MODIFIER";
    private static final DamageCauseRegistry damageCauseRegistry = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(DamageCauseRegistry.class);

    public static DamageModifierStat fromData(String statType, JSONObject data) {
        DamageModifierStat.DamageModifierStatBuilder builder = builder();
        Preconditions.checkArgument(statType.equals(STAT_TYPE));
        builder.relation(Relation.valueOf(data.getString("relation")));
        builder.name(data.optString("name", null));
        if (data.has("damageOperator")) {
            builder.damageOperator(DamageOperator.valueOf(data.getString("damageOperator")));
        }
        if (data.has("modifierType")) {
            builder.modifierType(ModifierType.valueOf(data.getString("modifierType")));
        }
        if (data.has("damageOperand")) {
            builder.damageOperand(data.getDouble("damageOperand"));
        }
        if (data.has("damageCause")) {
            builder.damageCause(damageCauseRegistry.get(data.getString("damageCause")));
        }
        return builder.build();
    }

    @NotNull
    private Relation relation;
    /**
     * The name of the Modifier
     */
    @Nullable
    private String name;
    /**
     * The damage operator
     */
    @Nullable
    private DamageOperator damageOperator;
    /**
     * The modifier type
     */
    @Nullable
    private ModifierType modifierType;
    /**
     * the damage operand
     */
    @Nullable
    private Double damageOperand;
    /**
     * The damage cause
     */
    @Nullable
    private DamageCause damageCause;

    private boolean filterSpecificModifierUsage(Map.Entry<IStat, Long> entry) {
        DamageModifierStat other = (DamageModifierStat) entry.getKey();
        if (!relation.equals(other.relation)) return false;
        if (name != null && !Objects.equals(name, other.name)) return false;
        if (damageOperator != null && damageOperator != other.damageOperator) return false;
        if (modifierType != null && modifierType != other.modifierType) return false;
        return damageCause == null || Objects.equals(damageCause, other.damageCause);
    }

    private boolean filterByModifierTypeOnly(Map.Entry<IStat, Long> entry) {
        DamageModifierStat other = (DamageModifierStat) entry.getKey();
        if (!relation.equals(other.relation)) return false;
        if (modifierType != other.modifierType) return false;
        return damageCause == null || Objects.equals(damageCause, other.damageCause);
    }

    private boolean filterByOperatorAndModifierType(Map.Entry<IStat, Long> entry) {
        DamageModifierStat other = (DamageModifierStat) entry.getKey();
        if (!relation.equals(other.relation)) return false;
        if (damageOperator != other.damageOperator) return false;
        if (modifierType != other.modifierType) return false;
        return damageCause == null || Objects.equals(damageCause, other.damageCause);
    }

    private boolean filterByOperatorOnly(Map.Entry<IStat, Long> entry) {
        DamageModifierStat other = (DamageModifierStat) entry.getKey();
        if (!relation.equals(other.relation)) return false;
        if (damageOperator != other.damageOperator) return false;
        return damageCause == null || Objects.equals(damageCause, other.damageCause);
    }

    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        // modifierType + damageOperator + null name/operand -> count that operator/type combination
        if (modifierType != null && damageOperator != null && name == null && damageOperand == null) {
            return getFilteredStat(statContainer, type, period, this::filterByOperatorAndModifierType);
        }
        // modifierType only -> count by modifier type
        if (modifierType != null && damageOperator == null && name == null && damageOperand == null) {
            return getFilteredStat(statContainer, type, period, this::filterByModifierTypeOnly);
        }
        // damageOperator only -> count by operator
        if (damageOperator != null && modifierType == null && name == null && damageOperand == null) {
            return getFilteredStat(statContainer, type, period, this::filterByOperatorOnly);
        }
        // damageOperand null -> count uses for the provided modifier details
        if (damageOperand == null) {
            return getFilteredStat(statContainer, type, period, this::filterSpecificModifierUsage);
        }

        return statContainer.getProperty(type, period, this);
    }

    @Override
    public @NotNull StatValueType getStatValueType() {
        return StatValueType.LONG;
    }

    @Override
    public @NotNull String getStatType() {
        return STAT_TYPE;
    }

    @Override
    public @Nullable JSONObject getJsonData() {
        JSONObject obj = new JSONObject()
                .putOnce("relation", relation.name());
        if (name != null) obj.putOnce("name", name);
        if (damageOperator != null) obj.putOnce("damageOperator", damageOperator.name());
        if (modifierType != null) obj.putOnce("modifierType", modifierType.name());
        if (damageOperand != null) obj.putOnce("damageOperand", damageOperand);
        if (damageCause != null) obj.putOnce("damageCause", damageCause.getName());
        return obj;
    }

    @Override
    public String getSimpleName() {
        StringBuilder stringBuilder = new StringBuilder()
                .append("Damage Modifier")
                .append(" ")
                .append(UtilFormat.cleanString(relation.name()));
        if (name != null) {
            stringBuilder.append(" ")
                    .append(UtilFormat.cleanString(name));
        }
        if (damageOperator != null) {
            stringBuilder.append(" ")
                    .append(UtilFormat.cleanString(damageOperator.name()));
        }
        if (damageOperand != null) {
            stringBuilder.append(" ")
                    .append(UtilFormat.cleanString(String.valueOf(damageOperand)));
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean isSavable() {
        return name != null && damageOperator != null && modifierType != null && damageOperand != null && damageCause != null;
    }

    @Override
    public boolean containsStat(IStat otherStat) {
        if (!(otherStat instanceof DamageModifierStat other)) return false;
        if (relation != other.relation) return false;
        if (name != null && !Objects.equals(name, other.name)) return false;
        if (damageOperator != null && damageOperator != other.damageOperator) return false;
        if (modifierType != null && modifierType != other.modifierType) return false;
        if (damageOperand != null && !Objects.equals(damageOperand, other.damageOperand)) return false;
        return damageCause == null || Objects.equals(damageCause, other.damageCause);
    }

    @Override
    public @NotNull IStat getGenericStat() {
        return DamageModifierStat.builder()
                .relation(relation)
                .name(name)
                .build();
    }

    @Override
    public @NotNull IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data) {
        DamageModifierStat other = fromData(statType, data);
        this.relation = other.relation;
        this.name = other.name;
        this.damageOperator = other.damageOperator;
        this.modifierType = other.modifierType;
        this.damageOperand = other.damageOperand;
        this.damageCause = other.damageCause;
        return this;
    }
}
