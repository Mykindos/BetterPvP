package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
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
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Singleton
@BPvPListener
public class AlligatorsTooth extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {
    private double bonusDamage;
    private double velocityStrength;

    private int soundUpdateCounter;

    private final EnergyHandler energyHandler;
    private final ClientManager clientManager;

    @Inject
    public AlligatorsTooth(Champions champions, EnergyHandler energyHandler, ClientManager clientManager) {
        super(champions, "alligators_tooth");
        this.energyHandler = energyHandler;
        this.clientManager = clientManager;
    }

    @Override
    public List<Component> getLore(ItemStack item) {
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
        if (!enabled) {
            return;
        }
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

            VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), velocityStrength, false, 0, 0.11, 1.0, true);
            UtilVelocity.velocity(player, null, velocityData);
            
            if (++soundUpdateCounter % 3 == 0)
            {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 0.5F, 1.25F);
                player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.LAPIS_BLOCK);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(PreCustomDamageEvent event) {
        if (!enabled) {
            return;
        }

        CustomDamageEvent cde = event.getCustomDamageEvent();

        if (cde.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(cde.getDamager() instanceof Player player)) return;
        if (!isHoldingWeapon(player)) return;

        cde.setDamage(baseDamage);
        if (cde.getDamager().getLocation().getBlock().isLiquid()) {
            cde.setDamage(cde.getDamage() + bonusDamage);
        }

    }

    @UpdateEvent(delay = 1000)
    public void onOxygendDrain() {
        if (!enabled) {
            return;
        }
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

    @Override
    public void loadWeaponConfig() {
        bonusDamage = getConfig("bonusDamage", 4.0, Double.class);
        velocityStrength = getConfig("velocityStrength", 1.0, Double.class);
    }
}
