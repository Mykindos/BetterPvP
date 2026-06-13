package me.mykindos.betterpvp.core.scene.behavior;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.scene.SceneEntity;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
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
 * Spawns a {@link TextDisplay} that follows a {@link TagAnchor} on a scene entity.
 * The display is repositioned every tick, so moving anchors (entities, model bones)
 * are tracked smoothly. Multiple instances can be stacked on the same entity for
 * layered labels (name + role, etc.).
 *
 * <p>By default the tag anchors 0.2 blocks above the owner entity's bounding box
 * ({@link TagAnchor#aboveEntity}). Pass a custom anchor - e.g. a
 * {@link BoneTagAnchor} - to track something else.
 *
 * <pre>{@code
 * entity.addBehavior(new TagBehavior(entity, new Vector(0, 0.3, 0), d -> {
 *     d.text(Component.text("Jack", NamedTextColor.WHITE, TextDecoration.BOLD));
 *     d.setBillboard(Display.Billboard.CENTER);
 *     d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
 * }));
 * }</pre>
 *
 * <p>The display is automatically removed when the behavior is stopped - both when the entity is
 * removed and when the behavior is explicitly detached via
 * {@link me.mykindos.betterpvp.core.scene.SceneEntity#removeBehavior}.
 *
 * <p>To update the text or style at runtime, hold a reference to the behavior and call
 * {@link #getDisplay()} after the behavior has been started.
 */
@CustomLog
public class TagBehavior implements SceneBehavior {

    private final SceneObject owner;
    private final TagAnchor anchor;
    private final Vector offset;
    private final Consumer<TextDisplay> configurator;

    /**
     * The spawned display. {@code null} until {@link #start()} resolves the anchor successfully,
     * and {@code null} again after {@link #stop()}.
     */
    @Getter @Nullable
    private TextDisplay display;

    /**
     * Creates a tag anchored 0.2 blocks above the owner entity's bounding box.
     *
     * @param owner        The scene entity this tag belongs to.
     * @param offset       World-space offset added to the anchor position every tick (in blocks).
     * @param configurator Called once when the display is spawned - set text, colors, scale,
     *                     billboard, shadow, view range, etc. here.
     */
    public TagBehavior(SceneObject owner, Vector offset, Consumer<TextDisplay> configurator) {
        this(owner, TagAnchor.aboveEntity(owner), offset, configurator);
    }

    /**
     * @param owner        The scene entity this tag belongs to (used for world reference).
     * @param anchor       Supplies the tag's base position every tick.
     * @param offset       World-space offset added to the anchor position every tick (in blocks).
     *                     Pass {@code new Vector(0, 0.3, 0)} to hover 0.3 blocks above the anchor.
     * @param configurator Called once when the display is spawned - set text, colors, scale,
     *                     billboard, shadow, view range, etc. here.
     */
    public TagBehavior(SceneObject owner, TagAnchor anchor, Vector offset, Consumer<TextDisplay> configurator) {
        this.owner = owner;
        this.anchor = anchor;
        this.offset = offset.clone();
        this.configurator = configurator;
    }

    /**
     * Resolves the anchor and spawns the {@link TextDisplay} at its current position.
     * If the anchor cannot be resolved a warning is logged and the behavior becomes a no-op.
     */
    @Override
    public void start() {
        final Location spawnLoc = anchor.resolve();
        if (spawnLoc == null) {
            log.warn("TagBehavior: anchor could not be resolved for scene object #{} - tag will not be shown.",
                    owner.getId()).submit();
            return;
        }

        display = owner.getEntity().getWorld().spawn(spawnLoc.add(offset), TextDisplay.class, d -> {
            d.setPersistent(false);
            d.setTeleportDuration(1); // client-side interpolation for smooth tracking
            configurator.accept(d);
        });
    }

    /**
     * Teleports the display to the anchor's current world position plus the configured offset.
     * Called every server tick. No-op if the display or anchor is unavailable.
     */
    @Override
    public void tick() {
        if (display == null || !display.isValid()) return;
        final Location location = anchor.resolve();
        if (location == null) return;
        display.teleport(location.add(offset));
    }

    /**
     * Removes the display from the world. Called when the behavior is detached from the entity
     * or when the entity itself is removed.
     */
    @Override
    public void stop() {
        if (display != null && !UtilEntity.isRemoved(display)) {
            display.remove();
        }
        display = null;
    }

    /**
     * Convenience factory that adds two {@link TagBehavior}s to {@code entity} in one call:
     * <ol>
     *   <li>A <b>name</b> tag - {@code name} rendered in green, at the default anchor
     *       (0.2 blocks above the entity's bounding box).</li>
     *   <li>A <b>role</b> tag - the caller-supplied {@code role} Component, 0.3 blocks
     *       above the name.</li>
     * </ol>
     *
     * <p>Both tags use a transparent background, center-billboard, and shadowed text so they
     * look consistent regardless of the surrounding environment.
     *
     * <pre>{@code
     * TagBehavior.addNameplate(npc, "Jack", Component.text("Blacksmith", NamedTextColor.GRAY));
     * }</pre>
     *
     * @param entity The scene entity to attach the behaviors to.
     * @param name   Plain name string; displayed as green.
     * @param role   Pre-styled role {@link Component} displayed above the name.
     */
    public static void addNameplate(SceneEntity entity, String name, Component role) {
        addNameplate(entity, TagAnchor.aboveEntity(entity), new Vector(0, 0, 0), new Vector(0, 0.3, 0), name, role);
    }

    /**
     * Anchor-parameterized variant of {@link #addNameplate(SceneEntity, String, Component)}.
     * Adds a name tag at {@code anchor + nameOffset} and a role tag at {@code anchor + roleOffset},
     * both styled identically (transparent background, center-billboard, shadowed text).
     *
     * @param entity     The scene entity to attach the behaviors to.
     * @param anchor     Supplies the base position both tags follow every tick.
     * @param nameOffset Offset of the name tag from the anchor (in blocks).
     * @param roleOffset Offset of the role tag from the anchor (in blocks).
     * @param name       Plain name string; displayed as green.
     * @param role       Pre-styled role {@link Component} displayed above the name.
     */
    public static void addNameplate(SceneEntity entity, TagAnchor anchor, Vector nameOffset, Vector roleOffset,
                                    String name, Component role) {
        entity.addBehavior(new TagBehavior(entity, anchor, nameOffset, d -> {
            d.text(Component.text(name, NamedTextColor.GREEN));
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setPersistent(false);
            d.setSeeThrough(true);
        }));

        entity.addBehavior(new TagBehavior(entity, anchor, roleOffset, d -> {
            d.text(role);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setPersistent(false);
            d.setSeeThrough(true);
        }));
    }
}
