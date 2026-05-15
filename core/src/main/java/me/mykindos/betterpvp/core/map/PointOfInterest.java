package me.mykindos.betterpvp.core.map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.map.MapCursor;

@Getter
@RequiredArgsConstructor
public class PointOfInterest {

    private final Location location;
    private final String name;
    private final MapCursor.Type type;
}