package me.mykindos.betterpvp.core.framework;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;

@AllArgsConstructor
@Getter
public class ClansServerType implements ServerType {
    private final String name;
    private final int squadSize;
    private final String serverNamePrefix;
    private final Component displayTitle;
}
