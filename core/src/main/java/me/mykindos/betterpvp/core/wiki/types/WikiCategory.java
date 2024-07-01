package me.mykindos.betterpvp.core.wiki.types;

import java.util.List;

public enum WikiCategory {

    EFFECTS("Effects", "<white>Effects</white>", List.of(
            "Effects and their Descriptions."
    ));

    final String name;
    final String title;
    final List<String> description;

    WikiCategory(String name, String title, List<String> description) {
        this.name = name;
        this.title = title;
        this.description = description;
    }
}
