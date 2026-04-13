package me.mykindos.betterpvp.core.scene.loader;

import dev.brauw.mapper.region.Region;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * A {@link SceneObjectLoader} that sources its spawn positions from Mapper data-points.
 * <p>
 * Subclasses supply their region collection via {@link #getRegions()} and then use
 * {@link #getDataPoint(String, Class)} / {@link #getDataPoints(String, Class)} inside
 * their {@link #load()} implementation. The helpers throw descriptive exceptions when
 * a required data-point is missing, making misconfigured maps obvious at load time.
 */
public abstract class MapperSceneLoader extends SceneObjectLoader {

    /**
     * Returns the full set of regions available to this loader.
     * Typically delegates to a world-holder object (e.g. {@code HubWorld.getRegions()}).
     */
    @NotNull
    protected abstract Collection<Region> getRegions();

    /**
     * Returns the first region with the given name that is an instance of {@code type}.
     *
     * @param name the data-point name (case-insensitive)
     * @param type the expected region type
     * @param <T>  the region subtype
     * @return the matching region
     * @throws IllegalStateException if no matching region is found
     */
    @NotNull
    protected <T extends Region> T getDataPoint(@NotNull String name, @NotNull Class<T> type) {
        return getRegions().stream()
                .filter(r -> r.getName().equalsIgnoreCase(name))
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        getClass().getSimpleName() + ": no data-point found with name '" + name + "' of type " + type.getSimpleName()));
    }

    /**
     * Returns all regions with the given name that are instances of {@code type}.
     *
     * @param name the data-point name (case-insensitive)
     * @param type the expected region type
     * @param <T>  the region subtype
     * @return an unmodifiable list of matching regions (may be empty)
     */
    @NotNull
    protected <T extends Region> List<T> getDataPoints(@NotNull String name, @NotNull Class<T> type) {
        return getRegions().stream()
                .filter(r -> r.getName().equalsIgnoreCase(name))
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

}
