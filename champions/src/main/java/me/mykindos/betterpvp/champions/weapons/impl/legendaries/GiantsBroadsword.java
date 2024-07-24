package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Singleton
@BPvPListener
public class GiantsBroadsword extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private static final String ABILITY_NAME = "Shield";
    private int regenAmplifier;
    private final EnergyHandler energyHandler;
    private final Set<UUID> holdingWeapon = new HashSet<>();
    private final ClientManager clientManager;
    private final EffectManager effectManager;

    @Inject
    public GiantsBroadsword(Champions champions, EnergyHandler energyHandler, ClientManager clientManager, EffectManager effectManager) {
        super(champions, "giants_broadsword");
        this.energyHandler = energyHandler;
        this.clientManager = clientManager;
        this.effectManager = effectManager;
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Forged in the godly mines of Plagieus,", NamedTextColor.WHITE));
        lore.add(Component.text("this sword has endured thousands of", NamedTextColor.WHITE));
        lore.add(Component.text("their tyranny, this airy blade is", NamedTextColor.WHITE));
        lore.add(Component.text("wars. It is sure to grant glorious", NamedTextColor.WHITE));
        lore.add(Component.text("victory in battle.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>with attack", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>" + ABILITY_NAME));
        return lore;
    }


    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
        effectManager.addEffect(player, player, EffectTypes.REGENERATION, "Giants Broadsword", regenAmplifier, -1, true, true,
                (livingEntity) -> {
                    if (livingEntity instanceof Player p) {
                        return p.getInventory().getItemInMainHand().getType() != getMaterial();
                    }
                    return false;
                });

    }

    private void deactivate(Player player) {
        effectManager.removeEffect(player, EffectTypes.REGENERATION, "Giants Broadsword");
    }

    @UpdateEvent
    public void doRegen() {
        if (!enabled) {
            return;
        }
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (player.getInventory().getItemInMainHand().getType() != getMaterial()) {
                iterator.remove();
                deactivate(player);
                continue;
            }

            Gamer gamer = clientManager.search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()) {
                iterator.remove();
                deactivate(player);
                continue;
            }

            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, this, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                iterator.remove();
                deactivate(player);
                continue;
            }

            if (!canUse(player)) {
                iterator.remove();
                deactivate(player);
                continue;
            }

            if (!energyHandler.use(player, ABILITY_NAME, energyPerTick, true)) {
                iterator.remove();
                deactivate(player);
                continue;
            }

            // Particles and sound if they're regenerating
            new SoundEffect(Sound.BLOCK_LAVA_POP, 1f, 2f).play(player.getLocation());
            new ParticleBuilder(Particle.HEART)
                    .location(player.getEyeLocation().add(0, 0.25, 0))
                    .offset(0.5, 0.5, 0.5)
                    .extra(0.2f)
                    .receivers(60)
                    .spawn();


        }

        // Passive particles
        final Iterator<UUID> holders = holdingWeapon.iterator();
        while (holders.hasNext()) {
            Player player = Bukkit.getPlayer(holders.next());
            if (player == null) {
                holders.remove();
                continue;
            }

            if (player.getInventory().getItemInMainHand().getType() != getMaterial()) {
                holders.remove();
                continue;
            }

            if (active.contains(player.getUniqueId())) {
                continue; // Only skip if they're currently using the ability
            }

            // If they are holding the item
            new ParticleBuilder(Particle.ENCHANTED_HIT)
                    .location(player.getLocation().add(0, 1, 0))
                    .extra(0)
                    .offset(0.3f, 0.3f, 0.3f)
                    .count(3)
                    .receivers(60)
                    .spawn();
        }
    }

    @EventHandler
    public void onSwapWeapon(PlayerItemHeldEvent event) {
        if (!enabled) {
            return;
        }
        final Player player = event.getPlayer();
        if (matches(player.getInventory().getItem(event.getNewSlot()))) {
            holdingWeapon.add(player.getUniqueId());
        } else {
            holdingWeapon.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        final boolean remove = active.remove(event.getPlayer().getUniqueId());
        if (remove) {
            deactivate(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(PreDamageEvent event) {
        if (!enabled) {
            return;
        }

        DamageEvent de = event.getDamageEvent();
        if (de.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(de.getDamager() instanceof Player damager)) return;
        if (isHoldingWeapon(damager)) {
            if (this.active.contains(damager.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            de.setDamage(baseDamage);
        }
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }

    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

    @Override
    public boolean useShield(Player player) {
        return active.contains(player.getUniqueId());
    }

    @Override
    public void loadWeaponConfig() {
        regenAmplifier = getConfig("regenAmplifier", 4, Integer.class);
    }
}
