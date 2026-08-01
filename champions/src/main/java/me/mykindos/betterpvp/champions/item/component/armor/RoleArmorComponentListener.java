package me.mykindos.betterpvp.champions.item.component.armor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.armor.ArmorEquipEvent;
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

import java.util.Collections;
import java.util.Set;

@BPvPListener
@Singleton
public class RoleArmorComponentListener implements Listener {

    private final RoleArmorResolver armorResolver;

    @Inject
    private RoleArmorComponentListener(RoleArmorResolver armorResolver) {
        this.armorResolver = armorResolver;
    }

    @EventHandler
    public void onEquip(ArmorEquipEvent event) {
        final Set<Role> roles = armorResolver.rolesFor(event.getItem());
        if (roles.isEmpty()) {
            return; // No restrictions
        }

        // Gate on the sets this piece still leaves reachable rather than the current role, because a set is
        // built one piece at a time and the wearer has no role until it is complete
        final Set<Role> reachable = armorResolver.reachableRoles(event.getPlayer(), event.getArmorSlot(), event.getItem());
        if (Collections.disjoint(roles, reachable)) {
            event.setCancelled(true);
        }
    }

    // If they swap kits, take off their incompatible armor
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRoleChange(RoleChangeEvent event) {
        final Role role = event.getRole();
        if (role == null) {
            return; // Losing a role means an incomplete set, which they are allowed to keep wearing
        }

        final EntityEquipment equipment = event.getLivingEntity().getEquipment();
        if (equipment == null) {
            return;
        }

        final LivingEntity entity = event.getLivingEntity();
        final ItemStack[] armorContents = equipment.getArmorContents();
        for (int i = 0; i < armorContents.length; i++) {
            final ItemStack armorContent = armorContents[i];
            final Set<Role> roles = armorResolver.rolesFor(armorContent);
            if (roles.isEmpty()) {
                continue; // No restrictions
            }

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
