package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

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
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilItem;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Overcharge extends ChannelSkill implements Listener, PassiveSkill, DamageSkill, OffensiveSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this, charging);
    private final WeakHashMap<Arrow, Double> bonus = new WeakHashMap<>();
    private final List<Arrow> arrows = new ArrayList<>();


    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseCharge ;
    private double chargeIncreasePerLevel;
    private double baseMaxDamage;
    private double maxDamageIncreasePerLevel;

    @Inject
    public Overcharge(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Overcharge";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Bow to use",
                "",
                "Draw back your bow to charge <val>" + getValueString(this::getChargePerSecond, level, 1, "%", 0) + "</val> per second",
                "",
                "",
                "Deals up to " + getValueString(this::getMaxDamage, level) + " bonus damage."
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

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getMaxDamage(int level) {
        return baseMaxDamage + ((level - 1) * maxDamageIncreasePerLevel);
    }

    private double getChargePerSecond(int level) {
        return baseCharge + (chargeIncreasePerLevel * (level - 1)); // Increment of 10% per level
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
        if (hasSkill(player)) {
            ChargeData overchargeData = charging.get(player);
            if (overchargeData != null) {
                double bonusVal = Math.round((overchargeData.getCharge() * getMaxDamage(getLevel(player))) * 10) / 10.0;
                bonus.put(arrow, bonusVal);
            }
        }
        charging.remove(player);
    }

    @UpdateEvent
    public void createRedDustParticles() {
        bonus.forEach((arrow, bonusDamage) -> {
            if (arrow.isValid() && !arrow.isDead() && !arrow.isOnGround() && bonus.get(arrow) > 0) {

                double baseSize = 0.25;
                double count = (bonus.get(arrow));

                double finalSize = baseSize * count;

                Particle.DustOptions redDust = new Particle.DustOptions(Color.fromRGB(255, 0, 0), (float)finalSize);
                new ParticleBuilder(Particle.REDSTONE)
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
        if (!(event.getDamager() instanceof Player)) return;
        if (bonus.containsKey(arrow)) {
            event.setDamage(event.getDamage() + bonus.get(arrow));
            event.addReason(getName());
        }
    }

    @UpdateEvent
    public void updateOvercharge() {
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
        if(event.getHand() != EquipmentSlot.HAND) return;
        if(event.useItemInHand() == Event.Result.DENY) return;
        if(event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();

        if(!UtilItem.isRanged(player.getInventory().getItemInMainHand())) return;

        int level = getLevel(player);
        if(level > 0) {
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
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        baseCharge = getConfig("baseCharge", 10.0, Double.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 7.5, Double.class);

        baseMaxDamage = getConfig("baseMaxDamage", 2.0, Double.class);
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 1.0, Double.class);
    }
}
