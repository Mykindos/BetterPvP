package me.mykindos.betterpvp.clans.clans.menus;


import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.menus.buttons.*;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class ClanMenu extends Menu {

    private final Player player;
    private final Clan playerClan;
    private final Clan clan;

    public ClanMenu(Player player, Clan playerClan, Clan clan) {
        super(player, 54, Component.text("Clan Menu"));
        this.playerClan = playerClan;
        this.clan = clan;
        this.player = player;

        fillPage();
        construct();
    }

    public void fillPage() {

        addButton(new TerritoryButton(3, player, clan));
        addButton(new EnergyButton(5, player, clan));
        addButton(new AlliesButton(8, playerClan, clan));
        addButton(new ClanButton(4, clan, player, clan.getRelation(playerClan)));

        List<ClanMember> members = clan.getMembers();
        loadPlayerHeads(members);

        // Only add the below buttons if it is the player's clan
        if (clan.getMemberByUUID(player.getUniqueId()).isPresent()) {
            addButton(new EnemiesButton(0, playerClan, clan));
            addButton(new LeaveClanButton(49, clan, player));
            addButton(new ClanCommandButton(53));
            addButton(new ClanVaultButton(6, clan));
            addButton(new ClanUpgradesButton(2,clan));
            List<ClanEnemy> topEnemies = getTopEnemiesByDominance();
            for (int i = 0; i < topEnemies.size(); i++) {
                ClanEnemy enemy = topEnemies.get(i);
                int slot = 9 * (i + 1);
                addButton(new EnemyButton(slot, playerClan, enemy));
            }
        }

    }

    private void loadPlayerHeads(List<ClanMember> members) {
        List<ClanMember> leaders = new ArrayList<>();
        List<ClanMember> admins = new ArrayList<>();
        List<ClanMember> memberRankList = new ArrayList<>();
        List<ClanMember> recruits = new ArrayList<>();

        for (ClanMember member : members) {
            switch (member.getRank()) {
                case LEADER -> leaders.add(member);
                case ADMIN -> admins.add(member);
                case MEMBER -> memberRankList.add(member);
                case RECRUIT -> recruits.add(member);
            }
        }

        if (!leaders.isEmpty()) {
            addMemberToMenu(leaders.get(0), 13);
        }

        // define starting slots for each rank(to make rows)
        int adminStartSlot = 18;
        int memberStartSlot = 27;
        int recruitStartSlot = 36;

        addMembersToRow(admins, adminStartSlot);
        addMembersToRow(memberRankList, memberStartSlot);
        addMembersToRow(recruits, recruitStartSlot);
    }

    private void addMembersToRow(List<ClanMember> rankMembers, int startSlot) {
        int middle = startSlot + 4; // gives the middle slot of a row

        for (int i = 0; i < rankMembers.size(); i++) {
            int offset = (i + 1) / 2; // calculate the offset from the middle

            if (i % 2 == 1) { // check for if odd and then offset to the right
                offset = -offset;
            }

            int slot = middle + offset;
            addMemberToMenu(rankMembers.get(i), slot);
        }
    }

    private void addMemberToMenu(ClanMember member, int slot) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(member.getUuid()));
        if (offlinePlayer.getName() == null) return;

        ItemStack playerHead;
        NamedTextColor displayNameColor;

        if (offlinePlayer.isOnline()) {
            playerHead = new ItemStack(Material.PLAYER_HEAD);
            displayNameColor = clan.getRelation(playerClan).getPrimary();
        } else {
            playerHead = new ItemStack(Material.SKELETON_SKULL);
            displayNameColor = NamedTextColor.GRAY;
        }

        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        skullMeta.setOwningPlayer(offlinePlayer);
        skullMeta.displayName(Component.text(offlinePlayer.getName(), displayNameColor));
        playerHead.setItemMeta(skullMeta);

        List<Component> lore = new ArrayList<>();

        TextColor rankColor = switch (member.getRank()) {
            case RECRUIT -> NamedTextColor.DARK_GRAY;
            case MEMBER -> NamedTextColor.GOLD;
            case ADMIN -> NamedTextColor.RED;
            case LEADER -> NamedTextColor.DARK_RED;
        };
        lore.add(Component.text(member.getRank().getName(), rankColor));
        lore.add(Component.text("K/D/A: " + 0, NamedTextColor.GRAY));
        lore.add(Component.text("Playtime: " + 0, NamedTextColor.GRAY));

        ClanMember viewingMember = clan.getMemberByUUID(player.getUniqueId()).orElse(null);

        if (viewingMember != null) {
            boolean isAdmin = viewingMember.getRank() == ClanMember.MemberRank.ADMIN;
            boolean isLeader = viewingMember.getRank() == ClanMember.MemberRank.LEADER;

            if (isLeader && !member.getUuid().equals(viewingMember.getUuid())) {
                lore.add(Component.text(""));
                lore.add(Component.text("Left click to promote", NamedTextColor.DARK_GRAY));
                lore.add(Component.text("Right click to demote", NamedTextColor.DARK_GRAY));
                lore.add(Component.text("Shift left click to kick", NamedTextColor.DARK_GRAY));
            } else if (isAdmin && !member.getUuid().equals(viewingMember.getUuid()) && (member.getRank() == ClanMember.MemberRank.RECRUIT || member.getRank() == ClanMember.MemberRank.MEMBER)) {
                lore.add(Component.text(""));
                lore.add(Component.text("Left click to promote", NamedTextColor.DARK_GRAY));
                lore.add(Component.text("Right click to demote", NamedTextColor.DARK_GRAY));
                lore.add(Component.text("Shift left click to kick", NamedTextColor.DARK_GRAY));
            }
        }

        ItemMeta itemMeta = playerHead.getItemMeta();
        itemMeta.lore(lore);
        playerHead.setItemMeta(itemMeta);

        Component displayName = Component.text(offlinePlayer.getName(), displayNameColor);

        Button memberButton = new Button(slot, playerHead, displayName, lore) {
            @Override
            public void onClick(Player player, Gamer gamer, ClickType clickType) {
                ClanMember viewingMember = clan.getMemberByUUID(player.getUniqueId()).orElse(null);

                if (viewingMember == null) return;  // Exit if the clicking player isn't a clan member.

                if (clickType.isLeftClick()) {
                    if (viewingMember.getRank() == ClanMember.MemberRank.ADMIN && !member.getUuid().equals(viewingMember.getUuid()) && (member.getRank() == ClanMember.MemberRank.RECRUIT || member.getRank() == ClanMember.MemberRank.MEMBER)) {
                    }
                } else if (clickType.isRightClick()) {
                    if (viewingMember.getRank() == ClanMember.MemberRank.LEADER && !member.getUuid().equals(viewingMember.getUuid()) && member.getRank() != ClanMember.MemberRank.LEADER) {
                        // Demote member logic for leader
                    } else if (viewingMember.getRank() == ClanMember.MemberRank.ADMIN && !member.getUuid().equals(viewingMember.getUuid()) && member.getRank() != ClanMember.MemberRank.LEADER && member.getRank() != ClanMember.MemberRank.ADMIN) {
                        // Demote member logic for admins
                    }
                } else if (clickType.isShiftClick()) {
                    // Kick member logic
                }
            }
        };
        addButton(memberButton);
    }

    public ClanRelation getRelation(UUID uuid){

        if(clan.getMembers().stream().anyMatch(member -> member.getUuid().equals(uuid.toString()))){
            return ClanRelation.SELF;
        }else if(clan.getAlliances().stream().anyMatch(ally -> ally.getClan().getMembers().stream().anyMatch(member -> member.getUuid().equals(uuid.toString())))) {
            return ClanRelation.ALLY;
        }else if(clan.getEnemies().stream().anyMatch(enemy -> enemy.getClan().getMembers().stream().anyMatch(member -> member.getUuid().equals(uuid.toString())))) {
            return ClanRelation.ENEMY;
        }

        return ClanRelation.NEUTRAL;
    }


    private List<ClanEnemy> getTopEnemiesByDominance() {
        List<ClanEnemy> enemies = clan.getEnemies();
        enemies.sort(Comparator.comparingDouble((ClanEnemy e) -> Math.abs(e.getDominance())).reversed());

        return enemies.subList(0, Math.min(5, enemies.size()));
    }


}
