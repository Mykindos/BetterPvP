package me.mykindos.betterpvp.core.scene;

import com.ticxo.modelengine.api.model.ModeledEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Marks a {@link SceneEntity} that is backed by a ModelEngine {@link ModeledEntity}.
 * <p>
 * Implemented by both {@link me.mykindos.betterpvp.core.scene.npc.ModeledNPC} and
 * {@link me.mykindos.betterpvp.core.scene.prop.ModeledProp}, allowing model-aware code such as
 * {@link me.mykindos.betterpvp.core.scene.behavior.BoneTagAnchor} to operate
 * on either without coupling to the NPC hierarchy.
 */
public interface HasModeledEntity {

    /**
     * Returns the ModelEngine entity wrapping the Bukkit entity, or {@code null} if not yet
     * initialized.
     */
    @Nullable
    ModeledEntity getModeledEntity();
}
