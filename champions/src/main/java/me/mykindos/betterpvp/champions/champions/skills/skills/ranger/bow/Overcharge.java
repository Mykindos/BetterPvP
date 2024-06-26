package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.CrossbowMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Overcharge extends Skill implements InteractSkill, Listener, DamageSkill, OffensiveSkill {

    private final WeakHashMap<Player, OverchargeData> data = new WeakHashMap<>();
    private final WeakHashMap<Arrow, Integer> bonus = new WeakHashMap<>();
    private final List<Arrow> arrows = new ArrayList<>();
    private final Set<UUID> charging = new HashSet<>();
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseDuration;
    private double durationDecreasePerLevel;
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
                "Draw back harder on your bow, giving",
                getValueString(this::getDamage, level) + " bonus damage per " + getValueString(this::getDuration, level) + "</val> seconds",
                "",
                "Maximum Damage: " + getValueString(this::getMaxDamage, level)
        };
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getMaxDamage(int level) {
        return baseMaxDamage + ((level - 1) * maxDamageIncreasePerLevel);
    }

    private double getDuration(int level) {
        return baseDuration - ((level - 1) * durationDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        charging.remove(event.getPlayer().getUniqueId());
    }


    @EventHandler
    public void onPlayerShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        charging.remove(player.getUniqueId());
        if (hasSkill(player)) {
            OverchargeData overchargeData = data.get(player);
            if (overchargeData != null) {
                bonus.put(arrow, overchargeData.getCharge());
                data.remove(player);
            }
        }

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
        Iterator<Map.Entry<Player, OverchargeData>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {
            OverchargeData data = iterator.next().getValue();
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player != null) {
                int level = getLevel(player);
                if (!charging.contains(player.getUniqueId())) {
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

                if (UtilTime.elapsed(data.getLastCharge(), (long) (getDuration(level) * 1000))) {
                    if (data.getCharge() < data.getMaxCharge()) {
                        data.addCharge();
                        UtilMessage.simpleMessage(player, getClassType().getName(), "%s: <yellow>+%d<gray> Bonus Damage", getName(), data.getCharge());
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4F, 1.0F + 0.05F * data.getCharge());
                    }
                }
            }
        }

        arrows.removeIf(arrow -> arrow.isOnGround() || !arrow.isValid() || arrow.isInsideVehicle());
    }


    @Override
    public SkillType getType() {

        return SkillType.BOW;
    }

    @Override
    public void activate(Player player, int level) {
        if (!data.containsKey(player)) {
            data.put(player, new OverchargeData(player.getUniqueId(), (int) getDamage(level), (int) getMaxDamage(level)));
            charging.add(player.getUniqueId());
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean displayWhenUsed() {
        return false;
    }

    @Data
    private static class OverchargeData {
        private final UUID uuid;
        private final int increment;
        private final int maxCharge;

        private int charge;
        private long lastCharge;

        public OverchargeData(UUID uuid, int increment, int maxCharge) {
            this.uuid = uuid;
            this.charge = 0;
            this.lastCharge = System.currentTimeMillis();
            this.increment = increment;
            this.maxCharge = maxCharge;

        }

        public void addCharge() {
            if (getCharge() <= getMaxCharge()) {
                setCharge(getCharge() + getIncrement());
                lastCharge = System.currentTimeMillis();
            }
        }
    }

    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        baseDuration = getConfig("baseDuration", 2.4, Double.class);
        durationDecreasePerLevel = getConfig("durationDecreasePerLevel", 0.4, Double.class);

        baseMaxDamage = getConfig("baseMaxDamage", 2.0, Double.class);
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 1.0, Double.class);
    }
}
