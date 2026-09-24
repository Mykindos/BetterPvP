package me.mykindos.betterpvp.core.world.construction;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/** One structure a holding owns: what it is, where it stands, what shape it is in and what is being done to it. */
@Data
@NoArgsConstructor
public class PlacedStructure {

    private UUID id;
    /** A {@link StructureType} id. */
    private String type;
    /** The stage it stands at now. Advancing moves it on only once claimed. */
    @JsonAlias("version")
    private int stage;
    private StructurePosition position;
    private StructureCondition condition;
    private @Nullable Job job;
    /** What its containers hold, by {@link StructureStorage} slot. Null when they are all empty. */
    private @Nullable Map<String, List<String>> storage;
    /** The upgrade picked at each stage, by stage. A stage missing from it has not been picked from. */
    private Map<Integer, String> upgrades = new TreeMap<>();

    public PlacedStructure(@NotNull UUID id, @NotNull String type, @NotNull StructurePosition position,
                           @NotNull StructureCondition condition) {
        this.id = id;
        this.type = type;
        this.position = position;
        this.condition = condition;
    }

    public boolean hasUpgrade(@NotNull String upgrade) {
        return upgrades.containsValue(upgrade);
    }

    /** The upgrade picked at {@code stage}, if one was. */
    public @NotNull Optional<String> upgradeAt(int stage) {
        return Optional.ofNullable(upgrades.get(stage));
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
                case ADVANCE -> StructureStatus.ADVANCING;
                case REPAIR, FIT_UPGRADE -> fromCondition();
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
