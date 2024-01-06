package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Inferno extends ChannelSkill implements InteractSkill, EnergySkill {

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
                "Hold right click with a Sword to chennel",
                "",
                "You spray fire at high speed igniting",
                "anything it hits for <stat>" + getFireDuration(level) + "</stat> seconds",
                "",
                "Energy / Second: <val>" + getEnergy(level)
        };
    }

    public double getFireDuration(int level) {
        return baseFireDuration + level * fireDurationIncreasePerLevel;
    }

    public double getDamage(int level) {
        return baseDamage + level * damageIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    @EventHandler
    public void onCollide(ThrowableHitEntityEvent e) {
        if (e.getThrowable().getName().equals(getName())) {
            if (e.getCollision() instanceof ArmorStand) {
                return;
            }
            if (e.getThrowable().getThrower() instanceof Player damager) {
                int level = getLevel(damager);
                if (e.getCollision().getFireTicks() <= 0) {

                    e.getCollision().setFireTicks((int) (getFireDuration(level) * 20));
                }
                if (!e.getThrowable().getImmunes().contains(e.getCollision())) {
                    if (tempImmune.containsKey(e.getCollision())) return;
                    CustomDamageEvent cde = new CustomDamageEvent(e.getCollision(), damager, null, DamageCause.FIRE, getDamage(level), false, "Inferno");
                    cde.setDamageDelay(0);
                    UtilDamage.doCustomDamage(cde);
                    e.getThrowable().getImmunes().add(e.getCollision());
                    tempImmune.put(e.getCollision(), System.currentTimeMillis());
                }
            }
        }
    }


    @UpdateEvent(delay = 125)
    public void updateImmunes() {
        tempImmune.entrySet().removeIf(entry -> UtilTime.elapsed(entry.getValue(), (long) (immuneTime * 1000L)));
    }

    @UpdateEvent
    public void update() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            Player cur = Bukkit.getPlayer(iterator.next());
            if (cur == null) {
                iterator.remove();
                continue;
            }

            Gamer gamer = championsManager.getClientManager().search().online(cur).getGamer();
            if (!gamer.isHoldingRightClick()) {
                iterator.remove();
                continue;
            }

            int level = getLevel(cur);
            if (level <= 0) {
                iterator.remove();
            } else if (!isHolding(cur)) {
                iterator.remove();
            } else if (!championsManager.getEnergy().use(cur, getName(), getEnergy(level) / 2, true)) {
                iterator.remove();
            } else {
                Item fire = cur.getWorld().dropItem(cur.getEyeLocation(), new ItemStack(Material.BLAZE_POWDER));
                championsManager.getThrowables().addThrowable(fire, cur, getName(), 5000L);

                fire.teleport(cur.getEyeLocation());
                fire.setVelocity(cur.getLocation().getDirection().add(new Vector(UtilMath.randDouble(-0.2, 0.2), UtilMath.randDouble(-0.2, 0.3), UtilMath.randDouble(-0.2, 0.2))));
                cur.getWorld().playSound(cur.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.1F, 1.0F);
            }
        }

    }

    @Override
    public float getEnergy(int level) {

        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }


    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
    }

    @Override
    public void loadSkillConfig(){
        baseFireDuration = getConfig("baseFireDuration", 2.5, Double.class);
        fireDurationIncreasePerLevel = getConfig("fireDurationIncreasePerLevel", 0.0, Double.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);

        immuneTime = getConfig("immuneTime", 0.45, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
