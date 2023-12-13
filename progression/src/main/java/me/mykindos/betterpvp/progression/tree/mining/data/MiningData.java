package me.mykindos.betterpvp.progression.tree.mining.data;

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
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class MiningData extends ProgressionData<Mining> {

    private long oresMined = 0;
    @Getter(AccessLevel.NONE)
    private final ConcurrentHashMap<Material, Integer> oresToSave = new ConcurrentHashMap<>();

    public void increaseMinedStat(Block block) {
        oresMined++;
    }

    public void saveOreMined(Block block) {
        oresToSave.compute(block.getType(), (material, integer) -> integer == null ? 1 : integer + 1);
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database) {
        final String stmt = "INSERT INTO progression_mining (Gamer,Material,AmountMined) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE AmountMined = AmountMined + VALUES(AmountMined);";
        List<Statement> statements = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : oresToSave.entrySet()) {
            final Material type = entry.getKey();
            final int amount = entry.getValue();
            Statement statement = new Statement(stmt,
                    new UuidStatementValue(uuid),
                    new StringStatementValue(type.name()),
                    new IntegerStatementValue(amount));
            statements.add(statement);
        }
        database.executeBatch(statements, false);
        oresToSave.clear();
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                UtilMessage.deserialize("Ores Mined: <alt>%,d", oresMined)
        };
    }
}
