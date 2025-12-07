package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.projectile.BlackHole;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class BlackHoleAbility extends ItemAbility {

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
    @EqualsAndHashCode.Exclude
    private final CooldownManager cooldownManager;

    // Track active black holes
    @EqualsAndHashCode.Exclude
    private final WeakHashMap<Player, List<BlackHole>> blackHoles = new WeakHashMap<>();

    @Inject
    private BlackHoleAbility(Champions champions, CooldownManager cooldownManager) {
        super(new NamespacedKey(champions, "black_hole"),
                "Black Hole",
                "Creates a black hole that pulls nearby entities for a short period of time.",
                TriggerTypes.RIGHT_CLICK);
        this.champions = champions;
        this.cooldownManager = cooldownManager;
        
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
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        if (!cooldownManager.use(player, getName(), cooldown, true, true, false)) {
            return false;
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
        UtilItem.damageItem(player, itemStack, 5);
        return true;
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