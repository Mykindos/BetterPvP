package me.mykindos.betterpvp.core.map.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.map.PointOfInterest;

import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class MapPointOfInterestEvent extends CustomEvent {

    private final List<PointOfInterest> pointsOfInterest = new ArrayList<>();
}