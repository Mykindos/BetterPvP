package me.mykindos.betterpvp.core.quest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** The {@code data} payload of a stage transition: guard conditions. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestEdgeData {

    private List<PrimitiveData> conditions = new ArrayList<>();
}
