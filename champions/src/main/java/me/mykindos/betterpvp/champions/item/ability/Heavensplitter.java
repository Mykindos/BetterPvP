package me.mykindos.betterpvp.champions.item.ability;

import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.projectile.BoomerangProjectile;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Heavensplitter extends ItemAbility implements Listener {

    private static final String TRIDENT_METADATA_KEY = "heavensplitter_trident";

    @EqualsAndHashCode.Include
    private float hitbox; // Hitbox radius for collision detection
    @EqualsAndHashCode.Include
    private double damage; // Damage dealt by the trident
    @EqualsAndHashCode.Include
    private double impactVelocity; // velocity at which the target is hit
    @EqualsAndHashCode.Include
    private double velocity; // Velocity at which the trident flies
    @EqualsAndHashCode.Include
    private double airTime; // Seconds
    private final BaseItem heldItem;
    private final ItemFactory itemFactory;
    private final ClientManager clientManager;
    private final Map<Player, BoomerangProjectile> projectiles = new HashMap<>();

    public Heavensplitter(BaseItem heldItem, ItemFactory itemFactory, ClientManager clientManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class),
                        "heavensplitter"),
                "Heavensplitter",
                "Throw the weapon, summon the power of Thor, and deal damage to enemies in its path.",
                TriggerTypes.OFF_HAND);
        this.heldItem = heldItem;
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getPlugin(Champions.class));
        UtilServer.runTaskTimer(JavaPlugin.getPlugin(Champions.class), this::tickProjectiles, 0L, 1L);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        final Player caster = Objects.requireNonNull(client.getGamer().getPlayer());
        // Player already has a projectile, summon back
        if (projectiles.containsKey(caster)) {
            final BoomerangProjectile projectile = projectiles.get(caster);
            projectile.recall();
            projectile.playRedirectSound();
            return true;
        }

        // Spawn projectile
        final BoomerangProjectile projectile = new BoomerangProjectile(
                getName(),
                caster,
                hitbox,
                caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(0.5)),
                (long) (airTime * 1000),
                damage,
                impactVelocity,
                itemInstance,
                this
        );
        projectile.redirect(caster.getLocation().getDirection().multiply(velocity));
        projectile.playRedirectSound();
        projectiles.put(caster, projectile);

        // Consume durability
        UtilItem.damageItem(caster, itemStack, 1);
        return true;
    }

    public void tickProjectiles() {
        final Iterator<Map.Entry<Player, BoomerangProjectile>> iterator = projectiles.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, BoomerangProjectile> entry = iterator.next();
            final Player caster = entry.getKey();
            final BoomerangProjectile projectile = entry.getValue();
            // Remove if player is offline or dead
            if (caster == null || !caster.isOnline() || caster.isDead()) {
                projectile.remove();
                iterator.remove();
                continue;
            }

            // Check if the projectile is expired or marked for removal
            if (projectile.isExpired() || projectile.isMarkForRemoval()) {
                projectile.remove();
                iterator.remove();
                continue;
            }

            // Update the projectile's position
            projectile.tick();
        }
    }
}