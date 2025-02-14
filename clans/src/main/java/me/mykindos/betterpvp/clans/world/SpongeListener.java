package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    private final EffectManager effectManager;

    @Inject
    public SpongeListener(CooldownManager cooldownManager, EffectManager effectManager){
        this.cooldownManager = cooldownManager;
        this.effectManager = effectManager;
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

        if (!UtilBlock.isStandingOn(event.getPlayer(), event.getClickedBlock().getType())) {
            return;
        }

        if (cooldownManager.use(player, "Sponge", 0.8, false)) {

            player.setVelocity(new Vector(0.0D, 1.8D, 0.0D));
            player.getWorld().playSound(event.getClickedBlock().getLocation(), Sound.ENTITY_BAT_TAKEOFF, 2f, 1.7f);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.SPONGE, 15);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.SPONGE, 15);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.SPONGE, 15);
            event.setCancelled(true);

            effectManager.addEffect(player, player, EffectTypes.NO_FALL, "Sponge", 14,
                    5000L, true, true, UtilBlock::isGrounded);
        }
    }
}
