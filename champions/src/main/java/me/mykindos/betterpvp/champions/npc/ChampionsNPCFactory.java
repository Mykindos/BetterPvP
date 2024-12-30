package me.mykindos.betterpvp.champions.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mcmonkey.sentinel.SentinelTrait;

import java.util.Collection;
import java.util.Set;

/**
 * Factory for creating Champions NPCs.
 *
 * @see NPCFactory
 */
@Singleton
public class ChampionsNPCFactory extends NPCFactory {

    private final ClientManager clientManager;
    private final ItemHandler itemHandler;
    private final RoleManager roleManager;
    private final ChampionsSkillManager skillManager;
    private final BuildManager buildManager;

    @Inject
    public ChampionsNPCFactory(ClientManager clientManager, ItemHandler itemHandler, RoleManager roleManager, ChampionsSkillManager skillManager, BuildManager buildManager) {
        super(CitizensAPI.createInMemoryNPCRegistry("champions"));
        this.clientManager = clientManager;
        this.itemHandler = itemHandler;
        this.roleManager = roleManager;
        this.skillManager = skillManager;
        this.buildManager = buildManager;
    }

    @Override
    public NPC spawnDefault(@NotNull Location location, @NotNull String name) {
        return this.spawnNPC(location, name, Set.of(Role.ASSASSIN));
    }

    @Override
    public NPC spawnNPC(@NotNull Location location, @NotNull String name, @NotNull Collection<@NotNull Enum<?>> options) {
        final Role role = options.stream()
                .filter(Role.class::isInstance)
                .map(Role.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Role option is required"));

        final NPC npc = registry.createNPC(EntityType.PLAYER, name);

        // Combat
        final SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
        sentinel.respawnTime = -1; // Disable respawn
        sentinel.addTarget("players");
        sentinel.attackRate = 1;
        sentinel.attackRateRanged = 4;

        // Equipment
        final Equipment equipment = npc.getOrAddTrait(Equipment.class);
        equipment.set(Equipment.EquipmentSlot.HELMET, itemHandler.updateNames(ItemStack.of(role.getHelmet())));
        equipment.set(Equipment.EquipmentSlot.CHESTPLATE, itemHandler.updateNames(ItemStack.of(role.getChestplate())));
        equipment.set(Equipment.EquipmentSlot.LEGGINGS, itemHandler.updateNames(ItemStack.of(role.getLeggings())));
        equipment.set(Equipment.EquipmentSlot.BOOTS, itemHandler.updateNames(ItemStack.of(role.getBoots())));
        equipment.set(Equipment.EquipmentSlot.HAND, itemHandler.updateNames(ItemStack.of(Material.DIAMOND_SWORD)));

        npc.spawn(location);

        // Load it into the client cache so the server can recognize this as a player
        final Player player = (Player) npc.getEntity();
        clientManager.loadInMemory(player);

        // Build
        roleManager.addObject(player.getUniqueId(), role);
        final GamerBuilds builds = new GamerBuilds(player.getUniqueId().toString());
        final RoleBuild build = new RoleBuild(player.getUniqueId().toString(), Role.ASSASSIN, 0);
        build.setSkill(SkillType.PASSIVE_B, skillManager.getObject("Silencing Strikes").orElseThrow(), 3);
        builds.getActiveBuilds().put(role.getName(), build);
        new ApplyBuildEvent(player, builds, null, build).callEvent();
        buildManager.addObject(player.getUniqueId(), builds);
        return npc;
    }

}
