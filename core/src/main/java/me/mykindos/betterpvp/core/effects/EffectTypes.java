package me.mykindos.betterpvp.core.effects;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.effects.types.negative.AntiHealEffect;
import me.mykindos.betterpvp.core.effects.types.negative.BleedEffect;
import me.mykindos.betterpvp.core.effects.types.negative.BlindnessEffect;
import me.mykindos.betterpvp.core.effects.types.negative.ConcussedEffect;
import me.mykindos.betterpvp.core.effects.types.negative.DarknessEffect;
import me.mykindos.betterpvp.core.effects.types.negative.FrozenEffect;
import me.mykindos.betterpvp.core.effects.types.negative.LevitationEffect;
import me.mykindos.betterpvp.core.effects.types.negative.NoJumpEffect;
import me.mykindos.betterpvp.core.effects.types.negative.NoSprintEffect;
import me.mykindos.betterpvp.core.effects.types.negative.PoisonEffect;
import me.mykindos.betterpvp.core.effects.types.negative.ShockEffect;
import me.mykindos.betterpvp.core.effects.types.negative.SilenceEffect;
import me.mykindos.betterpvp.core.effects.types.negative.SlownessEffect;
import me.mykindos.betterpvp.core.effects.types.negative.StunEffect;
import me.mykindos.betterpvp.core.effects.types.negative.VulnerabilityEffect;
import me.mykindos.betterpvp.core.effects.types.negative.WitherEffect;
import me.mykindos.betterpvp.core.effects.types.positive.AttackSpeedEffect;
import me.mykindos.betterpvp.core.effects.types.positive.CooldownReductionEffect;
import me.mykindos.betterpvp.core.effects.types.positive.EnergyReductionEffect;
import me.mykindos.betterpvp.core.effects.types.positive.FireResistanceEffect;
import me.mykindos.betterpvp.core.effects.types.positive.HealthBoostEffect;
import me.mykindos.betterpvp.core.effects.types.positive.ImmuneEffect;
import me.mykindos.betterpvp.core.effects.types.positive.InvisibilityEffect;
import me.mykindos.betterpvp.core.effects.types.positive.NoFallEffect;
import me.mykindos.betterpvp.core.effects.types.positive.ProtectionEffect;
import me.mykindos.betterpvp.core.effects.types.positive.RegenerationEffect;
import me.mykindos.betterpvp.core.effects.types.positive.ResistanceEffect;
import me.mykindos.betterpvp.core.effects.types.positive.SpeedEffect;
import me.mykindos.betterpvp.core.effects.types.positive.StrengthEffect;
import me.mykindos.betterpvp.core.effects.types.positive.VanishEffect;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@CustomLog
public class EffectTypes {

    @Getter
    private static List<EffectType> effectTypes = new ArrayList<>();

    // <editor-fold defaultstate="collapsed" desc="Negative Effect Types">
    public static final EffectType SILENCE = createEffectType(new SilenceEffect());
    public static final EffectType VULNERABILITY = createEffectType(new VulnerabilityEffect());
    public static final EffectType STUN = createEffectType(new StunEffect());
    public static final EffectType POISON = createEffectType(new PoisonEffect());
    public static final EffectType NO_JUMP = createEffectType(new NoJumpEffect());
    public static final EffectType NO_SPRINT = createEffectType(new NoSprintEffect());
    public static final EffectType CONCUSSED = createEffectType(new ConcussedEffect());
    public static final EffectType BLINDNESS = createEffectType(new BlindnessEffect());
    public static final EffectType LEVITATION = createEffectType(new LevitationEffect());
    public static final EffectType BLEED = createEffectType(new BleedEffect());
    public static final EffectType SLOWNESS = createEffectType(new SlownessEffect());
    public static final EffectType SHOCK = createEffectType(new ShockEffect());
    public static final EffectType WITHER = createEffectType(new WitherEffect());
    public static final EffectType DARKNESS = createEffectType(new DarknessEffect());
    public static final EffectType FROZEN = createEffectType(new FrozenEffect());
    public static final EffectType ANTI_HEAL = createEffectType(new AntiHealEffect());
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Positive Effect Types">
    public static final EffectType SPEED = createEffectType(new SpeedEffect());
    public static final EffectType STRENGTH = createEffectType(new StrengthEffect());
    public static final EffectType RESISTANCE = createEffectType(new ResistanceEffect());
    public static final EffectType FIRE_RESISTANCE = createEffectType(new FireResistanceEffect());
    public static final EffectType IMMUNE = createEffectType(new ImmuneEffect());
    public static final EffectType VANISH = createEffectType(new VanishEffect());
    public static final EffectType INVISIBILITY = createEffectType(new InvisibilityEffect());
    public static final EffectType PROTECTION = createEffectType(new ProtectionEffect());
    public static final EffectType NO_FALL = createEffectType(new NoFallEffect());
    public static final EffectType HEALTH_BOOST = createEffectType(new HealthBoostEffect());
    public static final EffectType REGENERATION = createEffectType(new RegenerationEffect());
    public static final EffectType COOLDOWN_REDUCTION = createEffectType(new CooldownReductionEffect());
    public static final EffectType ENERGY_REDUCTION = createEffectType(new EnergyReductionEffect());
    public static final EffectType ATTACK_SPEED = createEffectType(new AttackSpeedEffect());
    // </editor-fold>

    public static EffectType createEffectType(EffectType effectType) {
        log.info("Added effect: {}", effectType.getName()).submit();
        effectTypes.add(effectType);
        return effectType;
    }

    public static Optional<EffectType> getEffectTypeByName(String name) {
        return effectTypes.stream().filter(effectType -> effectType.getName().equalsIgnoreCase(name)).findFirst();
    }

    public static void disableProtectionReminder(Player player) {
        Component runComponent = Component.text("/protection", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/protection"))
                .hoverEvent(HoverEvent.showText(Component.text("/protection")));
        Component component = Component.text("Use ").append(runComponent).append(Component.text(" to disable this permanently"));
        UtilMessage.message(player, "Protected", component);
    }

}
