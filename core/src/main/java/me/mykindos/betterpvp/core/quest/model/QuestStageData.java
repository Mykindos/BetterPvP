package me.mykindos.betterpvp.core.quest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** The {@code data} payload of a quest stage node. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestStageData {

    private String title = "";
    private List<PrimitiveData> objectives = new ArrayList<>();
    private List<PrimitiveData> actions = new ArrayList<>();
}
