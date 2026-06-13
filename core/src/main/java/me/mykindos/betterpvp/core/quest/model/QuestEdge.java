package me.mykindos.betterpvp.core.quest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A directed transition between two stage nodes (source → target). */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestEdge {

    private String id;
    private String source;
    private String target;
    private QuestEdgeData data = new QuestEdgeData();
}
