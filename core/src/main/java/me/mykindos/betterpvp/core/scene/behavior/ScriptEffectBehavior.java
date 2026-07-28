package me.mykindos.betterpvp.core.scene.behavior;

import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.scene.SceneEntity;
import me.mykindos.betterpvp.core.scene.effect.SceneEffect;
import me.mykindos.betterpvp.core.scene.effect.SceneEffectRegistry;
import me.mykindos.betterpvp.core.scene.effect.ScriptSpec;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plays sounds and particles authored as ModelEngine script keyframes, so an animator can add a
 * footstep or a spark without touching Java.
 * <p>
 * Attach it once and every {@code betterpvp:<kind>;<params>} instruction on the model's animations
 * works - see {@link SceneEffectRegistry} for the kinds and their parameters. Instructions this
 * behaviour does not recognise are left alone, so keyframes that drive other systems (a melee strike,
 * say) are unaffected.
 *
 * <pre>{@code
 * npc.addBehavior(new ScriptEffectBehavior(npc, model));
 * // in Blockbench: betterpvp:sound;s=block.gravel.step;v=0.4;b=leg_left
 * }</pre>
 *
 * <h3>Where an effect happens</h3>
 * ModelEngine's script keyframes live on the animation's global timeline - {@code null} is passed
 * where a bone would be - so there is no owning bone to read. An instruction therefore names its own:
 * {@code b|bone=head} anchors to that bone and follows it through the animation, and without one the
 * effect plays at the entity itself. {@code at=x,y,z} nudges it along the world axes and
 * {@code fwd=0.4} along the entity's facing.
 * <p>
 * One instruction can cover several places at once. {@code b=hand_left,hand_right} plays the effect
 * at each, and a trailing {@code *} takes every bone with that prefix - {@code b=fx_*} fires at
 * {@code fx_wheel}, {@code fx_pipe} and any anchor added to the model later, without the keyframe
 * changing. Bone names are unique per model (ModelEngine renames duplicates to their UUID), so this
 * is how a repeated effect is authored. Prefixes are expanded when the instruction is first seen.
 */
@CustomLog
public class ScriptEffectBehavior extends ModelEngineScriptBehavior {

    /** Cached for instructions that are not ours, so an unknown keyframe costs one map lookup. */
    private static final Binding IGNORED = new Binding(origin -> {}, List.of(), new Vector(), 0);

    private final SceneEntity owner;
    private final SceneEffectRegistry registry;
    private final Core plugin;

    /** Parsed instructions by their raw script string; keyframes repeat, the parse should not. */
    private final Map<String, Binding> bindings = new ConcurrentHashMap<>();

    /** Bone names already reported as missing, so a broken anchor is one log line, not one per frame. */
    private final Set<String> missingBones = ConcurrentHashMap.newKeySet();

    public ScriptEffectBehavior(@NotNull SceneEntity owner, @NotNull ActiveModel model) {
        super(model);
        this.owner = owner;
        this.plugin = JavaPlugin.getPlugin(Core.class);
        this.registry = plugin.getInjector().getInstance(SceneEffectRegistry.class);
    }

    @Override
    public void onScript(IAnimationProperty property, String script) {
        final Binding binding = bindings.computeIfAbsent(script, this::bind);
        if (binding == IGNORED) {
            return;
        }

        // Script keyframes fire from ModelEngine's async model tick, and both the origin lookup and
        // the effect itself touch the world.
        UtilServer.runTask(plugin, () -> {
            for (Location origin : origins(binding)) {
                binding.effect.play(origin);
            }
        });
    }

    private Binding bind(String script) {
        final ScriptSpec spec = ScriptSpec.parse(script);
        if (!registry.knows(spec.kind())) {
            // Parameters mean it was meant as an effect; a bare word is somebody else's keyframe.
            if (spec.hasArguments()) {
                log.warn("Script keyframe '{}' names no known effect kind", script).submit();
            }
            return IGNORED;
        }

        final SceneEffect effect = registry.create(spec);
        if (effect == null) {
            return IGNORED;
        }
        return new Binding(effect,
                bones(spec),
                spec.getVector(new Vector(), "at", "pos"),
                spec.getDouble(0, "fwd", "forward"));
    }

    /**
     * Reads the {@code bone} parameter as a comma-separated list, expanding any {@code prefix*} entry
     * against the model's bones now - the model is fully built by the time its first keyframe fires,
     * and expanding once keeps a per-frame effect off the bone map.
     *
     * @return the bones to play at, empty to play at the entity itself
     */
    private List<String> bones(ScriptSpec spec) {
        final String raw = spec.getString("", "b", "bone", "bones");
        if (raw.isBlank()) {
            return List.of();
        }

        final List<String> names = new ArrayList<>();
        for (String token : raw.split(",")) {
            final String name = token.trim();
            if (name.isEmpty()) {
                continue;
            }
            if (name.endsWith("*")) {
                names.addAll(withPrefix(name.substring(0, name.length() - 1)));
            } else {
                names.add(name);
            }
        }
        return List.copyOf(names);
    }

    /** @return every bone on the model whose name starts with {@code prefix}, case-insensitively. */
    private List<String> withPrefix(String prefix) {
        final String lowered = prefix.toLowerCase(Locale.ROOT);
        final List<String> matches = new ArrayList<>();
        for (String bone : getModel().getBones().keySet()) {
            if (bone.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                matches.add(bone);
            }
        }
        if (matches.isEmpty()) {
            log.warn("Script keyframe anchors to '{}*', and model '{}' has no bone with that prefix",
                    prefix, getModel().getBlueprint().getName()).submit();
        }
        return matches;
    }

    /**
     * Resolves where this instruction's effect should play, on the main thread and at the moment it
     * plays - a bone-anchored effect has to track the animation, not the pose it was bound in.
     *
     * @return one origin per anchor, or empty if the entity is gone and nothing can be placed
     */
    private List<Location> origins(Binding binding) {
        final Entity entity = owner.getEntity();
        if (entity == null || !entity.isValid()) {
            return List.of();
        }
        if (binding.bones.isEmpty()) {
            return List.of(displace(entity.getLocation(), binding, entity));
        }

        final List<Location> origins = new ArrayList<>(binding.bones.size());
        for (String name : binding.bones) {
            final ModelBone bone = getModel().getBone(name).orElse(null);
            if (bone == null) {
                if (missingBones.add(name)) {
                    log.warn("Script keyframe anchors to bone '{}', which model '{}' does not have",
                            name, getModel().getBlueprint().getName()).submit();
                }
                continue;
            }
            origins.add(displace(bone.getLocation().clone(), binding, entity));
        }
        return origins;
    }

    /** Applies the instruction's world-axis and facing-relative offsets to one anchor. */
    private Location displace(Location origin, Binding binding, Entity entity) {
        origin.add(binding.offset);
        if (binding.forward != 0) {
            origin.add(entity.getLocation().getDirection().multiply(binding.forward));
        }
        return origin;
    }

    /** One instruction, parsed: what to play and which anchors to play it at. */
    private static final class Binding {

        private final SceneEffect effect;
        private final List<String> bones;
        private final Vector offset;
        private final double forward;

        private Binding(SceneEffect effect, List<String> bones, Vector offset, double forward) {
            this.effect = effect;
            this.bones = bones;
            this.offset = offset;
            this.forward = forward;
        }
    }
}
