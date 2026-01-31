package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.projectile.MeridianBeam;
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
@EqualsAndHashCode(callSuper = true)
public class MeridianBeamAbility extends CooldownInteraction {

    private double cooldown;
    private double damage;
    private double speed;
    private double hitbox;
    private double travelSeconds;

    @EqualsAndHashCode.Exclude
    private final Champions champions;

    // Track active beams
    @EqualsAndHashCode.Exclude
    private final WeakHashMap<Player, List<MeridianBeam>> beams = new WeakHashMap<>();

    @Inject
    public MeridianBeamAbility(Champions champions, CooldownManager cooldownManager) {
        super(MeridianBeam.NAME,
                "Fires a damaging beam of energy that travels in a straight line.",
                cooldownManager);
        this.champions = champions;

        // Default values, will be overridden by config
        this.cooldown = 1.0;
        this.damage = 4.0;
        this.speed = 4.0;
        this.hitbox = 0.5;
        this.travelSeconds = 0.3;
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    public boolean informCooldown() {
        return false;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Location location = player.getEyeLocation();
        final MeridianBeam beam = new MeridianBeam(
                player,
                location,
                hitbox,
                (long) (travelSeconds * 1000L),
                damage,
                this
        );

        beam.redirect(player.getLocation().getDirection().multiply(speed));
        beams.computeIfAbsent(player, p -> new ArrayList<>()).add(beam);
        if (itemStack != null) {
            UtilItem.damageItem(player, itemStack, 1);
        }
        return InteractionResult.Success.ADVANCE;
    }

    public void processBeams() {
        final Iterator<Map.Entry<Player, List<MeridianBeam>>> iterator = beams.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, List<MeridianBeam>> cur = iterator.next();
            final Player player = cur.getKey();
            final List<MeridianBeam> playerBeams = cur.getValue();

            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final Iterator<MeridianBeam> beamIterator = playerBeams.iterator();
            while (beamIterator.hasNext()) {
                final MeridianBeam beam = beamIterator.next();
                if (beam.isMarkForRemoval()) {
                    beamIterator.remove();
                    continue;
                }

                beam.tick();
            }

            if (playerBeams.isEmpty()) {
                iterator.remove();
            }
        }
    }
}
