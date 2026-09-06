package me.mykindos.betterpvp.core.cutscene.effect;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.cutscene.CutsceneSession;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A model hanging over something for the length of one shot - the arrow that says "this is the bit I am talking about".
 * <p>
 * Shown only to the player watching the cutscene, which is the whole reason it is a shot effect rather than a prop: it
 * is a thing said to one person during one moment of one presentation, and it must not be standing in the world for
 * everyone else. The location is a supplier rather than a fixed point so it can follow whatever it is pointing at -
 * an NPC that walks a route, say - and so it is resolved when the shot begins rather than when the cutscene was
 * written.
 * <p>
 * State is keyed by viewer because a cutscene definition may be shared between several players watching at once;
 * holding the entity in a field would let one player's shot ending despawn another's.
 */
public class PointerEffect implements ShotEffect {

    /** Interpolation on the bob. Long enough to glide, short enough that the model tracks a moving target. */
    private static final int BOB_INTERPOLATION_TICKS = 2;

    private final Supplier<Location> target;
    private final ItemStack model;
    private final double height;
    private final double bob;
    private final double spinDegreesPerSecond;

    private final Map<UUID, ItemDisplay> displays = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    /**
     * @param target where to hang it, resolved when the shot begins and again every tick
     * @param model  what to show - any item, so a custom model or a ModelEngine-skinned item both work
     * @param height blocks above {@code target}
     */
    public PointerEffect(@NotNull Supplier<Location> target, @NotNull ItemStack model, double height) {
        this(target, model, height, 0.15, 90);
    }

    public PointerEffect(@NotNull Supplier<Location> target, @NotNull ItemStack model, double height, double bob,
                         double spinDegreesPerSecond) {
        this.target = target;
        this.model = model;
        this.height = height;
        this.bob = bob;
        this.spinDegreesPerSecond = spinDegreesPerSecond;
    }

    @Override
    public void enter(@NotNull CutsceneSession session) {
        final Player viewer = session.getPlayer();
        final Location anchor = target.get();
        if (viewer == null || anchor == null || anchor.getWorld() == null) {
            return;
        }

        final Core core = JavaPlugin.getPlugin(Core.class);
        final ItemDisplay display = anchor.getWorld().spawn(anchor.clone().add(0, height, 0), ItemDisplay.class, spawned -> {
            spawned.setItemStack(model);
            spawned.setPersistent(false);
            spawned.setGravity(false);
            spawned.setInvulnerable(true);
            spawned.setVisibleByDefault(false);
            spawned.setBillboard(Display.Billboard.FIXED);
            spawned.setTeleportDuration(BOB_INTERPOLATION_TICKS);
        });
        viewer.showEntity(core, display);
        displays.put(session.getViewerId(), display);

        final UUID viewerId = session.getViewerId();
        tasks.put(viewerId, new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                final ItemDisplay current = displays.get(viewerId);
                if (current == null || !current.isValid()) {
                    cancel();
                    return;
                }
                ticks += BOB_INTERPOLATION_TICKS;
                final Location now = target.get();
                if (now == null) {
                    return;
                }
                final double lift = height + Math.sin(ticks / 10.0) * bob;
                current.teleport(now.clone().add(0, lift, 0));
                if (spinDegreesPerSecond != 0) {
                    final float angle = (float) Math.toRadians(ticks / 20.0 * spinDegreesPerSecond);
                    final Transformation transformation = current.getTransformation();
                    current.setTransformation(new Transformation(transformation.getTranslation(),
                            new Quaternionf().rotationY(angle), transformation.getScale(),
                            transformation.getRightRotation()));
                }
            }
        }.runTaskTimer(core, BOB_INTERPOLATION_TICKS, BOB_INTERPOLATION_TICKS));
    }

    @Override
    public void exit(@NotNull CutsceneSession session) {
        final BukkitTask task = tasks.remove(session.getViewerId());
        if (task != null) {
            task.cancel();
        }
        final ItemDisplay display = displays.remove(session.getViewerId());
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
}
