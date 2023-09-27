package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

@BPvPListener
public class SpongeListener implements Listener {

    private final CooldownManager cooldownManager;

    @Inject
    public SpongeListener(CooldownManager cooldownManager){
        this.cooldownManager = cooldownManager;
    }

    /*
     * The code for sponge springs.
     * When a player is standing on a sponge, and right clicks it below them, they get shot up into the air.
     */
    @EventHandler
    public void interactSpring(PlayerInteractEvent event) {

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if(event.getClickedBlock() == null) {
            return;
        }

        if (!event.getClickedBlock().getType().name().contains("SPONGE")) {
            return;
        }

        final Player player = event.getPlayer();

        if (UtilMath.offset(player.getLocation(), event.getClickedBlock().getLocation().add(0.5D, 1.5D, 0.5D)) > 0.6D) {
            return;
        }

        if (cooldownManager.use(player, "Sponge", 0.8, false)) {

            player.setVelocity(new Vector(0.0D, 1.8D, 0.0D));
            player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0, 15);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.SPONGE, 15);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.SPONGE, 15);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.SPONGE, 15);
            event.setCancelled(true);
        }
    }
}
