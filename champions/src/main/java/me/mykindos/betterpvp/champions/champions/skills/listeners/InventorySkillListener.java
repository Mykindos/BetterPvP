package me.mykindos.betterpvp.champions.champions.skills.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillUpdateEvent;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
@BPvPListener
public class InventorySkillListener implements Listener {

    private final Champions champions;
    private final BuildManager buildManager;
    private final RoleManager roleManager;
    private final ItemHandler itemHandler;

    @Inject
    public InventorySkillListener(Champions champions, BuildManager buildManager, RoleManager roleManager, ItemHandler itemHandler) {
        this.champions = champions;
        this.buildManager = buildManager;
        this.roleManager = roleManager;
        this.itemHandler = itemHandler;
    }


    @EventHandler
    public void onInventoryPickup(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            boolean isPlayerInventory = event.getClickedInventory() != null && event.getClickedInventory().getType().equals(InventoryType.PLAYER);
            if (event.getAction().name().contains("PICKUP")) {
                processItem(player, isPlayerInventory , event.getCurrentItem());
            } else if (event.getAction().name().contains("PLACE")) {
                processItem(player, isPlayerInventory, event.getCursor());
            } else if (event.getAction().name().contains("HOTBAR")) {
                if (!isPlayerInventory) {
                    processItem(player, true, event.getCurrentItem());
                    UtilServer.runTaskLater(champions, false, () -> processItem(player, false, event.getClickedInventory().getItem(event.getSlot())), 1);
                }
            } else if (event.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) {
                processItem(player, isPlayerInventory, event.getCurrentItem());
                processItem(player, isPlayerInventory, event.getCursor());
            } else if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                processItem(player, !isPlayerInventory, event.getCurrentItem());
            }

        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Player player) {
            //updateName overrides, so need to run this after
            ItemStack item = event.getItem().getItemStack();
            if (UtilItem.isAxe(item) || UtilItem.isRanged(item) || UtilItem.isSword(item))
            {
                //need to check the whole inventory, for reasons beyond my comprehension
                UtilServer.runTaskLater(champions, () -> player.getInventory().forEach(itemStack -> processItem(player, true, itemStack)), 1);
            }

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(EntityDropItemEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Player player) {
            processItem(player, false, event.getItemDrop().getItemStack());
        }
    }

    @EventHandler
    public void onRoleChange(RoleChangeEvent event) {
        event.getPlayer().getInventory().forEach(itemStack -> processItem(event.getPlayer(), true, itemStack));
    }

    @EventHandler
    public void onSkillEquip(SkillEquipEvent event) {
        event.getPlayer().getInventory().forEach(itemStack -> processItem(event.getPlayer(), true, itemStack));
    }

    @EventHandler
    public void onSkillUpdate(SkillUpdateEvent event) {
        event.getPlayer().getInventory().forEach(itemStack -> processItem(event.getPlayer(), true, itemStack));
    }

    @EventHandler
    public void onBuildApply(ApplyBuildEvent event) {
        event.getPlayer().getInventory().forEach(itemStack -> processItem(event.getPlayer(), true, itemStack));
    }

    @EventHandler
    public void onClientJoin (PlayerLoginEvent event) {
        UtilServer.runTaskLater(champions, () -> event.getPlayer().getInventory().forEach(itemStack -> processItem(event.getPlayer(), true, itemStack)), 40);
    }

    public void processItem(Player player, boolean playerInventory, ItemStack itemStack) {
        if (itemStack == null) {
            return;
        }
        SkillType skillType = SkillWeapons.getTypeFrom(itemStack);
        if (skillType == null) {
            return;
        }
        BPvPItem item = itemHandler.getItem(itemStack);
        if (item == null) {
            //expect that all items are BPvPItems that we want to alter
            return;
        }
        if (playerInventory) {
            //player is placing this item, it needs to be updated
            Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId());
            if (roleOptional.isEmpty()) {
                return;
            }
            Role role = roleOptional.get();
            Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId());
            if (gamerBuildsOptional.isEmpty()) {
                return;
            }
            GamerBuilds gamerBuilds = gamerBuildsOptional.get();
            BuildSkill buildSkill = gamerBuilds.getActiveBuilds().get(role.getName()).getBuildSkill(skillType);
            if (buildSkill == null) {
                itemHandler.updateNames(itemStack);
                return;
            }
            ItemMeta itemMeta = itemStack.getItemMeta();
            ItemUpdateLoreEvent itemUpdateLoreEvent = new ItemUpdateLoreEvent(itemStack, itemMeta, new ArrayList<>(item.getLore()));
            UtilServer.callEvent(itemUpdateLoreEvent);

            itemUpdateLoreEvent.getItemLore().addAll(getSkillComponent(buildSkill, itemStack));

            item.applyLore(itemMeta, itemUpdateLoreEvent.getItemLore());
            itemStack.setItemMeta(itemMeta);
        } else {
            itemHandler.updateNames(itemStack);
        }
    }

    private List<Component> getSkillComponent(BuildSkill buildSkill, ItemStack itemStack) {
        int level = buildSkill.getLevel();
        if (SkillWeapons.isBooster(itemStack.getType())) {
            level++;
        }
        List<Component> components = new ArrayList<>();
        components.add(UtilMessage.DIVIDER);
        components.add(UtilMessage.deserialize("<yellow>%s</yellow> (<green>%s</green>)", buildSkill.getSkill().getName(), level));
        for (String str : buildSkill.getSkill().getDescription(level)) {
            components.add(MiniMessage.miniMessage().deserialize("<gray>" + str, SkillMenu.TAG_RESOLVER));
        }
        components.add(UtilMessage.DIVIDER);
        return components;
    }

}
