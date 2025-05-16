package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
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
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Inferno extends ChannelSkill implements InteractSkill, EnergyChannelSkill, ThrowableListener, FireSkill, OffensiveSkill, DamageSkill {
    private final WeakHashMap<LivingEntity, Long> tempImmune = new WeakHashMap<>();

    private double baseFireDuration;

    private double fireDurationIncreasePerLevel;

    private double baseDamage;

    private double damageIncreasePerLevel;

    private double immuneTime;

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
                "You spray fire at high speed igniting",
                "anything it hits for " + getValueString(this::getFireDuration, level) + " seconds",
                "",
                "Energy: " + getValueString(this::getEnergyPerSecond, level),
        };
    }

    public double getFireDuration(int level) {
        return baseFireDuration + ((level - 1) * fireDurationIncreasePerLevel);
    }


    private float getEnergyPerSecond(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    @Override
    public float getEnergy(int level) {
        return energy;
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
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
        active.add(player.getUniqueId());
    }

    @UpdateEvent(delay = 125)
    public void updateImmunes() {
        tempImmune.entrySet().removeIf(entry -> UtilTime.elapsed(entry.getValue(), (long) (immuneTime * 1000L)));
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {

        if (hit instanceof ArmorStand) {
            return;
        }
        if (hit instanceof Player damager) {
            int level = getLevel(damager);

            if (!throwableItem.getImmunes().contains(hit)) {
                if (tempImmune.containsKey(hit)) return;
                CustomDamageEvent cde = new CustomDamageEvent(hit, damager, null, DamageCause.FIRE, getDamage(level), false, "Inferno");
                cde.setDamageDelay(0);
                if(!Objects.requireNonNull(UtilDamage.doCustomDamage(cde)).isCancelled()) {
                    if (hit.getFireTicks() <= 0) {
                        hit.setFireTicks((int) (getFireDuration(level) * 20));
                    }
                    throwableItem.getImmunes().add(hit);
                    tempImmune.put(hit, System.currentTimeMillis());
                }
            }
        }

    }

    @UpdateEvent
    public void doInferno() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            Player cur = Bukkit.getPlayer(iterator.next());
            if (cur == null) {
                iterator.remove();
                continue;
            }

            Gamer gamer = championsManager.getClientManager().search().online(cur).getGamer();
            if (!isHolding(cur) || !gamer.isHoldingRightClick()) {
                iterator.remove();
                continue;
            }

            int level = getLevel(cur);
            if (level <= 0) {
                iterator.remove();
            }
            if (!championsManager.getEnergy().use(cur, getName(), getEnergy(level) / 20, true)) {
                iterator.remove();
            } else {
                Item fire = cur.getWorld().dropItem(cur.getEyeLocation(), new ItemStack(Material.BLAZE_POWDER));
                ThrowableItem throwableItem = new ThrowableItem(this, fire, cur, getName(), 5000L);
                throwableItem.setCanHitFriendlies(false);
                throwableItem.setRemoveInWater(true);
                championsManager.getThrowables().addThrowable(throwableItem);
                fire.teleport(cur.getEyeLocation());
                Vector velocity = cur.getLocation().getDirection().add(new Vector(UtilMath.randDouble(-0.1, 0.1), UtilMath.randDouble(-0.15, 0.15), UtilMath.randDouble(-0.1, 0.1))).multiply(1.5);
                fire.setVelocity(velocity);
                cur.getWorld().playSound(cur.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.1F, 1.0F);
            }
        }
    }


    @Override
    public void loadSkillConfig() {
        baseFireDuration = getConfig("baseFireDuration", 2.5, Double.class);
        fireDurationIncreasePerLevel = getConfig("fireDurationIncreasePerLevel", 0.0, Double.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        immuneTime = getConfig("immuneTime", 0.45, Double.class);

    }

}

