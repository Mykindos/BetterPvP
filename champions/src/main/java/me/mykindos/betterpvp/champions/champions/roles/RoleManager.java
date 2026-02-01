package me.mykindos.betterpvp.champions.champions.roles;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.champions.champions.roles.packet.ArmorProtocol;
import me.mykindos.betterpvp.champions.champions.roles.packet.RemapperIn;
import me.mykindos.betterpvp.champions.champions.roles.packet.RemapperOut;
import me.mykindos.betterpvp.champions.properties.ChampionsProperty;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.champions.RoleStat;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;


@Singleton
@Getter
public class RoleManager {

    private final RoleRepository repository;
    private final ClientManager clientManager;
    private final EntityHealthService entityHealthService;
    private final ItemFactory itemFactory;
    private final Map<LivingEntity, Role> store = new WeakHashMap<>();
    public static final Map<Role, ArrayList<RoleEffect>> rolePassiveDescs = new EnumMap<>(Role.class);

    @Inject
    private RoleManager(RoleRepository repository, ClientManager clientManager, EntityHealthService entityHealthService, ItemFactory itemFactory, ArmorProtocol armorProtocol) {
        this.repository = repository;
        this.clientManager = clientManager;
        this.entityHealthService = entityHealthService;
        this.itemFactory = itemFactory;

        PacketEvents.getAPI().getEventManager().registerListener(new RemapperIn(armorProtocol), PacketListenerPriority.LOWEST);
        PacketEvents.getAPI().getEventManager().registerListener(new RemapperOut(this), PacketListenerPriority.LOWEST);
    }

    private void updateRole(@NotNull LivingEntity livingEntity, @NotNull Role role) {
        entityHealthService.setBaseHealth(livingEntity, role.getHealth());
        store.put(livingEntity, role);
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
     * Populate a living entity with their last role, or default
     * @param entity The living entity to populate
     */
    public void populate(@NotNull LivingEntity entity) {
        if (entity instanceof Player player && player.isOnline()) { // Players have their last role saved
            final Optional<String> property = clientManager.search().online(player).getGamer().getProperty(ChampionsProperty.CURRENT_ROLE);
            property.ifPresentOrElse(roleName -> {
                final Role role = Role.valueOf(roleName.toUpperCase());
                updateRole(entity, role);
            }, () -> updateRole(entity, Role.DEFAULT));
        } else {
            updateRole(entity, Role.DEFAULT);
        }
    }

    /**
     * Equip a role to a living entity
     * @param livingEntity The living entity
     * @param role The role to equip
     * @return True if the role was successfully equipped, or false if the role change was cancelled
     */
    public boolean equipRole(@NotNull LivingEntity livingEntity, @NotNull Role role) {
        final Role previous = getRole(livingEntity).orElse(null);
        if (previous != role) {
            final RoleChangeEvent roleChangeEvent = new RoleChangeEvent(livingEntity, role, previous);
            roleChangeEvent.callEvent();
            if (roleChangeEvent.isCancelled()) {
                return false;
            }

            updateRole(livingEntity, role);
            if (livingEntity instanceof Player player) {
                final Client client = clientManager.search().online(player);
                client.getGamer().saveProperty(ChampionsProperty.CURRENT_ROLE, role.name());
                final RoleStat roleStat = RoleStat.builder()
                        .role(role)
                        .action(RoleStat.Action.EQUIP)
                        .build();
                client.getStatContainer().incrementStat(roleStat, 1);
            }
        }
        return true;
    }

    /**
     * Get the role equipped by a living entity
     * @param livingEntity The living entity
     * @return The role equipped by the living entity
     */
    public @NotNull Optional<Role> getRole(@NotNull LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) return Optional.of(getRole(player));
        return Optional.ofNullable(store.get(livingEntity));
    }

    public @NotNull Role getRole(@NotNull Player player) {
        return store.computeIfAbsent(player, p -> Role.DEFAULT);
    }

    /**
     * Check if a living entity has a specific role equipped
     * @param livingEntity The living entity
     * @param role The role
     * @return True if the living entity has the target role equipped
     */
    public boolean hasRole(LivingEntity livingEntity, Role role) {
        return getRole(livingEntity).orElse(null) == role;
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

    private ItemStack getItem(ItemStack itemStack) {
        return itemFactory.create(itemFactory.getFallbackItem(itemStack)).createItemStack();
    }

    private ItemStack getItem(Material material) {
        return getItem(new ItemStack(material));
    }
}
