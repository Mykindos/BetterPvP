package me.mykindos.betterpvp.core.npc.behavior;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Spawns a {@link TextDisplay} anchored to a named bone on a {@link ModeledNPC}.
 * The display is repositioned every tick to follow the bone, including animated movement.
 * Multiple instances can be stacked on the same NPC for layered labels (name + role, etc.).
 *
 * <pre>{@code
 * npc.addBehavior(new BoneTagBehavior(npc, model, "head", new Vector(0, 0.3, 0), d -> {
 *     d.text(Component.text("Jack", NamedTextColor.WHITE, TextDecoration.BOLD));
 *     d.setBillboard(Display.Billboard.CENTER);
 *     d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
 * }));
 *
 * npc.addBehavior(new BoneTagBehavior(npc, model, "head", new Vector(0, 0.0, 0), d -> {
 *     d.text(Component.text("Blacksmith", NamedTextColor.GRAY));
 *     d.setBillboard(Display.Billboard.CENTER);
 *     d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
 * }));
 * }</pre>
 *
 * <p>The display is automatically removed when the behavior is stopped — both when the NPC is
 * removed and when the behavior is explicitly detached via
 * {@link me.mykindos.betterpvp.core.npc.model.NPC#removeBehavior}.
 *
 * <p>To update the text or style at runtime, hold a reference to the behavior and call
 * {@link #getDisplay()} after the behavior has been started.
 */
@CustomLog
public class BoneTagBehavior implements NPCBehavior {

    private final ModeledNPC npc;
    private final ActiveModel model;
    private final String boneId;
    private final Vector offset;
    private final Consumer<TextDisplay> configurator;

    /**
     * The spawned display. {@code null} until {@link #start()} resolves the bone successfully,
     * and {@code null} again after {@link #stop()}.
     */
    @Getter @Nullable
    private TextDisplay display;
    @Nullable
    private ModelBone bone;

    /**
     * @param npc          The NPC this tag belongs to.
     * @param model        The {@link ActiveModel} that owns the target bone.
     * @param boneId       Blueprint bone ID to anchor the display to (e.g. {@code "head"}).
     * @param offset       World-space offset added to the bone position every tick (in blocks).
     *                     Pass {@code new Vector(0, 0.3, 0)} to hover 0.3 blocks above the bone.
     * @param configurator Called once when the display is spawned — set text, colors, scale,
     *                     billboard, shadow, view range, etc. here.
     */
    public BoneTagBehavior(ModeledNPC npc, ActiveModel model, String boneId,
                           Vector offset, Consumer<TextDisplay> configurator) {
        this.npc = npc;
        this.model = model;
        this.boneId = boneId;
        this.offset = offset.clone();
        this.configurator = configurator;
    }

    /**
     * Resolves the bone and spawns the {@link TextDisplay} at its current position.
     * If the bone ID does not exist in the model a warning is logged and the behavior becomes a no-op.
     */
    @Override
    public void start() {
        bone = model.getBone(boneId).orElse(null);
        if (bone == null) {
            log.warn("BoneTagBehavior: bone '{}' not found in model '{}' — tag will not be shown.",
                    boneId, model.getBlueprint().getName()).submit();
            return;
        }

        Location spawnLoc = bone.getLocation().add(offset);
        display = npc.getEntity().getWorld().spawn(spawnLoc, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.setTeleportDuration(1); // client-side interpolation for smooth bone-tracking
            configurator.accept(d);
        });
    }

    /**
     * Teleports the display to the bone's current world position plus the configured offset.
     * Called every server tick. No-op if the display or bone is unavailable, or the model was removed.
     */
    @Override
    public void tick() {
        if (display == null || !display.isValid() || bone == null || model.isRemoved()) return;
        display.teleport(bone.getLocation().add(offset));
    }

    /**
     * Removes the display from the world. Called when the behavior is detached from the NPC
     * or when the NPC itself is removed.
     */
    @Override
    public void stop() {
        if (display != null && display.isValid()) {
            display.remove();
        }
        display = null;
        bone = null;
    }

    /**
     * Convenience factory that adds two {@link BoneTagBehavior}s to {@code npc} in one call:
     * <ol>
     *   <li>A <b>name</b> tag — {@code name} rendered in bold green, anchored at
     *       {@value #NAME_OFFSET_Y} blocks above {@code boneId}.</li>
     *   <li>A <b>role</b> tag — the caller-supplied {@code role} Component, anchored at
     *       {@value #ROLE_OFFSET_Y} blocks above {@code boneId} (i.e. above the name).</li>
     * </ol>
     *
     * <p>Both tags use a transparent background, center-billboard, and shadowed text so they
     * look consistent regardless of the surrounding environment.
     *
     * <pre>{@code
     * BoneTagBehavior.addNameplate(npc, model, "head", "Jack",
     *         Component.text("Blacksmith", NamedTextColor.GRAY));
     * }</pre>
     *
     * @param npc    The NPC to attach the behaviors to.
     * @param model  The {@link ActiveModel} that owns {@code boneId}.
     * @param boneId Blueprint bone ID the tags will track (e.g. {@code "head"}).
     * @param name   Plain name string; displayed as bold green.
     * @param role   Pre-styled role {@link Component} displayed above the name.
     */
    public static void addNameplate(ModeledNPC npc, ActiveModel model, String boneId,
                                    String name, Component role) {
        npc.addBehavior(new BoneTagBehavior(npc, model, boneId, new Vector(0, 0.75, 0), d -> {
            d.text(Component.text(name, NamedTextColor.GREEN));
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setPersistent(false);
            d.setSeeThrough(true);
        }));

        npc.addBehavior(new BoneTagBehavior(npc, model, boneId, new Vector(0, 1.05, 0), d -> {
            d.text(role);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setPersistent(false);
            d.setSeeThrough(true);
        }));
    }
}
