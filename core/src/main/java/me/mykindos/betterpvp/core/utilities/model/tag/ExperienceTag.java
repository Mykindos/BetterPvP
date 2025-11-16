package me.mykindos.betterpvp.core.utilities.model.tag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import org.jetbrains.annotations.NotNull;

public class ExperienceTag implements Modifying {
    @Override
    public Component apply(@NotNull Component current, int depth) {
        if (depth != 0) return Component.empty();
        return Component.text()
                .append(current.colorIfAbsent(TextColor.color(133, 255, 165)))
                .append(MiniMessage.miniMessage().deserialize("<font:nexo:default><white>ꓨ"))
                .build();
    }
}

