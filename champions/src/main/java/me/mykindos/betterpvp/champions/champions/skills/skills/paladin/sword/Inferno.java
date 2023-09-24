package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.WeakHashMap;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
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

@Singleton
@BPvPListener
public class Inferno extends ChannelSkill implements InteractSkill, EnergySkill {

    private final WeakHashMap<LivingEntity, Long> tempImmune = new WeakHashMap<>();

    private int fireTicks;

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
                "You spray fire at high speed,",
                "igniting anything it hits for <stat>" + ((double)fireTicks/ (double)20) + "</stat> seconds.",
                "",
                "Energy / Second: <val>" + getEnergy(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
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

                if (e.getCollision().getFireTicks() <= 0) {

                    e.getCollision().setFireTicks(fireTicks);
                }
                if (!e.getThrowable().getImmunes().contains(e.getCollision())) {
                    if (tempImmune.containsKey(e.getCollision())) return;
                    CustomDamageEvent cde = new CustomDamageEvent(e.getCollision(), damager, null, DamageCause.FIRE, 1, false, "Inferno");
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
        tempImmune.entrySet().removeIf(entry -> UtilTime.elapsed(entry.getValue(), 450));
    }

    @UpdateEvent
    public void Update() {
        for (Player cur : Bukkit.getOnlinePlayers()) {
            if (!active.contains(cur.getUniqueId())) continue;

            if (cur.isHandRaised()) {
                int level = getLevel(cur);
                if (level <= 0) {
                    active.remove(cur.getUniqueId());
                } else if (!UtilPlayer.isHoldingItem(cur, SkillWeapons.SWORDS)) {
                    active.remove(cur.getUniqueId());
                } else if (!championsManager.getEnergy().use(cur, getName(), getEnergy(level) / 2, true)) {
                    active.remove(cur.getUniqueId());
                } else {
                    Item fire = cur.getWorld().dropItem(cur.getEyeLocation(), new ItemStack(Material.BLAZE_POWDER));
                    championsManager.getThrowables().addThrowable(fire, cur, getName(), 3000L);


                    fire.teleport(cur.getEyeLocation());
                    fire.setVelocity(cur.getLocation().getDirection().add(new Vector(UtilMath.randDouble(-0.2, 0.2), UtilMath.randDouble(-0.2, 0.3), UtilMath.randDouble(-0.2, 0.2))));
                    cur.getWorld().playSound(cur.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.1F, 1.0F);
                }
            } else {
                active.remove(cur.getUniqueId());
            }
        }

    }

    @Override
    public float getEnergy(int level) {

        return energy - ((level - 1));
    }


    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
    }

    @Override
    public void loadSkillConfig(){
        fireTicks = getConfig("fireTicks", 50, Integer.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
