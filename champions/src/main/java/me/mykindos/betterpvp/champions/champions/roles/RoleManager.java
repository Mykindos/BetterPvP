package me.mykindos.betterpvp.champions.champions.roles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeCause;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.champions.item.component.armor.RoleArmorResolver;
import me.mykindos.betterpvp.champions.properties.ChampionsProperty;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.champions.RoleStat;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;


/**
 * The source of truth for which role every living entity has equipped.
 * <p>
 * An entity has a role only while it wears the complete armor set for it, so the store is kept in sync
 * by {@link me.mykindos.betterpvp.champions.champions.roles.listeners.RoleArmorListener} whenever equipment changes.
 */
@Singleton
@Getter
public class RoleManager {

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET);

    private final RoleRepository repository;
    private final ClientManager clientManager;
    private final EntityHealthService entityHealthService;
    private final ItemFactory itemFactory;
    private final RoleArmorResolver armorResolver;
    private final Map<LivingEntity, Role> store = new WeakHashMap<>();
    public static final Map<Role, ArrayList<RoleEffect>> rolePassiveDescs = new EnumMap<>(Role.class);

    @Inject
    private RoleManager(RoleRepository repository, ClientManager clientManager,
                        EntityHealthService entityHealthService, ItemFactory itemFactory,
                        RoleArmorResolver armorResolver) {
        this.repository = repository;
        this.clientManager = clientManager;
        this.entityHealthService = entityHealthService;
        this.itemFactory = itemFactory;
        this.armorResolver = armorResolver;
    }

    /**
     * Get all living entities that have a role equipped
     * @return A set of living entities that have a role equipped
     */
    public Set<LivingEntity> getLivingEntities() {
        return Collections.unmodifiableSet(store.keySet());
    }

    /**
     * Clean up the role data for a living entity
     * @param entity The living entity to clean up
     */
    public void cleanUp(@NotNull LivingEntity entity) {
        store.remove(entity);
    }

    /**
     * Re-read the entity's worn armor and make the resolved role its role, clearing the role if the
     * entity is not wearing a complete set.
     *
     * @param entity The living entity to resolve
     * @param cause What prompted the resolution
     */
    public void refreshRole(@NotNull LivingEntity entity, @NotNull RoleChangeCause cause) {
        setRole(entity, armorResolver.resolve(entity), cause);
    }

    /**
     * Equip a role to a living entity
     * @param livingEntity The living entity
     * @param role The role to equip
     * @return True if the role was successfully equipped, or false if the role change was cancelled
     */
    public boolean equipRole(@NotNull LivingEntity livingEntity, @NotNull Role role) {
        return equipRole(livingEntity, role, RoleChangeCause.EXTERNAL);
    }

    public boolean equipRole(@NotNull LivingEntity livingEntity, @NotNull Role role, @NotNull RoleChangeCause cause) {
        if (!setRole(livingEntity, role, cause)) {
            return false;
        }

        applyArmor(livingEntity, role);
        return true;
    }

    /**
     * Get the role equipped by a living entity
     * @param livingEntity The living entity
     * @return The role equipped by the living entity, or empty if it is not wearing a complete set
     */
    public @NotNull Optional<Role> getRole(@NotNull LivingEntity livingEntity) {
        return Optional.ofNullable(store.get(livingEntity));
    }

    /**
     * The last role this player equipped, persisted across sessions. This is not necessarily the role they
     * currently have, which they only hold while wearing its complete armor set.
     *
     * @param player The player
     * @return The last equipped role, or empty if they have never equipped one
     */
    public @NotNull Optional<Role> getLastEquippedRole(@NotNull Player player) {
        final Optional<String> property = clientManager.search().online(player).getGamer().getProperty(ChampionsProperty.CURRENT_ROLE);
        return property.map(String::toUpperCase).map(Role::valueOf);
    }

    /**
     * Check if a living entity has a specific role equipped
     * @param livingEntity The living entity
     * @param role The role
     * @return True if the living entity has the target role equipped
     */
    public boolean hasRole(LivingEntity livingEntity, Role role) {
        return store.get(livingEntity) == role;
    }

    public void equipWeapons(@NotNull HumanEntity humanEntity) {
        if (!humanEntity.getInventory().contains(Material.IRON_SWORD)) {
            humanEntity.getInventory().addItem(getItem(Material.IRON_SWORD));
        }

        if (!humanEntity.getInventory().contains(Material.IRON_AXE)) {
            humanEntity.getInventory().addItem(getItem(Material.IRON_AXE));
        }

        Optional<Role> role = getRole(humanEntity);
        if (role.isEmpty()) {
            return;
        }

        switch (role.get()) {
            case RANGER, ASSASSIN -> {
                if (!humanEntity.getInventory().contains(Material.BOW)) {
                    humanEntity.getInventory().addItem(getItem(Material.BOW));
                }

                final ItemStack arrow = new ItemStack(Material.ARROW);
                final ItemStack arrowItem = getItem(arrow);
                arrowItem.setAmount(role.get() == Role.RANGER ? 64 : 32);
                humanEntity.getInventory().addItem(arrowItem);
            }
        }
    }

    private boolean setRole(@NotNull LivingEntity livingEntity, @Nullable Role role, @NotNull RoleChangeCause cause) {
        final Role previous = store.get(livingEntity);
        if (previous == role) {
            return true;
        }

        final RoleChangeEvent roleChangeEvent = new RoleChangeEvent(livingEntity, role, previous, cause);
        roleChangeEvent.callEvent();
        if (roleChangeEvent.isCancelled()) {
            return false;
        }

        if (role == null) {
            store.remove(livingEntity);
            entityHealthService.resetBaseHealth(livingEntity);
            return true;
        }

        store.put(livingEntity, role);
        entityHealthService.setBaseHealth(livingEntity, role.getHealth());

        if (livingEntity instanceof Player player) {
            final Client client = clientManager.search().online(player);
            client.getGamer().saveProperty(ChampionsProperty.CURRENT_ROLE, role.name());
            final RoleStat roleStat = RoleStat.builder()
                    .role(role)
                    .action(RoleStat.Action.EQUIP)
                    .build();
            client.getStatContainer().incrementStat(roleStat, 1);
        }
        return true;
    }

    /**
     * Dress the entity in a role's armor set, keeping any piece that already belongs to the kit so reinforced
     * variants survive, and handing back whatever it displaces.
     */
    private void applyArmor(@NotNull LivingEntity livingEntity, @NotNull Role role) {
        final EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) {
            return;
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            final ItemStack current = equipment.getItem(slot);
            if (armorResolver.rolesFor(current).contains(role)) {
                continue;
            }

            if (!current.isEmpty()) {
                if (livingEntity instanceof Player player) {
                    UtilItem.insert(player, current);
                } else {
                    livingEntity.getWorld().dropItemNaturally(livingEntity.getLocation(), current);
                }
            }

            equipment.setItem(slot, getItem(role.getMaterial(slot)));
        }
    }

    private ItemStack getItem(ItemStack itemStack) {
        return itemFactory.create(itemFactory.getFallbackItem(itemStack)).createItemStack();
    }

    private ItemStack getItem(Material material) {
        return getItem(new ItemStack(material));
    }
}
