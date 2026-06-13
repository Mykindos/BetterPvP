package me.mykindos.betterpvp.core.quest.cinematic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** A cinematic track: a kind (camera/subtitle/sound/action) and its keyframes. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinTrack {
    private String id;
    private String kind;
    private String label = "";
    private List<CinKeyframe> keyframes = new ArrayList<>();
}
