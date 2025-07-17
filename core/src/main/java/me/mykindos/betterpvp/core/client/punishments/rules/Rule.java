package me.mykindos.betterpvp.core.client.punishments.rules;

import lombok.CustomLog;
import lombok.Data;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.RevokeType;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
@CustomLog
public class Rule {
    private final String key;
    private final String category;
    private final List<KeyValue<IPunishmentType, Long>> offensePunishment = new ArrayList<>();
    private final List<String> description;
    private final Material material;
    private final int customModelData;

    public Rule(String key, List<String> punishments, String category, List<String> description, Material material, int customModelData) {
        this.key = key;
        this.category = category;
        this.description = description;
        this.material = material;
        this.customModelData = customModelData;
        parsePunishments(punishments);
    }

    private void parsePunishments(List<String> punishments) {
        punishments.forEach(punishment -> {
            String[] splitInfo = punishment.split("\\s", 2);
            IPunishmentType punishmentType = PunishmentTypes.getPunishmentType(splitInfo[0]);
            if (punishmentType == null) {
                log.error("invalid punishment type {} for rule {}", splitInfo[0], this.key).submit();
                return;
            }
            Long duration = punishmentType.hasDuration() ? UtilTime.parseTimeString(splitInfo[1]) : 0;
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

    public ItemView getItemView(@Nullable String punishReason) {
        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder()
                .displayName(Component.text(this.getKey()))
                .material(this.getMaterial())
                .customModelData(this.getCustomModelData())
                .lore(this.getDescription().stream()
                        .map(UtilMessage::deserialize).toList())
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        return itemViewBuilder.build();
    }
}
