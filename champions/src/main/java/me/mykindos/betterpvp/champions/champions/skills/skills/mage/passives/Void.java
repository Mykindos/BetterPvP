package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Singleton
@BPvPListener
public class Void extends ActiveToggleSkill implements EnergySkill {

    public int damageReduction;

    public int energyReduction;
    @Inject
    public Void(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Void";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "While in void form, you receive",
                "<effect>Slownesss III</effect>, and take no Knockback",
                "",
                "Reduces incoming damage by <stat>" + damageReduction + "</stat>,",
                "but burns <stat>" + energyReduction + "</stat> of your energy",
                "",
                "Energy / Second: <val>" + getEnergy(level)
        };
    }

    @UpdateEvent(delay = 1000)
    public void audio() {
        for (UUID uuid : active) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2F, 0.5F);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2F, 0.5F);
            }
        }
    }

    @UpdateEvent
    public void update() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                if (!player.hasPotionEffect(PotionEffectType.SLOW)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 2));
                }

                int level = getLevel(player);
                if (level <= 0) {
                    it.remove();
                } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 6, true)) {
                    it.remove();
                }
            } else {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!active.contains(damagee.getUniqueId())) return;

        int level = getLevel(damagee);
        if (level > 0) {
            event.setDamage(event.getDamage() - damageReduction);
            championsManager.getEnergy().degenerateEnergy(damagee, energyReduction * 0.01);

            event.setKnockback(false);
        }
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public float getEnergy(int level) {

        return (float) (energy - ((level - 1) * 0.5));
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            active.remove(player.getUniqueId());
            UtilMessage.simpleMessage(player, "Champions", "Void: <red>Off");
        } else {
            active.add(player.getUniqueId());
            if (championsManager.getEnergy().use(player, "Void", 5, false)) {
                UtilMessage.simpleMessage(player, "Champions", "Void: <green>On");
            }
        }
    }

    public void loadSkillConfig() {
        damageReduction = getConfig("damageReduction", 5, Integer.class);
        energyReduction = getConfig("energyReduction", 20, Integer.class);
    }
}
