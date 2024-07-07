package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;

import static org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED;

@Singleton
@BPvPListener
public class Barrage extends ChannelSkill implements Listener, PassiveSkill, DamageSkill, OffensiveSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this, charging);
    private final List<Arrow> arrows = new ArrayList<>();

    private double baseCharge;
    private double chargeIncreasePerLevel;
    private double arrowDamage;
    private int numArrowsIncreasePerLevel;
    private int numArrows;
    private double arrowDamageIncreasePerLevel;
    private double spread;
    private final Random random = new Random();
    private final double fullChargeVelocity = 3.0;

    @Inject
    public Barrage(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Barrage";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hold right click with a Bow to use",
                "",
                "Draw back your bow to charge <val>" + getValueString(this::getChargePerSecond, level, 1, "%", 0) + "</val> per second",
                "",
                "The more charge, the more arrows you will fire",
                "up to a maximum of " + getValueString(this::getNumArrows, level) + " and they will deal " + getValueString(this::getArrowDamage, level),
                "damage each",
        };
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
    }

    public double getChargePerSecond(int level) {
        return baseCharge + (chargeIncreasePerLevel * (level - 1));
    }

    public double getNumArrows(int level) {
        return numArrows + ((level - 1) * numArrowsIncreasePerLevel);
    }

    public double getArrowDamage(int level){
        return arrowDamage + ((level - 1) * arrowDamageIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        charging.remove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        int level = getLevel(player);
        if (hasSkill(player)) {
            ChargeData overchargeData = charging.get(player);
            if (overchargeData != null) {
                // use quadratic scaling to encourage fully charging barrage and stop low charge arrow spam
                double charge = overchargeData.getCharge();
                int numArrows = (int) Math.floor(Math.pow(charge, 2) * getNumArrows(level));
                Location headLocation = player.getLocation().add(0, player.getEyeHeight(), 0);

                new BukkitRunnable() {
                    int arrowsSpawned = 0;

                    @Override
                    public void run() {
                        if (arrowsSpawned >= numArrows || !player.isOnline()) {
                            this.cancel();
                            return;
                        }
                        Vector direction = player.getLocation().getDirection().normalize().multiply(fullChargeVelocity);

                        double xOffset = (random.nextDouble() - 0.5) * spread;
                        double yOffset = (random.nextDouble() - 0.5) * spread;
                        double zOffset = (random.nextDouble() - 0.5) * spread;
                        Location spawnLocation = headLocation.clone().add(xOffset, yOffset, zOffset);

                        Arrow additionalArrow = player.getWorld().spawn(spawnLocation, Arrow.class);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);

                        additionalArrow.setShooter(player);
                        additionalArrow.setVelocity(direction);
                        additionalArrow.setDamage(getArrowDamage(level));
                        arrows.add(additionalArrow);
                        additionalArrow.setPickupStatus(DISALLOWED);

                        arrowsSpawned++;
                    }
                }.runTaskTimer(champions, 0L, 1L);
            }
        }
        charging.remove(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        int level = getLevel(player);
        if (level <= 0) return;

        if (arrows.contains(arrow)) {
            event.setDamage(getArrowDamage(level));
            event.addReason(getName());
        }
    }

    @UpdateEvent
    public void updateBarrage() {
        final Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final ChargeData data = charging.get(player);
            if (player != null) {
                int level = getLevel(player);

                if (level <= 0) {
                    iterator.remove();
                    continue;
                }

                if (!isHolding(player)) {
                    iterator.remove();
                    continue;
                }

                Material mainhand = player.getInventory().getItemInMainHand().getType();
                if (mainhand == Material.BOW && player.getActiveItem().getType() == Material.AIR) {
                    iterator.remove();
                    continue;
                }

                if (mainhand == Material.CROSSBOW && player.getActiveItem().getType() == Material.AIR) {
                    CrossbowMeta meta = (CrossbowMeta) player.getInventory().getItemInMainHand().getItemMeta();
                    if (!meta.hasChargedProjectiles()) {
                        iterator.remove();
                    }
                    continue;
                }

                if (UtilBlock.isInLiquid(player)) {
                    iterator.remove();
                    continue;
                }

                data.tick();
                data.tickSound(player);
            }
        }

        arrows.removeIf(arrow -> arrow.isOnGround() || !arrow.isValid() || arrow.isInsideVehicle());
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();

        if (!UtilItem.isRanged(player.getInventory().getItemInMainHand())) return;

        int level = getLevel(player);
        if (!UtilInventory.contains(player, Material.ARROW, 1)) {
            return;
        }
        if (level > 0) {
            charging.computeIfAbsent(player, k -> new ChargeData((float) getChargePerSecond(level) / 100));
        }
    }

    @Override
    public boolean isHolding(Player player) {
        return hasSkill(player) && UtilItem.isRanged(player.getInventory().getItemInMainHand());
    }

    @Override
    public boolean displayWhenUsed() {
        return false;
    }

    public void loadSkillConfig() {
        baseCharge = getConfig("baseCharge", 10.0, Double.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 10.0, Double.class);
        arrowDamage = getConfig("arrowDamage", 3.0, Double.class);
        numArrows = getConfig("numArrows", 4, Integer.class);
        numArrowsIncreasePerLevel = getConfig("numArrowsIncreasePerLevel", 3, Integer.class);
        arrowDamageIncreasePerLevel = getConfig("arrowDamageIncreasePerLevel", 0.0, Double.class);
        spread = getConfig("spread", 3.0, Double.class);
    }
}
