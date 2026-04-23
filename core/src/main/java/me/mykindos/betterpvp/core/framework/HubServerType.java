package me.mykindos.betterpvp.core.framework;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;

@AllArgsConstructor
@Getter
public class HubServerType implements SelectableServerType {
    private final String name;
    private final String serverNamePrefix;
    private final Component displayTitle;
}
