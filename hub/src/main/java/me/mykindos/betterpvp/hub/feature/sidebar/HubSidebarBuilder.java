package me.mykindos.betterpvp.hub.feature.sidebar;

import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public interface HubSidebarBuilder {

    Component retrievingComponent = Translations.component("hub.sidebar.retrieving").color(TextColor.color(201, 201, 201));

    void build(SidebarBuildEvent event);

}
