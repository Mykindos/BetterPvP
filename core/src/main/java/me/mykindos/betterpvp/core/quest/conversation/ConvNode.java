package me.mykindos.betterpvp.core.quest.conversation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A dialogue node in a conversation graph. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvNode {
    private String id;
    private String kind;
    private ConvNodeData data = new ConvNodeData();
}
