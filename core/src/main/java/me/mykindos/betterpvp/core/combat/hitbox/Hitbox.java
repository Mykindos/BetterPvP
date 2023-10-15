package me.mykindos.betterpvp.core.combat.hitbox;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

public class Hitbox {

    private final Player player;
    private Interaction interaction;

    public Hitbox(Player player, Core core) {
        this.player = player;
        this.interaction = player.getWorld().spawn(player.getLocation(), Interaction.class);
        interaction.getPersistentDataContainer().set(CoreNamespaceKeys.OWNER, CustomDataType.UUID, player.getUniqueId());
        interaction.setInteractionHeight((float) -(player.getHeight()));
        interaction.setInteractionWidth((float) (player.getWidth() + 0.2));
        player.hideEntity(core, interaction);
        relocate();
    }

    public void relocate() {
        checkState();
        interaction.teleport(player.getLocation());
        player.addPassenger(interaction);
    }

    private void checkState() {
        Preconditions.checkNotNull(player, "Player cannot be null");
        Preconditions.checkNotNull(interaction, "Hitbox Interaction cannot be null");
        Preconditions.checkState(player.isValid() && player.isOnline(), "Player is not valid");
    }

    public void remove() {
        interaction.remove();
        interaction = null;
    }
}
