package me.mykindos.betterpvp.champions.champions.skills.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Denies {@link MovementSkill}s to players under {@link EffectTypes#ROOTED}. The effect itself lives in
 * core and knows nothing about champions, so the skill half of "rooted players cannot reposition" is
 * enforced here.
 */
@Singleton
@BPvPListener
public class RootedSkillListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public RootedSkillListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onUseMovementSkillWhileRooted(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        final IChampionsSkill skill = event.getSkill();
        if (!(skill instanceof MovementSkill)) return;
        if (skill.ignoreNegativeEffects()) return;

        final Player player = event.getPlayer();
        if (effectManager.hasEffect(player, EffectTypes.ROOTED)) {
            UtilMessage.message(player, skill.getClassType().getDisplayName(), "champions.skill.rooted",
                    skill.getDisplayName().color(NamedTextColor.GREEN));
            event.setCancelled(true);
        }
    }
}
