package me.mykindos.betterpvp.core.combat.stats.model;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;

import java.util.ArrayList;
import java.util.List;

public abstract class CombatStatsRepository<T extends CombatData> extends StatsRepository<T> {

    protected final List<IAttachmentLoader> attachmentLoaders = new ArrayList<>();

    protected CombatStatsRepository(BPvPPlugin plugin) {
        super(plugin);
    }

    public final void addAttachmentLoader(IAttachmentLoader attachmentLoader) {
        this.attachmentLoaders.add(attachmentLoader);
    }

}
