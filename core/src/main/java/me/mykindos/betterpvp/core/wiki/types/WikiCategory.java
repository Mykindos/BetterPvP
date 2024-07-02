package me.mykindos.betterpvp.core.wiki.types;

import lombok.Getter;

import java.util.List;

@Getter
public enum WikiCategory {

    GENERAL(null,"GENERAL", "<yellow>General</yellow>", java.util.List.of(
            "General Information"
    )),
    EFFECTS(WikiCategory.GENERAL, "Effects", "<white>Effects</white>", java.util.List.of(
            "Effects and their Descriptions."
    ));

    private WikiCategory parent;
    private final String name;
    private final String title;
    private final List<String> description;

    WikiCategory(WikiCategory parent, String name, String title, List<String> description) {
        this.parent = parent;
        this.name = name;
        this.title = title;
        this.description = description;
    }
}
