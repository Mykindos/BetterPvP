package me.mykindos.betterpvp.progression.profile;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class ProfessionProfile {

    private final UUID gamerUUID;
    private Map<String, ProfessionData> professionDataMap = new HashMap<>();

}
