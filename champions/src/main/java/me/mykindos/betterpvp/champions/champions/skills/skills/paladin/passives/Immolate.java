package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Iterator;
import java.util.UUID;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
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

@Singleton
@BPvPListener
public class Immolate extends ActiveToggleSkill implements EnergySkill {


    @Inject
    public Immolate(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Immolate";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "Ignite yourself in flaming fury, gaining",
                "<effect>Speed II</effect> and <effect>Fire Resistance</effect>",
                "",
                "You leave a trail of fire, which",
                "burns players that go near it",
                "",
                "Energy / Second: <val>" + getEnergy(level)

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
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                int level = getLevel(player);
                if (level <= 0) {
                    iterator.remove();
                    sendState(player, false);
                } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 5, true)) {
                    iterator.remove();
                    sendState(player, false);
                } else if (championsManager.getEffects().hasEffect(player, EffectType.SILENCE)) {
                    iterator.remove();
                    sendState(player, false);
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 25, 0));
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
            sendState(player, false);
        } else {
            if (championsManager.getEnergy().use(player, getName(), 10, false)) {
                active.add(player.getUniqueId());
                sendState(player, true);
            }

        }
    }

    private void sendState(Player player, boolean state) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "Immolate: %s", state ? "<green>On" : "<red>Off");
    }
}
