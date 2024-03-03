package me.mykindos.betterpvp.core.utilities.model;

import lombok.Getter;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.stream.Stream;

@Getter
public final class MultiRayTraceResult {

    private final RayTraceResult[] results;

    public MultiRayTraceResult(RayTraceResult[] results) {
        this.results = results;
    }

    public Stream<RayTraceResult> stream() {
        return Arrays.stream(results);
    }

}
