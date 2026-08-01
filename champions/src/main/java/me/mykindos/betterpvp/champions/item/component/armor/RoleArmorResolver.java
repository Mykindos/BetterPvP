package me.mykindos.betterpvp.champions.item.component.armor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reads {@link RoleArmorComponent} off worn armor to answer which kit a set of armor belongs to.
 */
@Singleton
public class RoleArmorResolver {

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET);

    private final ComponentLookupService lookupService;

    @Inject
    private RoleArmorResolver(ComponentLookupService lookupService) {
        this.lookupService = lookupService;
    }

    /**
     * The roles an armor piece may be worn as. A piece with no {@link RoleArmorComponent} belongs to no kit.
     *
     * @param item The armor piece
     * @return The roles it belongs to
     */
    public @NotNull Set<Role> rolesFor(@Nullable ItemStack item) {
        return lookupService.getComponent(item, RoleArmorComponent.class)
                .map(RoleArmorComponent::getRoles)
                .orElse(Set.of());
    }

    /**
     * The role an entity's armor identifies it as. Every armor slot must be filled and every piece must
     * name the same role, so a partial or mixed set belongs to no kit.
     *
     * @param livingEntity The living entity
     * @return The resolved role, or null if its armor does not form a single complete set
     */
    public @Nullable Role resolve(@NotNull LivingEntity livingEntity) {
        final EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) {
            return null;
        }

        final Set<Role> shared = EnumSet.allOf(Role.class);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            final ItemStack worn = equipment.getItem(slot);
            if (worn.isEmpty()) {
                return null; // An incomplete set is no set at all
            }

            shared.retainAll(rolesFor(worn));
            if (shared.isEmpty()) {
                return null;
            }
        }

        return shared.iterator().next();
    }

    /**
     * The roles an entity could still complete a set for, treating {@code slot} as holding {@code item}.
     * Empty slots rule nothing out, so a partially dressed entity keeps every set it is still building towards.
     *
     * @param livingEntity The living entity
     * @param slot The slot to substitute
     * @param item The item to substitute into that slot
     * @return The set of reachable roles
     */
    public @NotNull Set<Role> reachableRoles(@NotNull LivingEntity livingEntity, @NotNull EquipmentSlot slot, @NotNull ItemStack item) {
        final EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) {
            return Set.of();
        }

        final Set<Role> shared = EnumSet.allOf(Role.class);
        for (EquipmentSlot armorSlot : ARMOR_SLOTS) {
            final ItemStack worn = armorSlot == slot ? item : equipment.getItem(armorSlot);
            if (worn.isEmpty()) {
                continue;
            }

            shared.retainAll(rolesFor(worn));
        }
        return shared;
    }

}
