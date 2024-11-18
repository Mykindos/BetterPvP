package me.mykindos.betterpvp.core.client.punishments.rules;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.RevokeType;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.UtilTime;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class Rule {
    private final String key;
    private final List<KeyValue<IPunishmentType, Long>> offensePunishment = new ArrayList<>();
    private final String description;

    public Rule(String key, List<String> punishments, String description) {
        this.key = key;
        this.description = description;
        parsePunishments(punishments);
    }

    private void parsePunishments(List<String> punishments) {
        punishments.forEach(punishment -> {
            String[] splitInfo = punishment.split("\\s", 2);
            IPunishmentType punishmentType = PunishmentTypes.getPunishmentType(splitInfo[0]);
            Long duration = UtilTime.parseTimeString(splitInfo[1]);
            offensePunishment.add(new KeyValue<>(punishmentType, duration));
        });
    }

    public KeyValue<IPunishmentType, Long> getPunishmentForClient(Client target) {
        int numPreviousPunishments = (int) target.getPunishments().stream()
                .filter(punishment -> punishment.getRevokeType() != RevokeType.INCORRECT)
                .filter(punishment -> punishment.getRule().equals(this))
                .count();
        int level = numPreviousPunishments;
        if (level >= offensePunishment.size()) {
            level = offensePunishment.size() - 1;
        }
        return offensePunishment.get(level);
    }
}
