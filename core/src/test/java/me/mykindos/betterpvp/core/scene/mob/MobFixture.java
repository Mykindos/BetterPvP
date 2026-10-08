package me.mykindos.betterpvp.core.scene.mob;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.ai.MobGoals;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.mob.animation.AnimationProvider;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import me.mykindos.betterpvp.core.scene.mob.sound.MobSound;
import me.mykindos.betterpvp.core.scene.mob.sound.SoundProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.BoundingBox;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * A {@link SceneMob} on a mocked body, world, pathfinder and ModelEngine model. Bodies stand at a shared, movable
 * position. Close it after each test to release the static mocks.
 */
public final class MobFixture implements AutoCloseable {

    public final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
    public final MockedStatic<ModelEngineAPI> modelEngine = mockStatic(ModelEngineAPI.class);
    public final MobGoals goals = mock(MobGoals.class);
    public final World world = mock(World.class);
    public final World otherWorld = mock(World.class);
    public final ModeledEntity modeled = mock(ModeledEntity.class, RETURNS_DEEP_STUBS);
    public final ActiveModel model = mock(ActiveModel.class);
    public final AnimationHandler handler = mock(AnimationHandler.class);
    public final List<Mob> bodies = new ArrayList<>();
    public final List<Player> players = new ArrayList<>();
    public final Map<UUID, Entity> entities = new HashMap<>();

    private final Map<Entity, Location> locations = new HashMap<>();
    private final Set<Entity> modelled = new HashSet<>();
    private Location position;
    private long now = 1_000_000L;

    public Mob body;
    public Pathfinder pathfinder;

    public MobFixture() {
        position = new Location(world, 0.5, 64, 0.5);
        bukkit.when(Bukkit::getMobGoals).thenReturn(goals);
        bukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenAnswer(invocation -> players.stream()
                .filter(player -> player.getUniqueId().equals(invocation.getArgument(0)))
                .findFirst().orElse(null));
        bukkit.when(() -> Bukkit.getEntity(any(UUID.class))).thenAnswer(invocation -> entities.get(invocation.<UUID>getArgument(0)));

        stubNearbyPlayers(world);
        stubNearbyPlayers(otherWorld);

        modelEngine.when(() -> ModelEngineAPI.getModeledEntity(any(Entity.class)))
                .thenAnswer(invocation -> modelled.contains(invocation.<Entity>getArgument(0)) ? modeled : null);
        modelEngine.when(() -> ModelEngineAPI.createModeledEntity(any(Entity.class), any())).thenAnswer(invocation -> {
            modelled.add(invocation.getArgument(0));
            return modeled;
        });
        modelEngine.when(() -> ModelEngineAPI.createActiveModel(anyString())).thenReturn(model);
        when(modeled.getModels()).thenReturn(Map.of("model", model));
        when(model.getAnimationHandler()).thenReturn(handler);
    }

    private void stubNearbyPlayers(World inWorld) {
        when(inWorld.getNearbyPlayers(any(Location.class), anyDouble())).thenAnswer(invocation -> {
            final Location at = invocation.getArgument(0);
            final double radius = invocation.getArgument(1);
            return players.stream()
                    .filter(player -> player.isOnline() && player.getWorld() == at.getWorld())
                    .filter(player -> player.getLocation().distanceSquared(at) <= radius * radius)
                    .toList();
        });
    }

    /** A clock tests move by hand. */
    public LongSupplier clock() {
        return () -> now;
    }

    public void advance(long millis) {
        now += millis;
    }

    public Location at(double x, double y, double z) {
        return new Location(world, x, y, z);
    }

    /** Where every body stands. */
    public Location position() {
        return position.clone();
    }

    public void moveBodyTo(Location location) {
        position = location.clone();
    }

