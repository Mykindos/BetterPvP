package me.mykindos.betterpvp.progression.tree.fishing.data;

import com.google.common.collect.ConcurrentHashMultiset;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public final class FishingData extends ProgressionData<Fishing> {

    // We store A LOT more data than this, but we don't want to load it all into memory for everybody
    // We *could*, in the future, load people's data upon request and cache it for some time, so it's
    // not as resource-intensive, but for now, we'll just store the bare minimum.
    private long fishCaught;
    private long weightCaught;
    @Setter(AccessLevel.NONE)
    private Fish biggestFish;
    @Getter(AccessLevel.NONE)
    private final ConcurrentHashMultiset<Fish> catchesToSave = ConcurrentHashMultiset.create();

    public void addFish(Fish fish) {
        this.fishCaught++;
        this.weightCaught += fish.getWeight();
        catchesToSave.add(fish);
        if (biggestFish == null || biggestFish.getWeight() < fish.getWeight()) {
            biggestFish = fish;
        }
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database) {
        final String stmt = "INSERT INTO progression_fishing (Gamer, Type, Weight) VALUES (?, ?, ?);";
        List<Statement> statements = new ArrayList<>();
        for (Fish fish : catchesToSave) {
            Statement statement = new Statement(stmt,
                    new UuidStatementValue(uuid),
                    new StringStatementValue(fish.getType().getName()),
                    new IntegerStatementValue(fish.getWeight()));
            statements.add(statement);
        }
        database.executeBatch(statements, false);
        catchesToSave.clear();
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                UtilMessage.deserialize("Total Fish Caught: <alt>%,d", fishCaught),
                UtilMessage.deserialize("Total Weight Caught: <alt>%,d", weightCaught),
        };
    }
}
