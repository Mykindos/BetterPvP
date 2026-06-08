package me.mykindos.betterpvp.progression.profession.skill;

import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

public interface IProfessionSkill {

    String getName();

    String[] getDescription(int level);

    /**
     * The localized description lines as components, resolved per-viewer. Prefers the multi-line key
     * {@code progression.skill.<nodeId>.desc} (derived from the {@link NodeId} annotation) when present;
     * otherwise deserializes the MiniMessage strings from {@link #getDescription(int)}. Skills with a
     * dynamic value in their description (e.g. a per-level percentage) should override this and build
     * translatable components with the value as an argument.
     */
    default Component[] getDescriptionComponents(int level) {
        final NodeId nodeId = getClass().getAnnotation(NodeId.class);
        if (nodeId != null) {
            final String base = "progression.skill." + nodeId.value() + ".desc";
            if (Translations.hasTranslation(base + ".lines")) {
                return Translations.componentLines(base);
            }
        }
        final String[] lines = getDescription(level);
        final Component[] out = new Component[lines.length];
        for (int i = 0; i < lines.length; i++) {
            out[i] = UtilMessage.deserialize(lines[i]);
        }
        return out;
    }

    Material getIcon();

    default boolean isGlowing() {
        return false;
    }

    default ItemFlag getFlag() {
        return null;
    }
}
