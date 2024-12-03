package me.mykindos.betterpvp.progression.profession.farming;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@CustomLog
@Singleton
public class FarmingRepository {
    private final Database database;
    private final ProfessionProfileManager profileManager;

    @Inject
    public WoodcuttingRepository(Database database, ProfessionProfileManager profileManager) {
        this.database = database;
        this.profileManager = profileManager;
    }

    public void saveHarvestedCrop(UUID playerUUID, Material material, Location location, int amount) {
        String query = "INSERT INTO progression_woodcutting (id, Gamer, Material, Location, Amount) VALUES (?, ?, ?, ?, ?);";
        Statement statement = new Statement(query,
                new UuidStatementValue(UUID.randomUUID()),
                new UuidStatementValue(playerUUID),
                new StringStatementValue(material.name()),
                new StringStatementValue(UtilWorld.locationToString(location)),
                new IntegerStatementValue(amount));

        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Progression.class), () -> database.executeUpdate(statement));
    }

}
