package me.mykindos.betterpvp.champions.champions.skills.traits.knight;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.traits.Trait;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

@Singleton
@BPvPListener
public class Resilience extends Trait implements PassiveSkill, BuffSkill, DefensiveSkill {

    private double durationReduction;

    @Inject
    public Resilience(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Resilience";
    }

    @Override
    public Component[] getDescription(int level) {
        return traitDescription(traitValue(durationReduction, 0, "%"));
    }

    @Override
    public @NotNull Role getClassType() {
        return Role.KNIGHT;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onReceiveEffect(EffectReceiveEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getTarget() instanceof Player player)) return;

        WeakReference<LivingEntity> applierRef = event.getEffect().getApplier();
        if (applierRef != null) {
            LivingEntity applier = applierRef.get();
            if (applier != null && applier.equals(player)) return;
        }
        if (!event.getEffect().getEffectType().isNegative()) return;
        if (event.getEffect().getEffectType().isSpecial()) return;

        if (getLevel(player) > 0) {
            event.getEffect().setLength((long) (event.getEffect().getRawLength() * getRemainingFraction()));
        }
    }

    @EventHandler
    public void onPotionEffectReceived(EntityPotionEffectEvent event) {
        if (event.isCancelled()) return;
        if (event.getNewEffect() == null) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) return;
        if (!UtilEffect.isNegativePotionEffect(event.getNewEffect())) return;

        if (getLevel(player) > 0) {
            UtilServer.runTaskLater(champions, () -> {
                player.removePotionEffect(event.getNewEffect().getType());
                UtilEffect.applyCraftEffect(player, new PotionEffect(event.getNewEffect().getType(),
                        (int) (event.getNewEffect().getDuration() * getRemainingFraction()),
                        event.getNewEffect().getAmplifier()));
            }, 1);
        }
    }

    @EventHandler
    public void onEntityCombustReceived(EntityCombustByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (getLevel(player) > 0) {
            UtilServer.runTaskLater(champions, () -> event.setDuration((float) (event.getDuration() * getRemainingFraction())), 1);
        }
    }

    private double getRemainingFraction() {
        return 1.0 - (durationReduction / 100);
    }

    @Override
    protected void loadTraitConfig() {
        durationReduction = getConfig("durationReduction", 30.0, Double.class);
    }
}
