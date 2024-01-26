package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Inferno extends ChannelSkill implements InteractSkill, CooldownSkill {
    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this,
            charging,
            gamer -> true);
    private double baseCharge;
    private double chargeIncreasePerLevel;
    private double baseFireDuration;
    private double fireDurationIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private int baseNumFlames;
    private int numFlamesIncreasePerLevel;

    @Inject
    public Inferno(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Inferno";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "Charges up to <val>" + getNumFlames(level) + "</val> flames",
                "",
                "Fire a scorching blast of fire that ignites ",
                "anything it hits for <stat>" + getFireDuration(level) + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getFireDuration(int level) {
        return baseFireDuration + level * fireDurationIncreasePerLevel;
    }

    public int getNumFlames(int level){
        return baseNumFlames + level * numFlamesIncreasePerLevel;
    }

    public double getDamage(int level) {
        return baseDamage + level * damageIncreasePerLevel;
    }

    private double getChargePerSecond(int level) {
        return baseCharge + (chargeIncreasePerLevel * (level - 1));
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1) * cooldownDecreasePerLevel;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
    }
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public void activate(Player player, int level) {
        final ChargeData chargeData = new ChargeData((float) getChargePerSecond(level) / 100);
        charging.put(player, chargeData);
    }

    @EventHandler
    public void onCollide(ThrowableHitEntityEvent e) {
        if (e.getThrowable().getName().equals(getName())) {
            if (e.getCollision() instanceof ArmorStand) {
                return;
            }
            if (e.getThrowable().getThrower() instanceof Player damager) {
                int level = getLevel(damager);
                e.getCollision().setFireTicks((int) (getFireDuration(level) * 20));
                CustomDamageEvent cde = new CustomDamageEvent(e.getCollision(), damager, null, DamageCause.FIRE, getDamage(level), false, "Inferno");
                cde.setDamageDelay(0);
                UtilDamage.doCustomDamage(cde);
                e.getThrowable().getImmunes().add(e.getCollision());
            }
        }
    }

    @UpdateEvent
    public void updateCharge() {
        Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            ChargeData charge = charging.get(player);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            if (isHolding(player) && gamer.isHoldingRightClick()) {
                charge.tick();
                charge.tickSound(player);
                continue;
            }

            iterator.remove();
            shotgun(player, charge, level);
        }
    }

    private void shotgun(Player player, ChargeData chargeData, int level) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s<gray>.", getName());

        float chargePercent = Math.min(chargeData.getCharge(), 1.0f);
        int numFlames = 1 + (int) (chargePercent * (getNumFlames(level) - 1));

        Location headLocation = player.getEyeLocation();
        Vector direction = headLocation.getDirection();

        for (int i = 0; i < numFlames; i++) {
            Item fire = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.BLAZE_POWDER));
            championsManager.getThrowables().addThrowable(fire, player, getName(), 2000L);

            fire.teleport(player.getEyeLocation());
            fire.setVelocity(player.getLocation().getDirection().add(new Vector(UtilMath.randDouble(-0.2, 0.2), UtilMath.randDouble(-0.2, 0.3), UtilMath.randDouble(-0.2, 0.2))));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.1F, 1.0F);
        }

        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                true,
                true,
                isCancellable(),
                this::shouldDisplayActionBar);
    }

    @Override
    public void loadSkillConfig(){
        baseFireDuration = getConfig("baseFireDuration", 2.5, Double.class);
        fireDurationIncreasePerLevel = getConfig("fireDurationIncreasePerLevel", 0.0, Double.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);

        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 0.0, Double.class);
        baseCharge = getConfig("baseCharge", 100.0, Double.class);
        baseNumFlames = getConfig("baseNumFlames", 6, Integer.class);
        numFlamesIncreasePerLevel = getConfig("numFlamesIncreasePerLevel", 2, Integer.class);
    }

    @Data
    @AllArgsConstructor
    private static class Shotgun {
        private final ChargeData data;
        private final int level;
    }
}

