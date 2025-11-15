package me.mykindos.betterpvp.core.client.stats.impl.dungeons;

import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@NoArgsConstructor
@Getter
public abstract class DungeonStat implements IBuildableStat {

    @NotNull
    @Builder.Default
    protected String dungeonName = "";

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
        StringBuilder stringBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(dungeonName)) {
            stringBuilder.append(dungeonName);
            stringBuilder.append(" ");
        }

        return stringBuilder.append(getSimpleName()).toString();
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return new JSONObject()
                .putOnce("dungeonName", dungeonName);
    }
}
