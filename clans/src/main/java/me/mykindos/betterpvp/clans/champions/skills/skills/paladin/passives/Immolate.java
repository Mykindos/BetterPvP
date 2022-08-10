package me.mykindos.betterpvp.clans.champions.skills.skills.paladin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.clans.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.clans.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.UUID;

@Singleton
@BPvPListener
public class Immolate extends ActiveToggleSkill implements EnergySkill {


    @Inject
    public Immolate(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Immolate";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop Axe/Sword to Toggle.",
                "",
                "Ignite yourself in flaming fury.",
                "You receive Speed II and",
                "Fire Resistance",
                "",
                "You leave a trail of fire, which",
                "burns players that go near it.",
                "",
                "Energy / Second: " + ChatColor.GREEN + getEnergy(level)

        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @EventHandler
    public void Combust(EntityCombustEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (active.contains(player.getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }


    @UpdateEvent(delay = 1000)
    public void audio() {


        for (UUID uuid : active) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.3F, 0.0F);
            }

        }
    }

    @UpdateEvent(delay = 125)
    public void fire() {
        for (UUID uuid : active) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Item fire = player.getWorld().dropItem(player.getLocation().add(0.0D, 0.5D, 0.0D), new ItemStack(Material.BLAZE_POWDER));
                ThrowableItem throwableItem = new ThrowableItem(fire, player, getName(), 2000L);
                throwableItem.setCollideGround(false);
                championsManager.getThrowables().addThrowable(throwableItem);

                fire.setVelocity(new Vector((Math.random() - 0.5D) / 3.0D, Math.random() / 3.0D, (Math.random() - 0.5D) / 3.0D));


            }
        }
    }

    @EventHandler
    public void onCollide(ThrowableHitEntityEvent e) {
        if (!e.getThrowable().getName().equals(getName())) return;
        if (!(e.getThrowable().getThrower() instanceof Player damager)) return;
        if (e.getCollision().getFireTicks() > 0) return;

        //LogManager.addLog(e.getCollision(), damager, "Immolate", 0);
        e.getCollision().setFireTicks(80);
    }


    @UpdateEvent
    public void checkActive() {

        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player cur = Bukkit.getPlayer(uuid);
            if (cur != null) {
                int level = getLevel(cur);
                if (level <= 0) {
                    iterator.remove();
                    UtilMessage.message(cur, getClassType().getName(), "Immolate: " + ChatColor.RED + "Off");
                } else if (!championsManager.getEnergy().use(cur, getName(), getEnergy(level) / 5, true)) {
                    iterator.remove();
                    UtilMessage.message(cur, getClassType().getName(), "Immolate: " + ChatColor.RED + "Off");
                } else if (championsManager.getEffects().hasEffect(cur, EffectType.SILENCE)) {
                    iterator.remove();
                    UtilMessage.message(cur, getClassType().getName(), "Immolate: " + ChatColor.RED + "Off");
                } else {
                    cur.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 1));
                    cur.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 25, 0));
                }
            } else {
                iterator.remove();
            }
        }

    }


    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }


    @Override
    public float getEnergy(int level) {

        return (float) energy - ((level - 1));
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            active.remove(player.getUniqueId());
            UtilMessage.message(player, getClassType().getName(), "Immolate: " + ChatColor.RED + "Off");
        } else {
            if (championsManager.getEnergy().use(player, getName(), 10, false)) {
                active.add(player.getUniqueId());
                UtilMessage.message(player, getClassType().getName(), "Immolate: " + ChatColor.GREEN + "On");
            }

        }
    }
}
