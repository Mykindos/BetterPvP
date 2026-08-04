package me.mykindos.betterpvp.clans.world.discovery;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.model.ActiveModel;
import lombok.CustomLog;
import lombok.Value;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.clans.world.ship.BerthKey;
import me.mykindos.betterpvp.clans.world.ship.ShipHelms;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.shader.ScreenEffectService;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.prop.InteractiveProp;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The two controls flanking each sailing ship's helm: putting them there, resolving clicks on them, and turning what
 * the crew is holding into the one number {@link ShipDynamics#advance} wants.
 * <p>
 * The controls cannot be spawned when the expedition opens. Helm props are chunk-managed, and materialization is
 * deferred by a tick because binding a ModelEngine model inside the caller's own tick races ModelEngine — so straight
 * after the limbo world's content loads there is a helm marker but no model to bolt anything to. Attachment is
 * therefore attempted on each expedition tick until the helm resolves, and given up on loudly if it never does.
 */
@Singleton
@CustomLog
public class SteeringService {

    /** How long a helm gets to appear before its absence is worth reporting. */
    private static final long ATTACH_TIMEOUT_MILLIS = 5_000L;

    /** How often one refused crewman is told the control is taken. */
    private static final long REFUSAL_THROTTLE_MILLIS = 1_500L;

    /** Blocks out along the helm's beam that each control is mounted. */
    private static final double CONTROL_REACH = 1.1;

    /** Below the titles the expedition itself sends: a gauge should never hide a departure or an arrival. */
    private static final int GAUGE_TITLE_PRIORITY = 50;

    private static final int REFUSAL_ACTION_BAR_PRIORITY = 400;

    /** One control and the expedition it belongs to, indexed by the backing entity a player actually clicks. */
    @Value
    private static class ControlHandle {
        UUID captain;
        SteerControl control;
    }

    private final ClansSceneObjectFactory objectFactory;

    /**
     * Deferred: the helms own the wheel a crew clicks to set out, so they reach this service back through the
     * destination the wheel opens. The lookup is only ever wanted mid-voyage, long after anything is constructed.
     */
    private final Provider<ShipHelms> helms;
    private final ShipService shipService;
    private final ClientManager clientManager;
    private final ScreenEffectService screenEffects;

    private final Map<UUID, SteeringControls> byCaptain = new ConcurrentHashMap<>();

    /**
     * Backing entity to control. The listener resolves clicks through this rather than through
     * {@code SceneObjectRegistry.getObject(Entity)}, which is a linear scan over every scene object on the server and
     * would run on every right-click anyone makes at anything.
     */
    private final Map<UUID, ControlHandle> byEntity = new ConcurrentHashMap<>();

    /** When each expedition first went looking for its helm, so a helm that never appears can be reported once. */
    private final Map<UUID, Long> searching = new ConcurrentHashMap<>();

    private final Map<UUID, Long> lastRefusal = new ConcurrentHashMap<>();

    @Inject
    public SteeringService(@NotNull ClansSceneObjectFactory objectFactory, @NotNull Provider<ShipHelms> helms,
                           @NotNull ShipService shipService, @NotNull ClientManager clientManager,
                           @NotNull ScreenEffectService screenEffects) {
        this.objectFactory = objectFactory;
        this.helms = helms;
        this.shipService = shipService;
        this.clientManager = clientManager;
        this.screenEffects = screenEffects;
    }

    /**
     * Attaches the controls if they are not up yet, renders everything the crew sees and hears, and reports what they
     * are asking the ship to do.
     *
     * @param crew the sailors still aboard, the only audience for any of it
     * @return the net steering input to feed {@link ShipDynamics#advance}
     */
    public int tick(@NotNull Expedition expedition, @NotNull List<Player> crew, long now) {
        SteeringControls controls = byCaptain.get(expedition.getCrew().getCaptain());
        if (controls == null) {
            // Wait for somebody to actually be aboard. Mounting the controls into an empty world hands them to no
            // viewer, and the crew arrives to a wheel flanked by two things they can click but cannot see.
            if (crew.isEmpty()) {
                return 0;
            }

            controls = attach(expedition, now);
            if (controls == null) {
                return 0;
            }
        }

        for (SteerSide side : SteerSide.values()) {
            controls.control(side).ensureModel();
        }

        final String world = BerthKey.worldOf(controls.getHelmKey());
        final String berth = BerthKey.berthOf(controls.getHelmKey());
        final ShipHelms wheels = helms.get();
        final InteractiveProp helm = wheels.helm(world, berth).orElse(null);
        final ActiveModel helmModel = wheels.helmModel(world, berth).orElse(null);
        final Location helmAt = helm != null && helm.isInitialized() ? helm.getEntity().getLocation() : null;

        controls.getCues().emit(expedition, controls, helmAt, helmModel, crew, now);
        pushGauge(expedition, controls, now);
        return controls.netInput(now);
    }

    /** Records one interact on a control. */
    public boolean press(@NotNull Player player, @NotNull Entity clicked) {
        final ControlHandle handle = byEntity.get(clicked.getUniqueId());
        if (handle == null) {
            return false;
        }

        final SteeringControls controls = byCaptain.get(handle.getCaptain());
        if (controls == null) {
            return false;
        }

        final SteeringInput.Response response = controls.getInput()
                .press(handle.getControl().getSide(), player.getUniqueId(), System.currentTimeMillis());
        if (response == SteeringInput.Response.HELD_BY_ANOTHER) {
            refuse(player);
        }

        // Cancelled either way: a refused grab is still a grab at a control, not a swing at whatever is behind it.
        return true;
    }

    /** Takes an expedition's controls out of the world. */
    public void release(@NotNull Expedition expedition) {
        final UUID captain = expedition.getCrew().getCaptain();
        searching.remove(captain);

        // The heel is pushed, not held: it lasts until something says otherwise. Nothing else will, so a crew that
        // makes port mid-turn would walk off the gangplank with the horizon still leaning.
        screenEffects.clear(expedition.getCrew().roster());

        final SteeringControls controls = byCaptain.remove(captain);
        if (controls == null) {
            return;
        }

        byEntity.values().removeIf(handle -> handle.getCaptain().equals(captain));
        controls.remove();
    }

    /**
     * Bolts a control either side of the helm, once the helm has a model to be measured against.
     *
     * @return the new pair, or null if the helm is not up yet
     */
    private @Nullable SteeringControls attach(@NotNull Expedition expedition, long now) {
        final UUID captain = expedition.getCrew().getCaptain();
        searching.putIfAbsent(captain, now);

        final World limbo = Bukkit.getWorld(expedition.getLimbo().getWorldName());
        final Berth mooring = limbo == null ? null : shipService.berths(limbo).stream().findFirst().orElse(null);
        final InteractiveProp helm = mooring == null
                ? null
                : helms.get().helm(limbo.getName(), mooring.getId()).orElse(null);

        if (helm == null || !helm.isMaterialized() || helm.getModeledEntity() == null) {
            final Long since = searching.get(captain);
            if (since != null && now - since > ATTACH_TIMEOUT_MILLIS) {
                log.warn("No helm materialized in '{}' - crew {} has nothing to steer with",
                        expedition.getLimbo().getWorldName(), captain).submit();
                searching.put(captain, Long.MAX_VALUE); // reported once; keep trying quietly
            }
            return null;
        }

        final Location home = helm.getEntity().getLocation();
        final ShipFrame beam = ShipFrame.of(home.getX(), home.getZ(), home.getYaw());
        final double outX = beam.rightX() * CONTROL_REACH;
        final double outZ = beam.rightZ() * CONTROL_REACH;

        // The starboard model is a mirror of the port one, so it is mounted facing back down the beam.
        final SteerControl starboard = spawnControl(SteerSide.STARBOARD, new Location(home.getWorld(),
                home.getX() - outX, home.getY() + 1.5, home.getZ() - outZ,
                (float) ShipDynamics.normalise(home.getYaw() + 180.0), 0f), captain);
        final SteerControl port = spawnControl(SteerSide.PORT, new Location(home.getWorld(),
                home.getX() + outX, home.getY() + 1.5, home.getZ() + outZ, home.getYaw(), 0f), captain);

        final SteeringControls controls = new SteeringControls(port, starboard,
                new SteeringInput(expedition.getCrew()::has),
                BerthKey.of(limbo.getName(), mooring.getId()), screenEffects);
        byCaptain.put(captain, controls);
        searching.remove(captain);
        return controls;
    }

    /**
     * Spawns one control eagerly rather than chunk-managed. Anchor-keyed materialization is only correct for
     * stationary scenery: these sit wherever their helm was found, and a body the framework may rebuild from a stale
     * anchor is a control the crew can no longer click.
     */
    private @NotNull SteerControl spawnControl(@NotNull SteerSide side, @NotNull Location at, @NotNull UUID captain) {
        final Entity body = objectFactory.backingEntity(at);
        body.setGravity(false);

        final SteerControl control = objectFactory.spawn(new SteerControl(objectFactory, side), body);
        byEntity.put(body.getUniqueId(), new ControlHandle(captain, control));
        return control;
    }

    /**
     * Re-sends the rudder gauge to whoever has a hand on a control. Titles expire, so this is a push every tick rather
     * than something set once; the bar itself is rendered on demand so a component queued a few ticks ago still shows
     * the wheel where it is now.
     */
    private void pushGauge(@NotNull Expedition expedition, @NotNull SteeringControls controls, long now) {
        for (UUID holder : controls.getInput().holders(now)) {
            final Player player = Bukkit.getPlayer(holder);
            if (player == null) {
                continue;
            }
            gamer(player).ifPresent(gamer -> gamer.getTitleQueue().add(GAUGE_TITLE_PRIORITY,
                    TitleComponent.subtitle(0, 0.4, 0, false,
                            gmr -> RudderGauge.bar(expedition.getDynamics().getRudder()))));
        }
    }

    private void refuse(@NotNull Player player) {
        final long now = System.currentTimeMillis();
        final Long last = lastRefusal.get(player.getUniqueId());
        if (last != null && now - last < REFUSAL_THROTTLE_MILLIS) {
            return;
        }
        lastRefusal.put(player.getUniqueId(), now);

        gamer(player).ifPresent(gamer -> gamer.getActionBar().add(REFUSAL_ACTION_BAR_PRIORITY,
                new TimedComponent(1.5, false,
                        gmr -> Translations.component("clans.discovery.control-taken").color(NamedTextColor.RED))));
    }

    private @NotNull Optional<Gamer> gamer(@NotNull Player player) {
        return Optional.ofNullable(clientManager.search().online(player)).map(client -> client.getGamer());
    }
}
