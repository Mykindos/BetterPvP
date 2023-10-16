package me.mykindos.betterpvp.champions.weapons.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.champions.utilities.ChampionsNamespacedKeys;
import me.mykindos.betterpvp.champions.weapons.WeaponManager;
import me.mykindos.betterpvp.champions.weapons.types.ChannelWeapon;
import me.mykindos.betterpvp.champions.weapons.types.CooldownWeapon;
import me.mykindos.betterpvp.champions.weapons.types.InteractWeapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateNameEvent;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemDropEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

@Singleton
@BPvPListener
public class WeaponListener implements Listener {

    private final WeaponManager weaponManager;
    private final ItemHandler itemHandler;
    private final CooldownManager cooldownManager;
    private final EnergyHandler energyHandler;

    @Inject
    public WeaponListener(WeaponManager weaponManager, ItemHandler itemHandler, CooldownManager cooldownManager, EnergyHandler energyHandler) {
        this.weaponManager = weaponManager;
        this.itemHandler = itemHandler;
        this.cooldownManager = cooldownManager;
        this.energyHandler = energyHandler;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeaponActivate(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return;
        if (!itemMeta.getPersistentDataContainer().has(ChampionsNamespacedKeys.IS_CUSTOM_WEAPON)) return;

        Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(item);
        if (weaponOptional.isEmpty()) return;


        IWeapon weapon = weaponOptional.get();

        if (weapon instanceof InteractWeapon interactWeapon) {
            if (Arrays.stream(interactWeapon.getActions()).noneMatch(action -> action == event.getAction())) return;
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
            if (channelWeapon.getEnergy() > 0) {
                if (!energyHandler.use(player, name, channelWeapon.getEnergy(), true)) {
                    return;
                }
            }

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

            event.getItemMeta().getPersistentDataContainer().set(ChampionsNamespacedKeys.IS_CUSTOM_WEAPON, PersistentDataType.STRING, "true");
            event.setItemName(weapon.getName());
        }
    }

    @EventHandler
    public void onUpdateLore(ItemUpdateLoreEvent event) {
        Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(event.getItemStack());
        if (weaponOptional.isPresent()) {
            IWeapon weapon = weaponOptional.get();

            event.getItemMeta().getPersistentDataContainer().set(ChampionsNamespacedKeys.IS_CUSTOM_WEAPON, PersistentDataType.STRING, "true");
            var lore = new ArrayList<>(weapon.getLore());

            var originalOwner = event.getItemMeta().getPersistentDataContainer().getOrDefault(ChampionsNamespacedKeys.ORIGINAL_OWNER, PersistentDataType.STRING, "");
            if(!originalOwner.equals("")) {
                lore.add(Component.text(""));
                lore.add(Component.text("Original Owner: ", NamedTextColor.WHITE).append(Component.text(originalOwner, NamedTextColor.YELLOW)));
            }

            event.setItemLore(lore);
        }
    }

    @EventHandler
    public void onPickupWeapon(EntityPickupItemEvent event) {
        if(!(event.getEntity() instanceof Player player)) return;
        var itemStack = event.getItem().getItemStack();
        Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(event.getItem().getItemStack());
        if (weaponOptional.isPresent()) {
            IWeapon weapon = weaponOptional.get();
            if(weapon instanceof LegendaryWeapon) {
                var itemMeta = itemStack.getItemMeta();
                if(itemMeta == null) return;

                if(!itemMeta.getPersistentDataContainer().has(ChampionsNamespacedKeys.ORIGINAL_OWNER)) {
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
        if(weaponOptional.isPresent()) {
            IWeapon weapon = weaponOptional.get();
            if(!(weapon instanceof LegendaryWeapon)) return;
            UtilMessage.broadcast(Component.text(event.getSource(), NamedTextColor.RED)
                    .append(Component.text(" dropped a legendary ", NamedTextColor.GRAY))
                    .append(weapon.getName().hoverEvent(itemStack)));
        }
    }

}
