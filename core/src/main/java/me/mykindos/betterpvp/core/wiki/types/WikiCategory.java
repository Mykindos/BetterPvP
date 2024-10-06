package me.mykindos.betterpvp.core.wiki.types;

import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

@Getter
public enum WikiCategory {

    GENERAL(null,"General", "<yellow>General</yellow>", java.util.List.of(
            "General Information"
    ), Material.PAPER, 0),
    EFFECTS(WikiCategory.GENERAL, "Effects", "<white>Effects</white>", java.util.List.of(
            "Effects and their Descriptions."
    ), Material.PAPER, 0);

    private final WikiCategory parent;
    private final String name;
    private final String title;
    private final List<String> description;
    private final Material material;
    private final int modelData;

    WikiCategory(WikiCategory parent, String name, String title, List<String> description, Material material, int modelData) {
        this.parent = parent;
        this.name = name;
        this.title = title;
        this.description = description;
        this.material = material;
        this.modelData = modelData;
    }
}
