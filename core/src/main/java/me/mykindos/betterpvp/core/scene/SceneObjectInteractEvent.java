package me.mykindos.betterpvp.core.scene;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

/**
 * Fired when a player right-clicks a registered {@link SceneObject}, regardless of whether the
 * object is backed by a real world entity (resolved via Bukkit interaction events) or rendered
 * purely through packets (e.g. {@link me.mykindos.betterpvp.core.scene.npc.HumanNPC}).
 * <p>
 * This is the single interaction seam: listeners that react to a scene object being clicked —
 * scene {@code act} dispatch, quest-giver triggers — subscribe here instead of binding directly
 * to {@code PlayerInteractEntityEvent}, so packet-only objects participate identically.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class SceneObjectInteractEvent extends CustomEvent {

    private final Player player;
    private final SceneObject sceneObject;

    public SceneObjectInteractEvent(Player player, SceneObject sceneObject) {
        this.player = player;
        this.sceneObject = sceneObject;
    }
}
