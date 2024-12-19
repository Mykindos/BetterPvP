package me.mykindos.betterpvp.progression.profession.woodcutting.repository;

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
public class WoodcuttingRepository {
    private final Database database;
    private final ProfessionProfileManager profileManager;

    @Inject
    public WoodcuttingRepository(Database database, ProfessionProfileManager profileManager) {
        this.database = database;
        this.profileManager = profileManager;
    }

    public void saveChoppedLog(UUID playerUUID, Material material, Location location, int amount) {
        String query = "INSERT INTO progression_woodcutting (id, Gamer, Material, Location, Amount) VALUES (?, ?, ?, ?, ?);";
        Statement statement = new Statement(query,
                new UuidStatementValue(UUID.randomUUID()),
                new UuidStatementValue(playerUUID),
                new StringStatementValue(material.name()),
                new StringStatementValue(UtilWorld.locationToString(location)),
                new IntegerStatementValue(amount));

        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Progression.class), () -> database.executeUpdate(statement));
    }

    /**
     * Gets the total chopped logs for a player given a unique ID
     */
    public Long getTotalChoppedLogsForPlayer(UUID playerUUID) {
        String query = "SELECT SUM(Amount) FROM progression_woodcutting WHERE Gamer = ? ORDER BY SUM(Amount) DESC";
        Statement statement = new Statement(query, new UuidStatementValue(playerUUID));

        AtomicLong atomicLong = new AtomicLong(0L);
        try (CachedRowSet result = database.executeQuery(statement)) {

            if (result.next()) {
                final long amount = result.getLong(1);
                atomicLong.set(amount);
            }
        } catch (SQLException e) {
            log.error("Error fetching woodcutting leaderboard data", e).submit();
        }

        return atomicLong.get();
    }

    /**
     * docs tbd
     */
    public CompletableFuture<HashMap<UUID, Long>> getTopLogsChoppedByCount(double days) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<UUID, Long> leaderboard = new HashMap<>();
            String query = "SELECT Gamer, SUM(Amount) FROM progression_woodcutting WHERE timestamp > NOW() - INTERVAL ? DAY GROUP BY Gamer ORDER BY SUM(Amount) DESC LIMIT 10";
            Statement statement = new Statement(query, new DoubleStatementValue(days));

            try (CachedRowSet result = database.executeQuery(statement)) {

                while (result.next()) {
                    final String gamer = result.getString(1);
                    final long count = result.getLong(2);
                    leaderboard.put(UUID.fromString(gamer), count);
                }
            } catch (SQLException e) {
                log.error("Error fetching woodcutting leaderboard data", e).submit();
            }

            return leaderboard;

        });
    }

}
