package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.projectile.MeridianBeam;
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
@EqualsAndHashCode(callSuper = true)
public class MeridianBeamAbility extends ItemAbility {

    private double cooldown;
    private double damage;
    private double speed;
    private double hitbox;
    private double travelSeconds;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final CooldownManager cooldownManager;
    
    // Track active beams
    @EqualsAndHashCode.Exclude
    private final WeakHashMap<Player, List<MeridianBeam>> beams = new WeakHashMap<>();

    @Inject
    private MeridianBeamAbility(Champions champions, CooldownManager cooldownManager) {
        super(new NamespacedKey(champions, "meridian_beam"),
                MeridianBeam.NAME,
                "Fires a damaging beam of energy that travels in a straight line.",
                TriggerTypes.LEFT_CLICK);
        this.champions = champions;
        this.cooldownManager = cooldownManager;
        
        // Default values, will be overridden by config
        this.cooldown = 1.0;
        this.damage = 4.0;
        this.speed = 4.0;
        this.hitbox = 0.5;
        this.travelSeconds = 0.3;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        if (!cooldownManager.use(player, getName(), cooldown, false, true, false)) {
            return false;
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
        UtilItem.damageItem(player, itemStack, 1);
        return true;
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