package me.mykindos.betterpvp.core.utilities.model.tag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import org.jetbrains.annotations.NotNull;

public class TimeTag implements Modifying {
    @Override
    public Component apply(@NotNull Component current, int depth) {
        if (depth != 0) return Component.empty();
        return Component.text()
                .append(current.colorIfAbsent(TextColor.color(0, 255, 30)))
                .appendSpace()
                .append(MiniMessage.miniMessage().deserialize("<font:nexo:default><white>ꑼ"))
                .build();
    }
}
