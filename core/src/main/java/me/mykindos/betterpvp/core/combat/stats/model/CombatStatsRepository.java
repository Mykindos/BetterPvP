package me.mykindos.betterpvp.core.combat.stats.model;

import java.util.ArrayList;
import java.util.List;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;

public abstract class CombatStatsRepository<T extends CombatData> extends StatsRepository<T> implements ICombatStatsRepository {

    protected final List<IAttachmentLoader<? extends ICombatDataAttachment<? extends CombatData, ? extends Kill>>> attachmentLoaders = new ArrayList<>();

    protected CombatStatsRepository(BPvPPlugin plugin) {
        super(plugin);
    }

    @Override
    public final void addAttachmentLoader(IAttachmentLoader<? extends ICombatDataAttachment<? extends CombatData, ? extends Kill>> attachmentLoader) {
        this.attachmentLoaders.add(attachmentLoader);
    }

}
