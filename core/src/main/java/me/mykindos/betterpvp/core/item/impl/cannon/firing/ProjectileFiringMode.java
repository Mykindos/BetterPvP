package me.mykindos.betterpvp.core.item.impl.cannon.firing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmo;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonProjectile;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonFuseEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonShootEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonService;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The siege cannon: load a cannonball, fuse it, and something leaves the barrel at speed. What that something is, and
 * what it does when it lands, is entirely the loaded {@link CannonAmmo}'s business.
 */
@Singleton
public class ProjectileFiringMode implements CannonFiringMode {

    private final Provider<CannonService> service;

    @Inject
    private ProjectileFiringMode(Provider<CannonService> service) {
        this.service = service;
    }

    @Override
    public @NotNull String id() {
        return "projectile";
    }

    @Override
    public boolean consumesAmmo() {
        return true;
    }

    @Override
    public boolean onTriggerRequested(@NotNull CannonProp cannon, @NotNull Player player) {
        if (cannon.getCycle() == null || cannon.getAmmo() == null) {
            return false;
        }

        final CannonFuseEvent event = new CannonFuseEvent(cannon, player);
        event.callEvent();
        if (event.isCancelled()) {
            return false;
        }
        return cannon.getCycle().beginFuse(player.getUniqueId());
    }

    @Override
    public @NotNull CannonState onFuseComplete(@NotNull CannonProp cannon, @Nullable Player operator,
                                               @NotNull UUID operatorId) {
        final CannonAmmo ammo = cannon.getAmmo();
        if (ammo == null) {
            return CannonState.IDLE;
        }

        final Vector direction = cannon.getLocation().getDirection().multiply(cannon.getProperties().getPower());
        final CannonProjectile projectile = ammo.launch(cannon, cannon.getMuzzle(), direction, operatorId);
        service.get().trackProjectile(projectile);

        cannon.setAmmo(null);
        service.get().persist(cannon);
        new CannonShootEvent(cannon, projectile, operator).callEvent();
        return CannonState.COOLDOWN;
    }

    @Override
    public @NotNull Component actionLine(@NotNull CannonProp cannon) {
        if (cannon.getCycleState() == CannonState.LOADED) {
            return Component.text("Shift-Right-Click", NamedTextColor.WHITE, TextDecoration.BOLD)
                    .append(Component.text(" to ", NamedTextColor.AQUA))
                    .append(Component.text("Fire", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        }
        if (cannon.getCycleState() == CannonState.IDLE) {
            return Component.text("Right-Click", NamedTextColor.WHITE, TextDecoration.BOLD)
                    .append(Component.text(" to ", NamedTextColor.AQUA))
                    .append(Component.text("Load", NamedTextColor.YELLOW, TextDecoration.BOLD));
        }
        return Component.empty();
    }
}
