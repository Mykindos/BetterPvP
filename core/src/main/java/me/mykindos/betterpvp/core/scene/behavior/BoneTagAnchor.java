package me.mykindos.betterpvp.core.scene.behavior;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.scene.SceneEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link TagAnchor} that tracks a named bone on a ModelEngine {@link ActiveModel},
 * including animated movement.
 *
 * <pre>{@code
 * entity.addBehavior(new TagBehavior(entity, new BoneTagAnchor(model, "head"),
 *         new Vector(0, 0.3, 0), d -> d.text(Component.text("Jack"))));
 * }</pre>
 *
 * <p>The bone is resolved lazily on first use. If the bone ID does not exist in the model
 * a warning is logged once and the anchor permanently remains unresolvable.
 */
@CustomLog
public class BoneTagAnchor implements TagAnchor {

    private final ActiveModel model;
    private final String boneId;

    @Nullable
    private ModelBone bone;
    private boolean lookedUp;

    /**
     * @param model  The {@link ActiveModel} that owns the target bone.
     * @param boneId Blueprint bone ID to anchor to (e.g. {@code "head"}).
     */
    public BoneTagAnchor(ActiveModel model, String boneId) {
        this.model = model;
        this.boneId = boneId;
    }

    @Override
    @Nullable
    public Location resolve() {
        if (!lookedUp) {
            lookedUp = true;
            bone = model.getBone(boneId).orElse(null);
            if (bone == null) {
                log.warn("BoneTagAnchor: bone '{}' not found in model '{}' - tag will not be shown.",
                        boneId, model.getBlueprint().getName()).submit();
            }
        }
        if (bone == null || model.isRemoved()) {
            return null;
        }
        return bone.getLocation();
    }

    /**
     * Bone-tracked variant of {@link TagBehavior#addNameplate(SceneEntity, String, Component)}:
     * the name tag hovers 0.75 blocks above {@code boneId} and the role tag 1.05 blocks above it,
     * following the bone through animations.
     *
     * <p>Lives here rather than on {@link TagBehavior} so that {@link TagBehavior} carries no
     * ModelEngine references - this class is only loaded when a caller actually uses bones.
     *
     * <pre>{@code
     * BoneTagAnchor.addNameplate(npc, model, "head", "Jack",
     *         Component.text("Blacksmith", NamedTextColor.GRAY));
     * }</pre>
     *
     * @param entity The scene entity to attach the behaviors to.
     * @param model  The {@link ActiveModel} that owns {@code boneId}.
     * @param boneId Blueprint bone ID the tags will track (e.g. {@code "head"}).
     * @param name   Plain name string; displayed as green.
     * @param role   Pre-styled role {@link Component} displayed above the name.
     */
    public static void addNameplate(SceneEntity entity, ActiveModel model, String boneId,
                                    String name, Component role) {
        TagBehavior.addNameplate(entity, new BoneTagAnchor(model, boneId),
                new Vector(0, 0.75, 0), new Vector(0, 1.05, 0), name, role);
    }
}
