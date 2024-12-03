package me.mykindos.betterpvp.progression.profession.farming.repository;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
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

import java.util.UUID;

@CustomLog
@Singleton
public class FarmingRepository {
    private final Database database;
    private final ProfessionProfileManager profileManager;  // will be necessary for leaderboards

    @Inject
    public FarmingRepository(Database database, ProfessionProfileManager profileManager) {
        this.database = database;
        this.profileManager = profileManager;
    }

    /**
     * Whenever a crop is planted, bonemealed, or harvested, the player gains experience.
     * This method saves that interaction into the database
     * @param playerUUID the player's unique id
     * @param material the crop that was interacted with
     * @param location where the interaction happened
     * @param farmingActionType the type of intetraction
     * @param amount will only be 1 for planting/bonemeal; can be multiple for harvesting a crop
     */
    public void saveCropInteraction(UUID playerUUID, Material material, Location location,
                                    FarmingActionType farmingActionType, int amount) {
        String query = "INSERT INTO progression_woodcutting (id, Gamer, Material, Location, ActionType, Amount) VALUES (?, ?, ?, ?, ?, ?);";
        Statement statement = new Statement(query,
                new UuidStatementValue(UUID.randomUUID()),
                new UuidStatementValue(playerUUID),
                new StringStatementValue(material.name()),
                new StringStatementValue(UtilWorld.locationToString(location)),
                new StringStatementValue(farmingActionType.name()),
                new IntegerStatementValue(amount));

        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Progression.class), () -> database.executeUpdate(statement));
    }

}
