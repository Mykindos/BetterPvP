package me.mykindos.betterpvp.core.scene.npc;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.HumanEntity;

import javax.annotation.Nullable;

@Getter
public class HumanNPC extends NPC {

    private final HumanNMS handle;

    @Setter
    private PlayerListVisibility playerListVisibility = PlayerListVisibility.CUSTOM_NAME_VISIBLE;

    public HumanNPC(HumanNMS handle, NPCFactory factory) {
        super(factory);
        this.handle = handle;
        // The NMS entity is available immediately via the handle, so we can
        // initialize in the constructor rather than waiting for an external call.
        init(handle.getBukkitEntity());
    }

    /**
     * Set the custom name of the NPC.
     *
     * @param component the custom name; {@code null} to fall back to the profile name
     */
    public void customName(@Nullable TextComponent component) {
        handle.getBukkitEntity().customName(component);
    }

    /**
     * Returns the entity as a {@link HumanEntity} for callers that need the narrower type.
     */
    @Override
    public HumanEntity getEntity() {
        return handle.getBukkitEntity();
    }

}
