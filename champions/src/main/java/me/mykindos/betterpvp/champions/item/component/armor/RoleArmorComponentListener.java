package me.mykindos.betterpvp.champions.item.component.armor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.armor.ArmorEquipEvent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;

@BPvPListener
@Singleton
public class RoleArmorComponentListener implements Listener {

    private final ComponentLookupService lookupService;
    private final RoleManager roleManager;

    @Inject
    private RoleArmorComponentListener(ComponentLookupService lookupService, RoleManager roleManager) {
        this.lookupService = lookupService;
        this.roleManager = roleManager;
    }

    @EventHandler
    public void onEquip(ArmorEquipEvent event) {
        final Optional<RoleArmorComponent> opt = lookupService.getComponent(event.getItem(), RoleArmorComponent.class);
        if (opt.isEmpty()) {
            return; // No restrictions
        }

        final RoleArmorComponent component = opt.get();
        final Set<Role> roles = component.getRoles();
        final Role role = roleManager.getRole(event.getPlayer());
        if (!roles.contains(role)) {
            event.setCancelled(true);
        }
    }

    // If they swap kits, take off their incompatible armor
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRoleChange(RoleChangeEvent event) {
        final EntityEquipment equipment = event.getLivingEntity().getEquipment();
        if (equipment == null) {
            return;
        }

        final LivingEntity entity = event.getLivingEntity();
        final Role role = event.getRole();
        final ItemStack[] armorContents = equipment.getArmorContents();
        for (int i = 0; i < armorContents.length; i++) {
            final ItemStack armorContent = armorContents[i];
            final Optional<RoleArmorComponent> opt = lookupService.getComponent(armorContent, RoleArmorComponent.class);
            if (opt.isEmpty()) {
                continue; // No restrictions
            }

            final RoleArmorComponent component = opt.get();
            final Set<Role> roles = component.getRoles();
            if (!roles.contains(role)) {
                armorContents[i] = null; // Remove incompatible armor

                if (entity instanceof Player player) {
                    UtilItem.insert(player, armorContent); // If it's a player, give
                } else {
                    entity.getWorld().dropItemNaturally(entity.getLocation(), armorContent);
                }
                new SoundEffect(Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.4f, 0.8f).play(entity);
                new SoundEffect(Sound.ENTITY_ITEM_PICKUP, 0.4f, 0.5f).play(entity);
            }
        }

        equipment.setArmorContents(armorContents);
    }

}