    public Mob newBody() {
        final Mob created = mock(Mob.class);
        final Pathfinder createdPathfinder = mock(Pathfinder.class);
        final UUID uuid = UUID.randomUUID();
        when(created.getPathfinder()).thenReturn(createdPathfinder);
        when(created.getWorld()).thenAnswer(invocation -> position.getWorld());
        when(created.getLocation()).thenAnswer(invocation -> position.clone());
        when(created.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        when(created.getUniqueId()).thenReturn(uuid);
        when(created.getEntityId()).thenReturn(42);
        when(created.isValid()).thenReturn(true);
        when(created.getBoundingBox()).thenAnswer(invocation -> box(position));
        bodies.add(created);
        body = created;
        pathfinder = createdPathfinder;
        return created;
    }

    /** A box 0.6 wide and 2 tall standing on {@code feet}. */
    public static BoundingBox box(Location feet) {
        return new BoundingBox(feet.getX() - 0.3, feet.getY(), feet.getZ() - 0.3,
                feet.getX() + 0.3, feet.getY() + 2, feet.getZ() + 0.3);
    }

    /** An online player standing at {@code location}, who can be moved with {@link #move}. */
    public Player player(Location location) {
        final Player player = mock(Player.class);
        final UUID uuid = UUID.randomUUID();
        locations.put(player, location.clone());
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenAnswer(invocation -> locations.get(player).clone());
        when(player.getEyeLocation()).thenAnswer(invocation -> locations.get(player).clone().add(0, 1.62, 0));
        when(player.getWorld()).thenAnswer(invocation -> locations.get(player).getWorld());
        when(player.getBoundingBox()).thenAnswer(invocation -> box(locations.get(player)));
        when(player.isOnline()).thenReturn(true);
        when(player.isValid()).thenReturn(true);
        players.add(player);
        entities.put(uuid, player);
        return player;
    }

    /** A player who always stands on the body, so the mob counts as watched. */
    public Player watcher() {
        final Player player = player(position);
        when(player.getLocation()).thenAnswer(invocation -> position.clone());
        when(player.getWorld()).thenAnswer(invocation -> position.getWorld());
        return player;
    }

    /** A living entity at {@code location} that can be moved with {@link #move}. */
    public LivingEntity living(Location location) {
        final LivingEntity living = mock(LivingEntity.class);
        final UUID uuid = UUID.randomUUID();
        locations.put(living, location.clone());
        when(living.getUniqueId()).thenReturn(uuid);
        when(living.getLocation()).thenAnswer(invocation -> locations.get(living).clone());
        when(living.getEyeLocation()).thenAnswer(invocation -> locations.get(living).clone().add(0, 1.5, 0));
        when(living.getWorld()).thenAnswer(invocation -> locations.get(living).getWorld());
        when(living.getBoundingBox()).thenAnswer(invocation -> box(locations.get(living)));
        when(living.isValid()).thenReturn(true);
        entities.put(uuid, living);
        return living;
    }

    public void move(Entity entity, Location location) {
        locations.put(entity, location.clone());
    }

    /** A mob with no model, set up and initialized on a fresh body. */
    public TestMob spawn(Consumer<TestMob> components) {
        return spawn(null, components);
    }

    /** A mob initialized on a fresh body, bound to {@code modelId} when it isn't null. */
    public TestMob spawn(String modelId, Consumer<TestMob> components) {
        final TestMob mob = create(modelId, components);
        mob.init(newBody());
        return mob;
    }

    /** A chunk-managed mob anchored at the body position and materialized once. */
    public TestMob spawnChunkManaged(String modelId, Consumer<TestMob> components) {
        final TestMob mob = create(modelId, components);
        mob.configureMaterialization(position(), anchor -> newBody());
        mob.materialize();
        return mob;
    }

    public TestMob create(String modelId, Consumer<TestMob> components) {
        final TestMob mob = new TestMob(components);
        mob.setModelId(modelId);
        return mob;
    }

    /** Ticks {@code mob} {@code times} times. */
    public static void tick(SceneMob mob, int times) {
        for (int i = 0; i < times; i++) {
            mob.tick();
        }
    }

    @Override
    public void close() {
        modelEngine.close();
        bukkit.close();
    }

    /** A mob whose components are supplied by the test. */
    public static final class TestMob extends SceneMob {

        private final Consumer<TestMob> components;
        public int registrations;

        public TestMob(Consumer<TestMob> components) {
            super(mock(SceneObjectFactory.class), EntityType.ZOMBIE, Disposition.HOSTILE);
            this.components = components;
        }

        @Override
        protected void registerComponents() {
            registrations++;
            components.accept(this);
        }

        public void sound(MobSound sound, SoundProvider provider) {
            setSound(sound, provider);
        }

        public void clip(MobAnimation animation, AnimationProvider provider) {
            setAnimation(animation, provider);
        }
    }
}
