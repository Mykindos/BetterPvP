package me.mykindos.betterpvp.champions.champions.skills.traits.assassin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.traits.Trait;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.CombatFeaturesService;
import me.mykindos.betterpvp.core.combat.events.PlayerCombatFeatureStateChangeEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

@Singleton
@BPvPListener
public class Blur extends Trait implements PassiveSkill, BuffSkill, MovementSkill {

    private int speedStrength;

    private final RoleManager roleManager;
    private final EffectManager effectManager;
    private final CombatFeaturesService combatFeaturesService;

    @Inject
    public Blur(Champions champions, ChampionsManager championsManager, RoleManager roleManager,
                EffectManager effectManager, CombatFeaturesService combatFeaturesService) {
        super(champions, championsManager);
        this.roleManager = roleManager;
        this.effectManager = effectManager;
        this.combatFeaturesService = combatFeaturesService;
    }

    @Override
    public String getName() {
        return "Blur";
    }

    @Override
    public Component[] getDescription(int level) {
        return traitDescription(Component.text(UtilFormat.getRomanNumeral(speedStrength), NamedTextColor.YELLOW));
    }

    @Override
    public @NotNull Role getClassType() {
        return Role.ASSASSIN;
    }

    @UpdateEvent(delay = 500)
    public void applySpeed() {
        if (!isEnabled()) {
            return;
        }

        for (LivingEntity livingEntity : roleManager.getLivingEntities()) {
            if (roleManager.hasRole(livingEntity, Role.ASSASSIN)
                    && (!(livingEntity instanceof Player player) || combatFeaturesService.isActive(player))) {
                effectManager.addEffect(livingEntity, null, EffectTypes.SPEED, getName(), speedStrength, -1, true, true, false, null);
            } else {
                effectManager.removeEffect(livingEntity, EffectTypes.SPEED, getName(), false);
            }
        }
    }

    @EventHandler
    public void onCombatFeatureStateChange(PlayerCombatFeatureStateChangeEvent event) {
        if (event.isActive()) {
            return;
        }

        if (roleManager.hasRole(event.getPlayer(), Role.ASSASSIN)) {
            effectManager.removeEffect(event.getPlayer(), EffectTypes.SPEED, getName(), false);
        }
    }

    @Override
    protected void loadTraitConfig() {
        speedStrength = getConfig("speedStrength", 2, Integer.class);
    }
}
