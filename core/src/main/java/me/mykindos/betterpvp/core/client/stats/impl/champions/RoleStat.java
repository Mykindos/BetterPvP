package me.mykindos.betterpvp.core.client.stats.impl.champions;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Builder
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class RoleStat implements IBuildableStat {
    public static final String PREFIX = "CHAMPIONS_ROLE";

    private static StringBuilderParser<RoleStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                    RoleStat::parsePrefix,
                    RoleStat::parseAction,
                    RoleStat::parseRole
            )
    );

    public static RoleStat fromString(String string) {
        return parser.parse(RoleStat.builder(), string).build();
    }

    private static RoleStatBuilder parsePrefix(RoleStatBuilder builder, String input) {
        Preconditions.checkArgument(PREFIX.equals(input));
        return builder;
    }
    private static RoleStatBuilder parseAction(RoleStatBuilder builder, String input) {
        return builder.action(Action.valueOf(input));
    }
    private static RoleStatBuilder parseRole(RoleStatBuilder builder, String input) {
        if (input.equals("null")) {
            return builder.role(null);
        }
        return builder.role(Role.valueOf(input));
    }

    @NotNull
    private Action action;
    @Nullable(value = "When player does not have a role")
    private Role role;

    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String periodKey) {
        return statContainer.getProperty(periodKey, this);
    }

    @Override
    public String getStatName() {
        return parser.asString(List.of(
                PREFIX,
                action.name(),
                role == null ? "null" : role.name()
        ));
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
        return UtilFormat.cleanString(action.name()) + " " + (role == null ? "No Role" : UtilFormat.cleanString(role.name()));
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
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    public boolean containsStat(String statName) {
        return statName.equals(getStatName());
    }

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        return IBuildableStat.super.containsStat(otherStat);
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return this;
    }

    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statName the statname
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public @NotNull IBuildableStat copyFromStatname(@NotNull String statName) {
        final RoleStat other = fromString(statName);
        this.action = other.action;
        this.role = other.role;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    public enum Action {
        //todo formatter
        TIME_PLAYED,
        EQUIP
    }
}
