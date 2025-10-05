package me.mykindos.betterpvp.core.framework.statusbar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;

@Singleton
@PluginAdapter("Core")
public class StatusBarController {

    private final EntityHealthService healthService;
    private final EnergyHandler energyHandler;

    @Inject
    private StatusBarController(EntityHealthService healthService, EnergyHandler energyHandler) {
        this.healthService = healthService;
        this.energyHandler = energyHandler;
    }

    public void setup(Gamer gamer) {
        gamer.setActionBar(new StatusBar(healthService, energyHandler));
    }

}
