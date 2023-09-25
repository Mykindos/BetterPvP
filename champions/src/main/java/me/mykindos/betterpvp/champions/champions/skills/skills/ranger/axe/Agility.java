package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
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
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class Agility extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final Set<UUID> active = new HashSet<>();

    private double baseDuration;

    private double damageReduction;

    @Inject
    public Agility(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Agility";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sprint with great agility, gaining",
                "<effect>Speed II</effect> for <val>" + (baseDuration + level) + "</val> seconds and ",
                "<stat>" + (damageReduction * 100) + "%</stat> reduced damage while active",
                "",
                "Agility ends if you interact",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {

        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1));
    }

    @EventHandler
    public void endOnInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        if (active.contains(player.getUniqueId())) {
            active.remove(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        if (active.contains(damagee.getUniqueId())) {
            event.setDamage(event.getDamage() * (1 - damageReduction));
            event.setKnockback(false);
            UtilMessage.message(damager, getClassType().getName(), damagee.getName() + " is using " + getName());
            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.5F, 2.0F);
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            active.remove(damager.getUniqueId());
        }

    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        active.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }


    @Override
    public void activate(Player player, int level) {
        if (!active.contains(player.getUniqueId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) ((baseDuration + level) * 20), 1));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 0.5F);
            active.add(player.getUniqueId());
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        damageReduction = getConfig("damageReduction", 0.60, Double.class);
    }
}
