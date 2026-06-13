package me.mykindos.betterpvp.core.quest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A node in the quest stage graph (kind is always {@code stage} for quests). */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestNode {

    private String id;
    private String kind;
    private QuestStageData data = new QuestStageData();
}
