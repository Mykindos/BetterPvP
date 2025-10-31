package me.mykindos.betterpvp.core.client.stats.impl.champions;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import me.mykindos.betterpvp.core.components.champions.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Builder
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class RoleStat implements IStat {
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
     * @param period
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String period) {
        return statContainer.getProperty(period, getStatName());
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

    public enum Action {
        //todo formatter
        TIME_PLAYED,
        EQUIP
    }
}
