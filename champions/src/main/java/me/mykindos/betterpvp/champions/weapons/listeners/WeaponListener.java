package me.mykindos.betterpvp.champions.weapons.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.utilities.ChampionsNamespacedKeys;
import me.mykindos.betterpvp.champions.weapons.WeaponManager;
import me.mykindos.betterpvp.champions.weapons.types.ChannelWeapon;
import me.mykindos.betterpvp.champions.weapons.types.CooldownWeapon;
import me.mykindos.betterpvp.champions.weapons.types.InteractWeapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEndEvent;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerCombatLogEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateNameEvent;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemDropEvent;
import me.mykindos.betterpvp.core.items.BPVPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
@BPvPListener
public class WeaponListener implements Listener {

    private final WeaponManager weaponManager;
    private final ItemHandler itemHandler;
    private final CooldownManager cooldownManager;
    private final EnergyHandler energyHandler;
    private final Map<UUID, IWeapon> clicked = new HashMap<>();

    @Inject
    public WeaponListener(WeaponManager weaponManager, ItemHandler itemHandler, CooldownManager cooldownManager, EnergyHandler energyHandler) {
        this.weaponManager = weaponManager;
        this.itemHandler = itemHandler;
        this.cooldownManager = cooldownManager;
        this.energyHandler = energyHandler;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRelease(RightClickEndEvent event) {
        clicked.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStart(RightClickEvent event) {
        if (clicked.containsKey(event.getPlayer().getUniqueId())) {
            final IWeapon weapon = clicked.get(event.getPlayer().getUniqueId());
            if (weapon instanceof ChannelWeapon channelWeapon && channelWeapon.useShield(event.getPlayer())) {
                event.setUseShield(true);
                event.setShieldModelData(RightClickEvent.INVISIBLE_SHIELD);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeaponActivate(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return; // Only main hand and right click
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return;
        if (!itemMeta.getPersistentDataContainer().has(CoreNamespaceKeys.CUSTOM_ITEM_KEY)) return;

        Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(item);
        if (weaponOptional.isEmpty()) return;

        IWeapon weapon = weaponOptional.get();
        if (weapon instanceof InteractWeapon interactWeapon) {
            if (!interactWeapon.canUse(player)) {
                return;
            }
        }

        var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, weapon, true));
        if (checkUsageEvent.isCancelled()) {
            UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
            return;
        }

        String name = PlainTextComponentSerializer.plainText().serialize(weapon.getName());
        if (weapon instanceof CooldownWeapon cooldownWeapon) {
            if (!cooldownManager.use(player, name, cooldownWeapon.getCooldown(),
                    cooldownWeapon.showCooldownFinished(), true, false, x -> weapon.isHoldingWeapon(player))) {
                return;
            }
        }

        if (weapon instanceof ChannelWeapon channelWeapon) {
            if (clicked.get(event.getPlayer().getUniqueId()) == weapon) {
                return; // Skip if we're currently holding click on this weapon
            }

            if (channelWeapon.getEnergy() > 0) {
                if (!energyHandler.use(player, name, channelWeapon.getEnergy(), true)) {
                    return;
                }
            }

            clicked.put(player.getUniqueId(), weapon); // Log this weapon as clicked
        }

        if (weapon instanceof InteractWeapon interactWeapon) {
            interactWeapon.activate(player);
        }

    }

    @EventHandler
    public void onUpdateName(ItemUpdateNameEvent event) {
        Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(event.getItemStack());
        if (weaponOptional.isPresent()) {
            IWeapon weapon = weaponOptional.get();
            if(!(weapon instanceof BPVPItem item)) return;

            event.getItemMeta().getPersistentDataContainer().set(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING, item.getIdentifier());
            event.setItemName(weapon.getName());
        }
    }

    @EventHandler
    public void onUpdateLore(ItemUpdateLoreEvent event) {
        Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(event.getItemStack());
        if (weaponOptional.isPresent()) {
            IWeapon weapon = weaponOptional.get();
            if(!(weapon instanceof BPVPItem item)) return;

            event.getItemMeta().getPersistentDataContainer().set(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING, item.getIdentifier());
            var lore = new ArrayList<>(weapon.getLore());

            var originalOwner = event.getItemMeta().getPersistentDataContainer().getOrDefault(ChampionsNamespacedKeys.ORIGINAL_OWNER, PersistentDataType.STRING, "");
            if (!originalOwner.isEmpty()) {
                lore.add(Component.text(""));
                lore.add(Component.text("Original Owner: ", NamedTextColor.WHITE).append(Component.text(originalOwner, NamedTextColor.YELLOW)));
            }

            event.setItemLore(lore);
        }
    }

    @EventHandler
    public void onPickupWeapon(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var itemStack = event.getItem().getItemStack();
        Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(event.getItem().getItemStack());
        if (weaponOptional.isPresent()) {
            IWeapon weapon = weaponOptional.get();
            if (weapon instanceof LegendaryWeapon) {
                var itemMeta = itemStack.getItemMeta();
                if (itemMeta == null) return;

                if (!itemMeta.getPersistentDataContainer().has(ChampionsNamespacedKeys.ORIGINAL_OWNER)) {
                    itemMeta.getPersistentDataContainer().set(ChampionsNamespacedKeys.ORIGINAL_OWNER, PersistentDataType.STRING, player.getName());
                }

                itemStack.setItemMeta(itemMeta);
                event.getItem().setItemStack(itemStack);
            }
        }
    }

    @EventHandler
    public void onSpecialItemDrop(SpecialItemDropEvent event) {
        var itemStack = itemHandler.updateNames(event.getItem().getItemStack());
        Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(itemStack);
        if (weaponOptional.isPresent()) {
            IWeapon weapon = weaponOptional.get();
            if (!(weapon instanceof LegendaryWeapon)) return;
            UtilMessage.broadcast(Component.text(event.getSource(), NamedTextColor.RED)
                    .append(Component.text(" dropped a legendary ", NamedTextColor.GRAY))
                    .append(weapon.getName().hoverEvent(itemStack)));
        }
    }

    @EventHandler
    public void onCombatLog(PlayerCombatLogEvent event) {
        for (ItemStack itemStack : event.getPlayer().getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;
            Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(itemStack);
            if (weaponOptional.isPresent()) {
                IWeapon weapon = weaponOptional.get();
                if (!(weapon instanceof LegendaryWeapon)) return;

                event.setSafe(false);
                event.setDuration(System.currentTimeMillis()); // Permanent combat log
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (IWeapon weapon : weaponManager.getObjects().values()) {
            if (weapon instanceof BPVPItem item) {
                item.getRecipeKeys().forEach(key -> event.getPlayer().discoverRecipe(key));
            }
        }
    }
}
