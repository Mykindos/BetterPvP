package me.mykindos.betterpvp.core.quest.conversation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;

import java.util.ArrayList;
import java.util.List;

/**
 * An actionable response under a dialogue line. Its identity is its
 * {@link #outcome} (what happens to the conversation), not a graph edge.
 * {@link #conditions} gate visibility; {@link #actions} are orthogonal side
 * effects that fire when chosen, regardless of the outcome.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvResponse {
    private String id;
    private String label = "Continue";
    /** Saved per player when chosen; usable later via a "has flag" condition. */
    private String flag = "";
    private List<PrimitiveData> conditions = new ArrayList<>();
    private List<PrimitiveData> actions = new ArrayList<>();
    private ConvOutcome outcome = new ConvOutcome();
}
