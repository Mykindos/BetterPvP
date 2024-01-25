package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Singleton
@BPvPListener
public class AlligatorsTooth extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    @Inject
    @Config(path = "weapons.alligators-tooth.energy-per-tick", defaultValue = "1.0", configName = "weapons/legendaries")
    private double energyPerTick;

    @Inject
    @Config(path = "weapons.alligators-tooth.initial-energy-cost", defaultValue = "10.0", configName = "weapons/legendaries")
    private double initialEnergyCost;

    @Inject
    @Config(path = "weapons.alligators-tooth.base-damage", defaultValue = "8.0", configName = "weapons/legendaries")
    private double baseDamage;

    @Inject
    @Config(path = "weapons.alligators-tooth.bonus-damage", defaultValue = "4.0", configName = "weapons/legendaries")
    private double bonusDamage;

    @Inject
    @Config(path = "weapons.alligators-tooth.strength", defaultValue = "1.0", configName = "weapons/legendaries")
    private double velocityStrength;

    private final EnergyHandler energyHandler;
    private final ClientManager clientManager;

    @Inject
    public AlligatorsTooth(EnergyHandler energyHandler, ClientManager clientManager) {
        super("alligators_tooth");
        this.energyHandler = energyHandler;
        this.clientManager = clientManager;
    }

    @Override
    public List<Component> getLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("This deadly tooth was stolen from", NamedTextColor.WHITE));
        lore.add(Component.text("a nest of reptilian beasts long ago.", NamedTextColor.WHITE));
        lore.add(Component.text("Legends say that the holder is granted", NamedTextColor.WHITE));
        lore.add(Component.text("the underwater agility of an Alligator.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f</yellow> Damage with attack on land", baseDamage));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f</yellow> Damage with attack in water", (baseDamage + bonusDamage)));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>Gator Stroke"));
        return lore;
    }

    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }

    @UpdateEvent
    public void doAlligatorsTooth() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            final Player player = Bukkit.getPlayer(iterator.next());
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final Gamer gamer = clientManager.search().online(player).getGamer();
            if (!gamer.isHoldingRightClick() || player.getInventory().getItemInMainHand().getType() != getMaterial()) {
                iterator.remove();
                continue;
            }

            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, this, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                continue;
            }

            if (!canUse(player)) {
                iterator.remove();
                continue;
            }

            if (!energyHandler.use(player, "Gator Stroke", energyPerTick, true)) {
                iterator.remove();
                continue;
            }

            UtilVelocity.velocity(player, velocityStrength, 0.11D, 1.0D, true);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.LAPIS_BLOCK);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, 0.8F, 1.5F);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isHoldingWeapon(player)) return;

        event.setDamage(baseDamage);
        if (event.getDamager().getLocation().getBlock().isLiquid()) {
            event.setDamage(event.getDamage() + bonusDamage);
        }

    }

    @UpdateEvent(delay = 1000)
    public void onOxygendDrain() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!UtilBlock.isInWater(player)) continue;
            if (!isHoldingWeapon(player)) continue;
            player.setRemainingAir(player.getMaximumAir());
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (!UtilBlock.isInWater(player)) {
            UtilMessage.simpleMessage(player, "Gator Stroke", "You can only use this ability in water!");
            return false;
        }
        return true;
    }


    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

}
