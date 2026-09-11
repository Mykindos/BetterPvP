package me.mykindos.betterpvp.clans.injector;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.DeathFrequencyFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.DeathLocalityFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.DistanceFromSafetyFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.FatigueFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.PlayerDeathFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.factor.RepeatKillerFactor;
import me.mykindos.betterpvp.clans.clans.fatigue.punishment.FatiguePunishment;
import me.mykindos.betterpvp.clans.clans.fatigue.punishment.SlownessPunishment;
import me.mykindos.betterpvp.clans.displayname.ClansDisplayNameProvider;
import me.mykindos.betterpvp.clans.world.island.CrewAllocationPolicy;
import me.mykindos.betterpvp.clans.world.island.InstanceAllocationPolicy;
import me.mykindos.betterpvp.clans.world.island.IslandAllocator;
import me.mykindos.betterpvp.clans.world.island.RoutingIslandAllocator;
import me.mykindos.betterpvp.clans.world.island.RoutingTravelTransport;
import me.mykindos.betterpvp.clans.world.island.TravelTransport;
import me.mykindos.betterpvp.clans.world.travel.TravelGuard;
import me.mykindos.betterpvp.clans.world.travel.guard.AllocatingTravelGuard;
import me.mykindos.betterpvp.clans.world.travel.guard.AlreadyOnIslandTravelGuard;
import me.mykindos.betterpvp.clans.world.travel.guard.CombatTravelGuard;
import me.mykindos.betterpvp.clans.world.travel.guard.DepartingTravelGuard;

public class ClansInjectorModule extends AbstractModule {

    private final Clans plugin;

    public ClansInjectorModule(Clans plugin) {
        this.plugin = plugin;

    }

    @Override
    protected void configure() {
        bind(Clans.class).toInstance(plugin);

        bind(ClansDisplayNameProvider.class);

        // Battle fatigue strategies. Adding/removing a factor or punishment is a
        // single line here — the manager and hold service never name a concrete
        // implementation (Open/Closed).
        final Multibinder<FatigueFactor> factors = Multibinder.newSetBinder(binder(), FatigueFactor.class);
        factors.addBinding().to(RepeatKillerFactor.class);
        factors.addBinding().to(DeathLocalityFactor.class);
        factors.addBinding().to(DeathFrequencyFactor.class);
        factors.addBinding().to(DistanceFromSafetyFactor.class);
        factors.addBinding().to(PlayerDeathFactor.class);

        final Multibinder<FatiguePunishment> punishments = Multibinder.newSetBinder(binder(), FatiguePunishment.class);
        punishments.addBinding().to(SlownessPunishment.class);

        // Travel guards. Adding a new restriction on where/when a player may travel is a single line here.
        final Multibinder<TravelGuard> travelGuards = Multibinder.newSetBinder(binder(), TravelGuard.class);
        travelGuards.addBinding().to(CombatTravelGuard.class);
        travelGuards.addBinding().to(DepartingTravelGuard.class);
        travelGuards.addBinding().to(AlreadyOnIslandTravelGuard.class);
        travelGuards.addBinding().to(AllocatingTravelGuard.class);

        // Discovery island occupancy. Solo for now; swapping to a shared/pooled policy is a single line here.
        // Crews must land in one island together; a solo traveller still gets their own, since nobody aboard is a
        // crewmate of theirs.
        bind(InstanceAllocationPolicy.class).to(CrewAllocationPolicy.class);

        // Where a discovery island instance comes from, and how a player physically gets to one. Both are routed
        // per-template (allocation) or per-handle (delivery) by IslandHostRouter, so hosting can be split across
        // servers on a per-template basis rather than as a single global local/remote switch. The local
        // implementations stay bound as concrete classes so the routers can compose them directly.
        bind(IslandAllocator.class).to(RoutingIslandAllocator.class);
        bind(TravelTransport.class).to(RoutingTravelTransport.class);
    }

}