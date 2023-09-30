package me.mykindos.betterpvp.progression.tree.fishing.data;

import com.google.common.collect.ConcurrentHashMultiset;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FishingData extends ProgressionData<Fishing> {

    // We store A LOT more data than this, but we don't want to load it all into memory for everybody
    // We *could*, in the future, load people's data upon request and cache it for some time, so it's
    // not as resource-intensive, but for now, we'll just store the bare minimum.
    @Getter
    @Setter
    private int fishCaught;
    private ConcurrentHashMultiset<Fish> catchesToSave = ConcurrentHashMultiset.create();

    public FishingData() {
        this.fishCaught = 0;
    }

    public void addFish(Fish fish) {
        this.fishCaught++;
        catchesToSave.add(fish);
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database, String databasePrefix) {
        final String stmt = "INSERT INTO " + databasePrefix + "fishing (gamer, type, weight) VALUES (?, ?, ?);";
        List<Statement> statements = new ArrayList<>();
        for (Fish fish : catchesToSave) {
            Statement statement = new Statement(stmt,
                    new StringStatementValue(uuid.toString()),
                    new StringStatementValue(fish.getType().getName()),
                    new IntegerStatementValue(fish.getWeight()));
            statements.add(statement);
        }
        database.executeBatch(statements, true);
        catchesToSave.clear();
    }
}
