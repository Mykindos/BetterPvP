package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class Concussion extends PrepareSkill implements CooldownSkill, Listener, DebuffSkill, OffensiveSkill {

    @Getter
    private double duration;
    private int concussionStrength;

    @Inject
    public Concussion(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Concussion";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to prepare",
                "",
                "Your next hit will <effect>Concuss</effect> the target for <val>" + getDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.CONCUSSED.getDescription(concussionStrength)
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
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!hasSkill(damager)) return;

        if (active.contains(damager.getUniqueId())) {
            event.addReason("Concussion");
            if (championsManager.getEffects().hasEffect(damagee, EffectTypes.CONCUSSED)) {
                UtilMessage.simpleMessage(damager, getName(), "<alt>%s</alt> is already concussed.", damagee.getName());
                return;
            }

            championsManager.getEffects().addEffect(damagee, damager, EffectTypes.CONCUSSED, concussionStrength, (long) (getDuration() * 1000L));

            UtilMessage.simpleMessage(damager, getName(), "You gave <alt>" + damagee.getName() + "</alt> a concussion.");
            UtilMessage.simpleMessage(damagee, getName(), "<alt>" + damager.getName() + "</alt> gave you a concussion.");
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
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 1.5, Double.class);
        concussionStrength = getConfig("concussionStrength", 1, Integer.class);
    }
}
