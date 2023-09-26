package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import javax.inject.Inject;
import javax.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class Concussion extends PrepareSkill implements CooldownSkill, Listener {

    private double durationPerLevel;

    @Inject
    public Concussion(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Concussion";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to prepare",
                "",
                "Your next hit will <effect>Blind</effect> the target and for ",
                "<val>" + (durationPerLevel * level) + "</val> seconds, and rotate them 180 degrees",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @EventHandler
    public void onDequip(SkillDequipEvent event) {
        if (event.getSkill() == this) {
            active.remove(event.getPlayer().getUniqueId());
        }
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 3);
    }

    @EventHandler
    public void onDamage(CustomDamageEvent e) {
        if (e.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!(e.getDamagee() instanceof Player damagee)) return;
        int level = getLevel(damager);
        if (level <= 0) return;

        if (active.contains(damager.getUniqueId())) {
            e.setReason("Concussion");
            damagee.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (level * durationPerLevel) * 20, 0));
            UtilMessage.simpleMessage(damager, getName(), "You gave <alt>" + damagee.getName() + "</alt> a concussion.");
            UtilMessage.simpleMessage(damagee, getName(), "<alt>" + damager.getName() + "</alt> gave you a concussion.");

            Location loc = damagee.getLocation();
            loc.setYaw(loc.getYaw() + 180);
            damagee.teleport(loc);

            active.remove(damager.getUniqueId());
        }

    }


    @Override
    public boolean canUse(Player player) {
        if (active.contains(player.getUniqueId())) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>" + getName() + "</alt> is already active.");
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        durationPerLevel = getConfig("durationPerLevel", 1.5, Double.class);
    }
}
