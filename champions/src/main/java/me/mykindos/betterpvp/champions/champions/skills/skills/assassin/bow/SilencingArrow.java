package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Getter
@Singleton
@BPvPListener
public class SilencingArrow extends PrepareArrowSkill implements DebuffSkill {

    private double duration;

    @Inject
    public SilencingArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Silencing Arrow";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow will <effect>Silence</effect> your",
                "target for <val>" + getDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.SILENCE.getDescription(0)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    @Override
    public void onHit(Player damager, LivingEntity target) {
        championsManager.getEffects().addEffect(target, EffectTypes.SILENCE, (long) (getDuration()) * 1000L);
        if (championsManager.getEffects().hasEffect(target, EffectTypes.IMMUNE)) {
            UtilMessage.simpleMessage(damager, getClassType().getName(), "<alt>" + target.getName() + "</alt> is immune to your silence!");
            return;
        }
        UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <yellow>%s</yellow> with <alt>%s</alt>.", target.getName(), getName());
        if (!(target instanceof Player damagee)) return;
        UtilMessage.simpleMessage(damagee, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", damager.getName(), getName());
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.EFFECT)
                .location(location)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }


    @Override
    public void activate(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 0.5, Double.class);
    }
}
