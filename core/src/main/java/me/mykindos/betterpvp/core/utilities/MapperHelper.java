package me.mykindos.betterpvp.core.utilities;

import dev.brauw.mapper.MapperPlugin;
import dev.brauw.mapper.export.JsonExportStrategy;
import dev.brauw.mapper.export.model.RegionCollection;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class MapperHelper {

    private MapperHelper() {
    }

    public static RegionCollection getRegions(@NotNull World world) {
        final File dataPointsFile = new File(world.getWorldFolder(), "dataPoints.json");
        final JsonExportStrategy loadStrategy = (JsonExportStrategy) MapperPlugin.getInstance()
                .getExportManager()
                .getAvailableStrategies()
                .get("json");
        return loadStrategy.read(dataPointsFile);
    }
}
