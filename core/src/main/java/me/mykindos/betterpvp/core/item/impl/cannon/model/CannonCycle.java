package me.mykindos.betterpvp.core.item.impl.cannon.model;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.impl.cannon.event.PreCannonShootEvent;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Drives one cannon's load-fire-recover sequence, one tick at a time.
 * <p>
 * A {@link SceneBehavior}, ticked by {@code SceneTicker} with every other scene entity: the sequence state lives on
 * the cannon it belongs to, only materialized cannons cost anything per tick, and a cannon destroyed mid-fuse takes
 * its own pending state with it.
 */
@Getter
public class CannonCycle implements SceneBehavior {

    private final @NotNull CannonProp cannon;

    private @NotNull CannonState state = CannonState.IDLE;
    private long stateSince = System.currentTimeMillis();

    /** Who is operating the cannon this cycle. Kept by id so a logout mid-fuse still credits the shot. */
    private @Nullable UUID operator;

    public CannonCycle(@NotNull CannonProp cannon) {
        this.cannon = cannon;
    }

    @Override
    public void start() {
        // A cannon that was chambered when its chunk unloaded (or when the server stopped) comes back chambered.
        this.state = cannon.getAmmo() != null ? CannonState.LOADED : CannonState.IDLE;
        this.stateSince = System.currentTimeMillis();
    }

    @Override
    public void tick() {
        switch (state) {
            case FUSING -> tickFuse();
            case COOLDOWN -> {
                if (UtilTime.elapsed(stateSince, cooldownMillis() + CannonProp.COOLDOWN_LERP_OUT + 100L)) {
                    transition(CannonState.IDLE);
                }
            }
            default -> {
            }
        }
        refreshTags();
    }

    private void tickFuse() {
        if (UtilTime.elapsed(stateSince, fuseMillis())) {
            detonate();
            return;
        }
        cannon.emitFuse(null);
    }

    private void detonate() {
        final UUID operatorId = operator;
        if (operatorId == null) {
            transition(CannonState.COOLDOWN);
            return;
        }

        final Player player = Bukkit.getPlayer(operatorId);
        final PreCannonShootEvent event = new PreCannonShootEvent(cannon, player, operatorId);
        event.callEvent();
        if (event.isCancelled()) {
            transition(CannonState.LOADED);
            return;
        }

        transition(cannon.getArchetype().getFiringMode().onFuseComplete(cannon, player, operatorId));
    }

    /** Chambers the cannon. Called by the reload flow and by firing modes that load something other than ammo. */
    public void load() {
        if (state == CannonState.IDLE) {
            transition(CannonState.LOADED);
        }
    }

    /** Starts the fuse on behalf of {@code operatorId}. No-op unless the cannon is chambered and idle-handed. */
    public boolean beginFuse(@NotNull UUID operatorId) {
        if (state != CannonState.LOADED && state != CannonState.BOARDING) {
            return false;
        }
        this.operator = operatorId;
        transition(CannonState.FUSING);
        return true;
    }

    /** Puts a rider aboard without starting the fuse yet. */
    public void beginBoarding(@NotNull UUID operatorId) {
        this.operator = operatorId;
        transition(CannonState.BOARDING);
    }

    /**
     * Closes a cycle that continued past the fuse (a passenger flight), so the cannon starts cooling down from the
     * moment it actually discharged rather than the moment it was aimed.
     */
    public void completeShot() {
        this.operator = null;
        transition(CannonState.COOLDOWN);
    }

    /** Abandons the current cycle and returns the cannon to a usable state (rider bailed, event cancelled). */
    public void abort() {
        this.operator = null;
        transition(cannon.getAmmo() != null ? CannonState.LOADED : CannonState.IDLE);
    }

    private void transition(@NotNull CannonState next) {
        this.state = next;
        this.stateSince = System.currentTimeMillis();
    }

    public long fuseMillis() {
        return cannon.fuseMillis();
    }

    public long cooldownMillis() {
        return (long) (cannon.getArchetype().getCooldownSeconds() * 1000L);
    }

    /** Whether the cannon is far enough past its last shot to be used again. */
    public boolean isReady() {
        return state != CannonState.COOLDOWN || UtilTime.elapsed(stateSince, cooldownMillis());
    }

    private void refreshTags() {
        final TextDisplay health = cannon.getHealthTag() == null ? null : cannon.getHealthTag().getDisplay();
        if (health != null && cannon.isInitialized() && cannon.getEntity() instanceof LivingEntity living) {
            final double max = Objects.requireNonNull(living.getAttribute(Attribute.MAX_HEALTH)).getValue();
            health.text(new ProgressBar((float) (living.getHealth() / max), 15).build());
        }

        final TextDisplay instructions = cannon.getInstructionTag() == null ? null : cannon.getInstructionTag().getDisplay();
        if (instructions != null) {
            instructions.text(cannon.instructions());
        }
    }

    /** The bottom line of the cannon's tag: a fuse bar, a cooldown timer, or a readiness label. */
    public @NotNull Component progressLine() {
        return switch (state) {
            case FUSING -> {
                final double secondsLeft = (stateSince + fuseMillis() - System.currentTimeMillis()) / 1000d;
                yield new ProgressBar((float) (secondsLeft / (fuseMillis() / 1000d)), 25)
                        .inverted()
                        .withCharacter(' ')
                        .build()
                        .decoration(TextDecoration.STRIKETHROUGH, true);
            }
            case COOLDOWN -> {
                final double secondsLeft = (stateSince + cooldownMillis() - System.currentTimeMillis()) / 1000d;
                if (secondsLeft <= 0) {
                    yield Component.text("READY", NamedTextColor.GREEN, TextDecoration.BOLD);
                }
                final TextColor color = ProgressColor.of((float) (secondsLeft / (cooldownMillis() / 1000d)))
                        .inverted().getTextColor();
                yield Component.text(UtilFormat.formatNumber(secondsLeft, 1) + "s", color, TextDecoration.BOLD);
            }
            case BOARDING -> Component.text("BOARDING", NamedTextColor.AQUA, TextDecoration.BOLD);
            case TARGETING -> Component.text("TARGETING", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
            case LOADED -> Component.text("LOADED", NamedTextColor.GOLD, TextDecoration.BOLD);
            case IDLE -> Component.empty();
        };
    }
}
