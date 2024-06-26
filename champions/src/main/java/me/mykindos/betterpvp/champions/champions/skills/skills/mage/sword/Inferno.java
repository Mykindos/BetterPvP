package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;


import com.destroystokyo.paper.ParticleBuilder;
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
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Inferno extends ChannelSkill implements InteractSkill, CooldownSkill, EnergyChannelSkill, ThrowableListener, FireSkill, OffensiveSkill, DamageSkill {
    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private List<Item> blazePowders = new ArrayList<>();
    private final HashMap<Player, Shotgun> shotguns = new HashMap<>();

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
                "Charges up to " + getValueString(this::getNumFlames, level) + " flames",
                "",
                "Release to shoot a scorching blast of fire",
                "that ignites anything it hits for " + getValueString(this::getFireDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "Energy: " + getValueString(this::getEnergyPerSecond, level),
        };
    }

    public double getFireDuration(int level) {
        return baseFireDuration + ((level - 1) * fireDurationIncreasePerLevel);
    }


    private float getEnergyPerSecond(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    public int getNumFlames(int level) {
        return baseNumFlames + ((level - 1) * numFlamesIncreasePerLevel);
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    private double getChargePerSecond(int level) {
        return baseCharge + (chargeIncreasePerLevel * (level - 1));
    }
    @Override
    public float getEnergy(int level) {
        return energy;
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
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !charging.containsKey(gamer.getPlayer()) && isHolding(gamer.getPlayer());
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

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        if (hit instanceof ArmorStand) {
            return;
        }

        Item fireItem = throwableItem.getItem();
        if (fireItem != null) {
            fireItem.remove();
        }

        if (thrower instanceof Player damager) {
            int level = getLevel(damager);
            hit.setFireTicks((int) (getFireDuration(level) * 20));

            CustomDamageEvent cde = new CustomDamageEvent(hit, damager, null, DamageCause.CUSTOM, getDamage(level), false, "Inferno");
            cde.setDamageDelay(0);
            UtilDamage.doCustomDamage(cde);
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

            if (isHolding(player) && gamer.isHoldingRightClick() && championsManager.getEnergy().use(player, getName(), getEnergyPerSecond(level) / 20, true)) {
                charge.tick();
                charge.tickSound(player);
                continue;
            }

            iterator.remove();
            shotgun(player, charge, level);
        }
    }

    private void shotgun(Player player, ChargeData chargeData, int level) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s %d<gray>.", getName(), level);

        float chargePercent = Math.min(chargeData.getCharge(), 1.0f);
        int numFlames = 1 + (int) (chargePercent * (getNumFlames(level) - 1));

        Shotgun shotgunInstance = new Shotgun(player, numFlames, 0, 1, System.currentTimeMillis() + 50); // Assuming 1 tick = 50 ms
        shotguns.put(player, shotgunInstance);

        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                true,
                true,
                isCancellable(),
                this::shouldDisplayActionBar);
    }


    @UpdateEvent
    public void onUpdate() {
        long currentTick = System.currentTimeMillis();
        Iterator<Map.Entry<Player, Shotgun>> shotgunIterator = shotguns.entrySet().iterator();

        while (shotgunIterator.hasNext()) {
            Map.Entry<Player, Shotgun> entry = shotgunIterator.next();
            Shotgun shotgun = entry.getValue();

            if (currentTick >= shotgun.getNextShotTick() && shotgun.getFlamesShot() < shotgun.getTotalFlames() && isHolding(shotgun.getPlayer())) {
                Item fire = shotgun.getPlayer().getWorld().dropItem(shotgun.getPlayer().getEyeLocation(), new ItemStack(Material.BLAZE_POWDER));
                championsManager.getThrowables().addThrowable(this, fire, shotgun.getPlayer(), getName(), 2000L, true);
                blazePowders.add(fire);

                fire.teleport(shotgun.getPlayer().getEyeLocation());
                Vector randomVector = new Vector(UtilMath.randDouble(-0.01, 0.01), UtilMath.randDouble(-0.01, 0.01), UtilMath.randDouble(-0.01, 0.1));
                Vector increasedVelocity = shotgun.getPlayer().getLocation().getDirection().add(randomVector).multiply(3);
                fire.setVelocity(increasedVelocity);
                shotgun.getPlayer().getWorld().playSound(shotgun.getPlayer().getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.1F, 1.0F);

                shotgun.setFlamesShot(shotgun.getFlamesShot() + 1);
                shotgun.setNextShotTick(currentTick + shotgun.getDelayBetweenShots());
            }

            if (shotgun.getFlamesShot() >= shotgun.getTotalFlames() || !isHolding(shotgun.getPlayer())) {
                shotgunIterator.remove();
            }
        }

        Iterator<Item> blazePowderIterator = blazePowders.iterator();
        while (blazePowderIterator.hasNext()) {
            Item blazePowder = blazePowderIterator.next();

            if (!blazePowder.isValid()) {
                blazePowderIterator.remove();
                continue;
            }

            Location location = blazePowder.getLocation();

            if (location.getBlock().getType() == Material.WATER) {
                blazePowder.remove();
                blazePowderIterator.remove();
                continue;
            }

            new ParticleBuilder(Particle.FLAME)
                    .extra(0)
                    .location(location)
                    .receivers(60)
                    .spawn();
        }
    }


    @Override
    public void loadSkillConfig() {
        baseFireDuration = getConfig("baseFireDuration", 2.5, Double.class);
        fireDurationIncreasePerLevel = getConfig("fireDurationIncreasePerLevel", 0.0, Double.class);

        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);

        baseCharge = getConfig("baseCharge", 100.0, Double.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 0.0, Double.class);

        baseNumFlames = getConfig("baseNumFlames", 4, Integer.class);
        numFlamesIncreasePerLevel = getConfig("numFlamesIncreasePerLevel", 2, Integer.class);
    }

    @Data
    @AllArgsConstructor
    private static class Shotgun {
        private final Player player;
        private final int totalFlames;
        private int flamesShot;
        private final long delayBetweenShots;
        private long nextShotTick;
    }
}

