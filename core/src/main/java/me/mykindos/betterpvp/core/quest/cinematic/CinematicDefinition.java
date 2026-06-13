package me.mykindos.betterpvp.core.quest.cinematic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** A published cinematic: a duration and a set of timeline tracks. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinematicDefinition {
    private String id;
    private String name;
    private int durationTicks = 200;
    private List<CinTrack> tracks = new ArrayList<>();
}
