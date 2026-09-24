package me.mykindos.betterpvp.clans.world.camp.upgrade;

import lombok.Data;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** An advance or repair a camp has queued for one of its structures, and who queued it. */
@Data
@NoArgsConstructor
public class QueuedAction {

    private UUID structure;
    /** The structure's type, so it can still be named once the structure is gone. */
    private String type;
    private ConstructionAction action;
    private UUID queuedBy;
    /** When it could not start, or 0 while it is still queued. */
    private long droppedAt;
    /** The members who have been told it could not start. */
    private Set<UUID> told = new HashSet<>();

    public QueuedAction(@NotNull UUID structure, @NotNull String type, @NotNull ConstructionAction action,
                        @NotNull UUID queuedBy) {
        this.structure = structure;
        this.type = type;
        this.action = action;
        this.queuedBy = queuedBy;
    }
}
