package me.mykindos.betterpvp.core.world.travel;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.mykindos.betterpvp.core.world.travel.guard.CombatTravelGuard;
import me.mykindos.betterpvp.core.world.travel.guard.DepartingTravelGuard;

/**
 * Wiring for travel. Adding a new restriction on where or when a player may travel is a single line here.
 */
public class TravelModule extends AbstractModule {

    @Override
    protected void configure() {
        final Multibinder<TravelGuard> guards = Multibinder.newSetBinder(binder(), TravelGuard.class);
        guards.addBinding().to(CombatTravelGuard.class);
        guards.addBinding().to(DepartingTravelGuard.class);
    }
}
