package me.mykindos.betterpvp.progression.profession.skill;

import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

public interface IProfessionAttribute {
    String getName();

    String getOperation();

    default String getDescription() {
        return getName();
    }

    default double getDisplayValue(double value) {
        return value;
    }

    static double computeValue(Player player, String profession, IProfessionAttribute attribute, ProfessionProfileManager profileManager) {
        return profileManager.getObject(player.getUniqueId().toString())
                .map(profile -> profile.getProfessionDataMap().get(profession))
                .map(data -> data.getBuild().getNodes().entrySet().stream()
                        .filter(e -> e.getKey() instanceof ProfessionAttributeNode && e.getValue() > 0)
                        .mapToDouble(e -> computeNodeContribution((ProfessionAttributeNode) e.getKey(), e.getValue(), attribute))
                        .sum())
                .orElse(0.0);
    }

    private static double computeNodeContribution(ProfessionAttributeNode node, int level, IProfessionAttribute attribute) {
        return node.getAttributes().entrySet().stream()
                .filter(e -> e.getKey().getClass() == attribute.getClass())
                .mapToDouble(e -> e.getValue().getBaseValue() + Math.max(level - 1, 0) * e.getValue().getPerLevel())
                .sum();
    }
}
