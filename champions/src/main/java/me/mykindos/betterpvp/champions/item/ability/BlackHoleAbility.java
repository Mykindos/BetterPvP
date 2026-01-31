package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.projectile.BlackHole;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class BlackHoleAbility extends CooldownInteraction {

    private double radius;
    private double speed;
    private double hitbox;
    private double pullStrength;
    private double pullRadius;
    private double aliveSeconds;
    private double expandSeconds;
    private double travelSeconds;
    private double cooldown;

    @EqualsAndHashCode.Exclude
    private final Champions champions;

    // Track active black holes
    @EqualsAndHashCode.Exclude
    private final WeakHashMap<Player, List<BlackHole>> blackHoles = new WeakHashMap<>();

    @Inject
    public BlackHoleAbility(Champions champions, CooldownManager cooldownManager) {
        super("Black Hole",
                "Creates a black hole that pulls nearby entities for a short period of time.",
                cooldownManager);
        this.champions = champions;

        // Default values, will be overridden by config
        this.radius = 0.5;
        this.speed = 3.0;
        this.hitbox = 0.5;
        this.pullStrength = 0.12;
        this.pullRadius = 3.5;
        this.aliveSeconds = 1.3;
        this.expandSeconds = 0.75;
        this.travelSeconds = 2.0;
        this.cooldown = 10.0;
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Location location = player.getEyeLocation();

        final BlackHole hole = new BlackHole(
                player,
                location,
                hitbox,
                pullStrength,
                pullRadius,
                aliveSeconds,
                expandSeconds,
                (long) (travelSeconds * 1000L),
                radius
        );

        hole.redirect(player.getLocation().getDirection().multiply(speed));
        blackHoles.computeIfAbsent(player, p -> new ArrayList<>()).add(hole);

        // Consume durability
        if (itemStack != null) {
            UtilItem.damageItem(player, itemStack, 5);
        }
        return InteractionResult.Success.ADVANCE;
    }

    public void processBlackHoles() {
        final Iterator<Map.Entry<Player, List<BlackHole>>> iterator = blackHoles.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, List<BlackHole>> cur = iterator.next();
            final Player player = cur.getKey();
            final List<BlackHole> holes = cur.getValue();

            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final Iterator<BlackHole> holeIterator = holes.iterator();
            while (holeIterator.hasNext()) {
                final BlackHole hole = holeIterator.next();
                if (hole.isMarkForRemoval()) {
                    holeIterator.remove();
                    continue;
                }

                hole.tick();
            }

            if (holes.isEmpty()) {
                iterator.remove();
            }
        }
    }
}
