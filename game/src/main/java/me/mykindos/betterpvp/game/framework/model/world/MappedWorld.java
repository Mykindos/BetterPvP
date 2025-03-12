package me.mykindos.betterpvp.game.framework.model.world;

import dev.brauw.mapper.MapperPlugin;
import dev.brauw.mapper.export.JsonExportStrategy;
import dev.brauw.mapper.metadata.MapMetadata;
import dev.brauw.mapper.region.Region;
import lombok.Getter;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents a {@link BPvPWorld} with a {@link MapMetadata} and a list of {@link Region}s
 */
@Getter
public class MappedWorld extends BPvPWorld {

    private final MapMetadata metadata;
    private final List<Region> regions;

    public MappedWorld(@NotNull File worldFolder) {
        super(worldFolder);
        this.metadata = MapperPlugin.getInstance().getMetadataManager().loadMetadata(worldFolder);
        final JsonExportStrategy json = (JsonExportStrategy) MapperPlugin.getInstance().getExportManager().getAvailableStrategies().get("json");
        this.regions = json.read(new File(worldFolder, "dataPoints.json"));
    }

    public <T extends Region> Stream<T> findRegion(String name, Class<T> type) {
        return regions.stream()
                .filter(region -> region.getName().equals(name))
                .filter(type::isInstance)
                .map(type::cast);
    }

}
