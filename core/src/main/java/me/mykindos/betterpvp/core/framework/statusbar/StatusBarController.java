package me.mykindos.betterpvp.core.framework.statusbar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;

@Singleton
@PluginAdapter("Core")
public class StatusBarController {

    private final StatusBar statusBar;

    @Inject
    private StatusBarController(StatusBar statusBar) {
        this.statusBar = statusBar;
    }

    public void setup(Gamer gamer) {
        gamer.getActionBar().add(Integer.MAX_VALUE, statusBar);
    }

    public void remove(Gamer gamer) {
        gamer.getActionBar().remove(statusBar);
    }
}
