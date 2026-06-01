package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.projectile.HealingNova;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
public class HealingNovaAbility extends CooldownInteraction implements DisplayedInteraction {

    private double radius;
    private double speed;
    private double hitbox;
    private double healRadius;
    private double aliveSeconds;
    private double expandSeconds;
    private double travelSeconds;
    private double cooldown;
    private double healAmount;

    @EqualsAndHashCode.Exclude
    private final Champions champions;

    @EqualsAndHashCode.Exclude
    private final WeakHashMap<Player, List<HealingNova>> novas = new WeakHashMap<>();

    @Inject
    public HealingNovaAbility(Champions champions, CooldownManager cooldownManager) {
        super("healing_nova", cooldownManager);
        this.champions = champions;

        // Default values, will be overridden by config
        this.radius = 1.5;
        this.speed = 3.0;
        this.hitbox = 0.5;
        this.healRadius = 7.0;
        this.aliveSeconds = 1.3;
        this.expandSeconds = 0.75;
        this.travelSeconds = 2.0;
        this.cooldown = 10.0;
        this.healAmount = 12.0;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Healing Nova");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Launches a nova that heals nearby allies for ", NamedTextColor.GRAY)
                .append(Component.text("12", NamedTextColor.YELLOW))
                .append(Component.text(" ❤", TextColor.color(255, 0, 0)))
                .append(Component.text(" when it lands.", NamedTextColor.GRAY));
    }

    @Override
    public double getCooldown(InteractionActor actor) {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Location location = player.getEyeLocation();

        final HealingNova nova = new HealingNova(
                player,
                location,
                hitbox,
                healRadius,
                aliveSeconds,
                expandSeconds,
                (long) (travelSeconds * 1000L),
                radius,
                healAmount
        );

        nova.redirect(player.getLocation().getDirection().multiply(speed));
        novas.computeIfAbsent(player, p -> new ArrayList<>()).add(nova);

        if (itemStack != null) {
            UtilItem.damageItem(player, itemStack, 5);
        }
        return InteractionResult.Success.ADVANCE;
    }

    public void processNovas() {
        final Iterator<Map.Entry<Player, List<HealingNova>>> iterator = novas.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, List<HealingNova>> cur = iterator.next();
            final Player player = cur.getKey();
            final List<HealingNova> playerNovas = cur.getValue();

            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final Iterator<HealingNova> novaIterator = playerNovas.iterator();
            while (novaIterator.hasNext()) {
                final HealingNova nova = novaIterator.next();
                if (nova.isMarkForRemoval()) {
                    novaIterator.remove();
                    continue;
                }

                nova.tick();
            }

            if (playerNovas.isEmpty()) {
                iterator.remove();
            }
        }
    }
}
