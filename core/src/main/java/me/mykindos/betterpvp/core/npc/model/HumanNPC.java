package me.mykindos.betterpvp.core.npc.model;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.HumanEntity;

import javax.annotation.Nullable;

@Getter
public class HumanNPC extends NPC {

    private final HumanNMS handle;

    /**
     * -- SETTER --
     * Set the visibility of the player list for the NPC.
     */
    @Setter
    private PlayerListVisibility playerListVisibility = PlayerListVisibility.CUSTOM_NAME_VISIBLE;

    public HumanNPC(HumanNMS handle, NPCFactory factory) {
        super(null, factory);
        this.handle = handle;
    }

    /**
     * Set the custom name of the NPC.
     *
     * @param component The custom name. Null to remove the custom name and use the profile's name.
     */
    public void customName(@Nullable TextComponent component) {
        handle.getBukkitEntity().customName(component);
    }

    @Override
    public HumanEntity getEntity() {
        return handle.getBukkitEntity();
    }

}
