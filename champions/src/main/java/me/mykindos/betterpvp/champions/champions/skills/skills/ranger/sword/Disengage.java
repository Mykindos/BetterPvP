package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Disengage extends PrepareSkill implements CooldownSkill {

    private final WeakHashMap<Player, Long> disengages = new WeakHashMap<>();

    private double baseSlowDuration;

    @Inject
    public Disengage(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Disengage";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to prepare",
                "",
                "If you are attacked within <stat>1</stat> second",
                "you successfully disengage, leaping backwards",
                "and giving your attacker <effect>Slowness IV</effect> for",
                "<val>" + (baseSlowDuration + level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)};
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {

        return SkillType.SWORD;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!disengages.containsKey(damagee)) return;

        int level = getLevel(damagee);
        if (level > 0) {
            LivingEntity ent = event.getDamager();
            Vector vec = ent.getLocation().getDirection();
            event.setKnockback(false);
            event.setDamage(0);
            UtilVelocity.velocity(damagee, vec, 3D, true, 0.0D, 0.4D, 1.5D, true);
            championsManager.getEffects().addEffect(damagee, EffectType.NOFALL, 3000);
            ent.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) ((baseSlowDuration + level) * 20), 3));
            UtilMessage.message(damagee, getClassType().getName(), "You successfully disengaged");
            disengages.remove(damagee);
        }

    }

    @UpdateEvent(delay = 100)
    public void checkTimers() {
        disengages.entrySet().removeIf(entry -> entry.getValue() - System.currentTimeMillis() <= 0);
    }

    @Override
    public double getCooldown(int level) {

        return cooldown;
    }

    @Override
    public void activate(Player player, int level) {
        disengages.put(player, System.currentTimeMillis() + 1000L);

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseSlowDuration = getConfig("baseSlowDuration", 2.0, Double.class);
    }
}
