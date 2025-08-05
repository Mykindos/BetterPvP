package me.mykindos.betterpvp.core.combat.stats.model;

public interface ICombatStatsRepository {
    void addAttachmentLoader(IAttachmentLoader<? extends ICombatDataAttachment<? extends CombatData, ? extends Kill>> attachmentLoader);
}
