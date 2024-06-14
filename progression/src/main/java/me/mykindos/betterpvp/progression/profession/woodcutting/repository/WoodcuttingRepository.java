package me.mykindos.betterpvp.progression.profession.woodcutting.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

@CustomLog
@Singleton
public class WoodcuttingRepository {
    private final Database database;

    @Inject
    public WoodcuttingRepository(Database database) {
        this.database = database;
    }

    public void saveChoppedLog(UUID playerUUID, Material material, Location location) {
        String query = "INSERT INTO progression_woodcutting (id, Gamer, Material, Location) VALUES (?, ?, ?, ?);";
        Statement statement = new Statement(query,
                new UuidStatementValue(UUID.randomUUID()),
                new UuidStatementValue(playerUUID),
                new StringStatementValue(material.name()),
                new StringStatementValue(location.toString()));

        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Progression.class), () -> database.executeUpdate(statement));
    }
}
