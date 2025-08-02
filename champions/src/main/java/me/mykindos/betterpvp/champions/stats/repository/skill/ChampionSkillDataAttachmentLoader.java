package me.mykindos.betterpvp.champions.stats.repository.skill;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.UUID;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.IAttachmentLoader;
import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ChampionSkillDataAttachmentLoader implements IAttachmentLoader<ChampionSkillDataAttachment> {
    private final BuildManager buildManager;

    @Inject
    public ChampionSkillDataAttachmentLoader(BuildManager buildManager) {
        this.buildManager = buildManager;
    }

    /**
     * Loads an attachment for the given {@link CombatData} object.
     *
     * @param player   The UUID of the player
     * @param data     The combat data
     * @param database The database
     * @return The attachment
     */
    @Override
    public @NotNull ChampionSkillDataAttachment loadAttachment(@NotNull UUID player, @NotNull CombatData data, @NotNull Database database) {
        return new ChampionSkillDataAttachment(buildManager);
    }
}
