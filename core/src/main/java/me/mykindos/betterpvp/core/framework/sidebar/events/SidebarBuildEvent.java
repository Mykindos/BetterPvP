package me.mykindos.betterpvp.core.framework.sidebar.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.framework.sidebar.Sidebar;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarType;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;

@EqualsAndHashCode(callSuper = true)
@Data
public class SidebarBuildEvent extends CustomEvent {

    private final Gamer gamer;
    private final Sidebar sidebar;
    private final SidebarComponent.Builder builder;
    private final SidebarType sidebarType;

}
