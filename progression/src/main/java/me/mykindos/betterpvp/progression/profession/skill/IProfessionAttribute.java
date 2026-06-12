package me.mykindos.betterpvp.progression.profession.skill;

import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface IProfessionAttribute {
    String getName();

    String getOperation();

    default String getDescription() {
        return getName();
    }

    /**
     * The localized attribute description as a component, resolved per-viewer. Uses the
     * {@code progression.attribute.<nodeId>.description} key (derived from the {@link NodeId} annotation)
     * when present, else falls back to the raw English {@link #getDescription()}.
     */
    default Component getDescriptionComponent() {
        final NodeId nodeId = getClass().getAnnotation(NodeId.class);
        if (nodeId != null) {
            final String key = "progression.attribute." + nodeId.value() + ".description";
            if (Translations.hasTranslation(key)) {
                return Translations.component(key);
            }
        }
        return Component.text(getDescription());
    }

    double getDisplayValue(double value);

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
