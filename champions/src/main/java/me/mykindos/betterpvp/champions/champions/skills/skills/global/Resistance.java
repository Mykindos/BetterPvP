package me.mykindos.betterpvp.champions.champions.skills.skills.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;

@Getter
@Singleton
@BPvPListener
public class Resistance extends Skill implements PassiveSkill, BuffSkill {

    private double durationReduction;


    @Inject
    public Resistance(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Resistance";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Negative effects have their duration reduced by <val>" + UtilFormat.formatNumber(durationReduction, 0) + "%</val>",
                "",
                "Self-inflicted effects are not affected by this skill."
        };
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @Override
    public SkillType getType() {

        return SkillType.GLOBAL;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onReceiveEffect(EffectReceiveEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getTarget() instanceof Player player)) return;
        if (event.getEffect().getApplier() != null && event.getEffect().getApplier().equals(player)) return;
        if (!event.getEffect().getEffectType().isNegative()) return;

        if (hasSkill(player)) {
            double reduction = 1.0 - (getDurationReduction() / 100);
            event.getEffect().setLength((long) (event.getEffect().getRawLength() * reduction));
        }
    }

    @EventHandler
    public void onPotionEffectReceived(EntityPotionEffectEvent event) {
        if (event.isCancelled()) return;
        if (event.getNewEffect() == null) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) return;
        if (!UtilEffect.isNegativePotionEffect(event.getNewEffect())) return;

        if (hasSkill(player)) {
            UtilServer.runTaskLater(champions, () -> {
                player.removePotionEffect(event.getNewEffect().getType());
                double reduction = 1.0 - (getDurationReduction() / 100);
                UtilEffect.applyCraftEffect(player, (new PotionEffect(event.getNewEffect().getType(), (int) (event.getNewEffect().getDuration() * reduction), event.getNewEffect().getAmplifier())));
            }, 1);
        }
    }

    @EventHandler
    public void onEntityCombustReceived(EntityCombustByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (hasSkill(player)) {
            UtilServer.runTaskLater(champions, () -> {
                double reduction = 1.0 - (getDurationReduction() / 100);
                event.setDuration((float) (event.getDuration() * reduction));
            }, 1);
        }
    }

    @Override
    public void loadSkillConfig() {
        durationReduction = getConfig("durationReduction", 30.0, Double.class);
    }
}
