package me.mykindos.betterpvp.core.world.construction;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** One structure a holding owns: what it is, where it stands, what shape it is in and what is being done to it. */
@Data
@NoArgsConstructor
public class PlacedStructure {

    private UUID id;
    /** A {@link StructureType} id. */
    private String type;
    /** The version it stands at now. An upgrade under way moves it on only once claimed. */
    private int version;
    private StructurePosition position;
    private StructureCondition condition;
    private @Nullable Job job;

    public PlacedStructure(@NotNull UUID id, @NotNull String type, @NotNull StructurePosition position,
                           @NotNull StructureCondition condition) {
        this.id = id;
        this.type = type;
        this.position = position;
        this.condition = condition;
    }

    public @NotNull StructureStatus status(long now) {
        if (condition == StructureCondition.NOT_PLACED) {
            return StructureStatus.NOT_PLACED;
        }
        if (job != null) {
            if (job.isHeld()) {
                return StructureStatus.PAUSED;
            }
            if (job.isDone(now)) {
                return StructureStatus.READY_TO_CLAIM;
            }
            return switch (job.getKind()) {
                case BUILD, MOVE -> StructureStatus.UNDER_CONSTRUCTION;
                case UPGRADE -> StructureStatus.UPGRADING;
                case REPAIR -> fromCondition();
            };
        }
        return fromCondition();
    }

    private @NotNull StructureStatus fromCondition() {
        return switch (condition) {
            case UNDER_CONSTRUCTION -> StructureStatus.UNDER_CONSTRUCTION;
            case ACTIVE -> StructureStatus.ACTIVE;
            case DISABLED -> StructureStatus.DISABLED;
            case NEEDS_REPAIR -> StructureStatus.NEEDS_REPAIR;
            case NOT_PLACED -> StructureStatus.NOT_PLACED;
        };
    }
}
