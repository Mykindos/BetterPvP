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
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    public void saveChoppedLog(UUID playerUUID, Material material, Location location) {
        String query = "INSERT INTO progression_woodcutting (id, Gamer, Material, Location) VALUES (?, ?, ?, ?);";
        Statement statement = new Statement(query,
                new UuidStatementValue(UUID.randomUUID()),
                new UuidStatementValue(playerUUID),
                new StringStatementValue(material.name()),
                new StringStatementValue(UtilWorld.locationToString(location)));

        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Progression.class), () -> database.executeUpdate(statement));
    }

    /**
     * Gets the total chopped logs for a player given a unique ID
     */
    public Long getTotalChoppedLogsForPlayer(UUID playerUUID) {
        Optional<ProfessionProfile> professionProfileOptional = profileManager.getObject(playerUUID.toString());
        if (professionProfileOptional.isPresent()) {
            ProfessionProfile professionProfile = professionProfileOptional.get();
            return (long) professionProfile.getProfessionDataMap().get("Woodcutting").getProperty("TOTAL_LOGS_CHOPPED").orElse(0L);
        }

        return 0L;
    }

    /**
     * docs tbd
     */
    public CompletableFuture<HashMap<UUID, Long>> getTopLogsChoppedByCount(double days) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<UUID, Long> leaderboard = new HashMap<>();
            Statement statement = new Statement("CALL GetTopLogsChoppedByCount(?, ?)",
                    new DoubleStatementValue(days),
                    new IntegerStatementValue(10));
            database.executeProcedure(statement, -1, result -> {
                try {
                    while (result.next()) {
                        final String gamer = result.getString(1);
                        final long count = result.getLong(2);
                        leaderboard.put(UUID.fromString(gamer), count);
                    }
                } catch (SQLException e) {
                    log.error("Error fetching woodcutting leaderboard data", e).submit();
                }
            });

            return leaderboard;

        });
    }

}
