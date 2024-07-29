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
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Hookshot extends ChannelSkill implements Listener, PassiveSkill, DamageSkill, OffensiveSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this, charging);
    private final WeakHashMap<Arrow, Double> bonus = new WeakHashMap<>();
    private final List<Arrow> arrows = new ArrayList<>();
    private double baseCharge;
    private double chargeIncreasePerLevel;
    private double pullMultiplierIncreasePerLevel;
    private double pullMultiplier;

    @Inject
    public Hookshot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Hookshot";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Draw back your bow to charge <val>" + getValueString(this::getChargePerSecond, level, 1, "%", 0) + "</val> per second and",
                "release to shoot an arrow with a hook that pulls",
                "its target with a velocity dependant on the charge",
                "",
                "Crouch to shoot an arrow without a hook",
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

    private double getChargePerSecond(int level) {
        return baseCharge + (chargeIncreasePerLevel * (level - 1));
    }
    private double getPullMultiplier(int level){
        return pullMultiplier + ((level - 1) * pullMultiplierIncreasePerLevel);
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

            if(player.isSneaking()) return;

            if (overchargeData != null) {
                bonus.put(arrow, ((double)overchargeData.getCharge()));
            }
        }
        charging.remove(player);
    }

    @UpdateEvent
    public void createRedDustParticles() {
        bonus.forEach((arrow, bonusKnockback) -> {
            if (arrow.isValid() && !arrow.isDead() && !arrow.isOnGround() && bonus.get(arrow) > 0) {

                double baseSize = 2;
                double count = (bonus.get(arrow));

                double finalSize = baseSize * count;

                Particle.DustOptions redDust = new Particle.DustOptions(Color.fromRGB(128, 0, 0), (float)finalSize);
                new ParticleBuilder(Particle.DUST)
                        .location(arrow.getLocation())
                        .count(1)
                        .offset(0.1, 0.1, 0.1)
                        .extra(0)
                        .data(redDust)
                        .receivers(60)
                        .spawn();
            }
        });

        bonus.keySet().removeIf(arrow -> !arrow.isValid() || arrow.isDead() || arrow.isOnGround());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (bonus.containsKey(arrow)) {
            event.setKnockback(false);
            int level = getLevel(player);
            Vector vec = arrow.getVelocity().clone().normalize().multiply(-1); // Invert the direction
            double velocityStrength = (bonus.get(arrow) * getPullMultiplier(level));
            VelocityData velocityData = new VelocityData(vec, velocityStrength, false, 0.0D, (0.4 * bonus.get(arrow)), (0.6 * bonus.get(arrow)), true);
            UtilVelocity.velocity(event.getDamagee(), null, velocityData, VelocityType.CUSTOM);
            event.addReason(getName());
            bonus.remove(arrow);
        }
    }

    @UpdateEvent
    public void updateHookshot() {
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
        return SkillType.PASSIVE_A;
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
        baseCharge = getConfig("baseCharge", 30.0, Double.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 20.0, Double.class);
        pullMultiplier = getConfig("pullMultiplier", 1.5, Double.class);
        pullMultiplierIncreasePerLevel = getConfig("pullMultiplierIncreasePerLevel", 0.0, Double.class);
    }
}
