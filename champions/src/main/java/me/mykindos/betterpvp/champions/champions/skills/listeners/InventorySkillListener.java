package me.mykindos.betterpvp.champions.champions.skills.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillUpdateEvent;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.properties.ChampionsProperty;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerEntityEquipment;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerSetSlot;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerWindowItems;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@PluginAdapter("ProtocolLib")
@BPvPListener
@Singleton
public class InventorySkillListener extends PacketAdapter implements Listener {

    private final BuildManager buildManager;
    private final RoleManager roleManager;
    private final Champions champions;
    private final ClientManager clientManager;
    private final CooldownManager cooldownManager;

    @Inject
    private InventorySkillListener(Champions champions, BuildManager buildManager, RoleManager roleManager, ClientManager clientManager, CooldownManager cooldownManager) {
        super(champions, ListenerPriority.HIGHEST,
                PacketType.Play.Server.WINDOW_ITEMS,
                PacketType.Play.Server.SET_SLOT);
        this.clientManager = clientManager;
        this.cooldownManager = cooldownManager;
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
        this.champions = champions;
        this.buildManager = buildManager;
        this.roleManager = roleManager;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        final PacketType type = event.getPacketType();
        final Player receiver = event.getPlayer();

        if(receiver.getOpenInventory().getType() != InventoryType.CRAFTING) return;
        if(type != PacketType.Play.Server.WINDOW_ITEMS && type != PacketType.Play.Server.SET_SLOT && type != PacketType.Play.Server.ENTITY_EQUIPMENT) return;

        Client client = clientManager.search().online(receiver);
        final boolean showTooltips = (boolean) client.getProperty(ChampionsProperty.SKILL_WEAPON_TOOLTIP).orElse(false);
        if(!showTooltips) {
            return;
        }

        // Its pretty easy to spam these packets, so we will limit it to 1 per second. Should have no negative impact
        if(!cooldownManager.use(receiver, "SkillWeaponTooltip", 1, false)) {
            return;
        }

        if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event.getPacket());
            packet.setCarriedItem(addLore(packet.getCarriedItem(), receiver));
            packet.setItems(addLore(packet.getItems(), receiver));
        } else if (type == PacketType.Play.Server.SET_SLOT) {
            final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event.getPacket());
            packet.setItemStack(addLore(packet.getItemStack(), receiver));
        } else if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            final WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event.getPacket());
            final List<Pair<EnumWrappers.ItemSlot, ItemStack>> slots = packet.getSlots();
            for (Pair<EnumWrappers.ItemSlot, ItemStack> slot : slots) {
                slot.setSecond(addLore(slot.getSecond(), receiver));
            }

            packet.setSlots(slots);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRoleChange(RoleChangeEvent event) {
        updateInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSkillEquip(SkillEquipEvent event) {
        updateInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSkillUpdate(SkillUpdateEvent event) {
        updateInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBuildApply(ApplyBuildEvent event) {
        updateInventory(event.getPlayer());
    }
    
    private void updateInventory(Player player) {
        UtilServer.runTaskLater(champions, () -> {
            if(player.getItemOnCursor().getType() == Material.AIR) {
                player.updateInventory();
            }
        }, 2L);
    }

    private List<ItemStack> addLore(Collection<ItemStack> items, Player player) {

        final List<ItemStack> newItems = new ArrayList<>();
        for (ItemStack itemStack : items) {
            newItems.add(addLore(itemStack, player));
        }
        return newItems;
    }

    private ItemStack addLore(ItemStack itemStack, Player player) {
        final Optional<Role> roleOpt = this.roleManager.getObject(player.getUniqueId());
        if (roleOpt.isEmpty()) {
            return itemStack; // No role
        }

        final Optional<GamerBuilds> buildOpt = this.buildManager.getObject(player.getUniqueId());
        if (buildOpt.isEmpty()) {
            return itemStack; // No build
        }

        final RoleBuild roleBuild = buildOpt.get().getActiveBuilds().get(roleOpt.get().getName());
        if (roleBuild == null) {
            return itemStack; // No role build
        }

        final SkillType type = SkillWeapons.getTypeFrom(itemStack);
        if (type == null) {
            return itemStack; // Not a skill item
        }

        final BuildSkill buildSkill = roleBuild.getBuildSkill(type);
        if (buildSkill == null || buildSkill.getSkill() == null) {
            return itemStack; // No skill
        }

        int level = buildSkill.getLevel();
        final Skill skill = buildSkill.getSkill();

        boolean boosted = SkillWeapons.isBooster(itemStack.getType());
        if (boosted) {
            level++;
        }

        final ItemStack clone = itemStack.clone();
        final ItemMeta meta = clone.getItemMeta();
        final List<Component> lore = Objects.requireNonNullElse(meta.lore(), new ArrayList<>());
        lore.add(Component.empty());
        lore.add(UtilMessage.DIVIDER);
        lore.add(buildSkill.getComponent(boosted).decoration(TextDecoration.ITALIC, false));
        lore.addAll(Arrays.stream(skill.parseDescription(level)).toList());
        lore.add(UtilMessage.DIVIDER);
        meta.lore(lore);
        clone.setItemMeta(meta);
        return clone;
    }
}
