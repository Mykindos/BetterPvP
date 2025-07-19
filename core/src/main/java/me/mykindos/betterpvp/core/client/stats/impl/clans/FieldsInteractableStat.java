package me.mykindos.betterpvp.core.client.stats.impl.clans;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.StringBuilderParser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class FieldsInteractableStat implements IBuildableStat {
    public static String PREFIX = "CLANS_FIELD_INTERACTABLE";

    private static StringBuilderParser<FieldsInteractableStatBuilder> parser = new StringBuilderParser<>(
            List.of(
                    FieldsInteractableStat::parsePrefix,
                    FieldsInteractableStat::parseName
            )
    );

    public static FieldsInteractableStat fromString(String string) {
        return parser.parse(FieldsInteractableStat.builder(), string).build();
    }

    @NotNull
    private String name;

    private static FieldsInteractableStatBuilder parsePrefix(FieldsInteractableStatBuilder builder, String input) {
        Preconditions.checkArgument(input.equals(PREFIX));
        return builder;
    }

    private static FieldsInteractableStatBuilder parseName(FieldsInteractableStatBuilder builder, String input) {
        return builder.name(input);
    }

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
        return parser.asString(
                List.of(
                        PREFIX,
                        name
                )
        );
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
        return getStatName().equals(statName);
    }

    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statName the statname
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Override
    public IBuildableStat copyFromStatname(@NotNull String statName) {
        FieldsInteractableStat other = fromString(statName);
        this.name = other.name;
        return this;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
