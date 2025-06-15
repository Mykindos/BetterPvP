package me.mykindos.betterpvp.champions.item;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.RegenerationShieldAbility;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Singleton
@BPvPListener
@EqualsAndHashCode(callSuper = true)
public class GiantsBroadsword extends WeaponItem implements Listener, ReloadHook {

    private static final ItemStack model = ItemView.builder()
            .material(Material.LEATHER_HORSE_ARMOR)
            .itemModel(Material.MUSIC_DISC_CAT.key())
            .customModelData(1)
            .build()
            .get();

    private final RegenerationShieldAbility regenerationShieldAbility;

    @EqualsAndHashCode.Exclude
    private final Set<UUID> holdingPlayers = new HashSet<>();
    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;

    @Inject
    private GiantsBroadsword(Champions champions,
                            RegenerationShieldAbility regenerationShieldAbility,
                            ItemFactory itemFactory) {
        super(champions, "Giant's Broadsword", model, ItemRarity.LEGENDARY);
        this.itemFactory = itemFactory;
        this.regenerationShieldAbility = regenerationShieldAbility;

        // Add abilities to container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(regenerationShieldAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final ItemConfig config = ItemConfig.of(Champions.class, this);
        
        // Configure RegenerationShield ability
        regenerationShieldAbility.setRegenerationAmplifier(config.getConfig("regenAmplifier", 5, Integer.class));
        regenerationShieldAbility.setEnergyPerTick(config.getConfig("energyPerTick", 1.5, Double.class));
    }

    /**
     * Track players who are holding the item
     */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        // Remove player from tracking
        holdingPlayers.remove(player.getUniqueId());

        // Check if they're now holding our item
        if (newItem != null) {
            itemFactory.fromItemStack(newItem).ifPresent(item -> {
                if (item.getBaseItem() == this) {
                    holdingPlayers.add(player.getUniqueId());
                }
            });
        }
    }

    /**
     * Display particles for players holding the item
     */
    @UpdateEvent(delay = 500) // Every half-second
    public void displayParticles() {
        Iterator<UUID> iterator = holdingPlayers.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // Check if still holding the item
            boolean stillHolding = itemFactory.fromItemStack(player.getInventory().getItemInMainHand())
                    .map(item -> item.getBaseItem() == this)
                    .orElse(false);

            if (!stillHolding) {
                iterator.remove();
                continue;
            }

            // Display particles
            new ParticleBuilder(Particle.ENCHANTED_HIT)
                    .location(player.getLocation().add(0, 1, 0))
                    .offset(0.3f, 0.3f, 0.3f)
                    .count(3)
                    .extra(0)
                    .receivers(60)
                    .spawn();
        }
    }
} 