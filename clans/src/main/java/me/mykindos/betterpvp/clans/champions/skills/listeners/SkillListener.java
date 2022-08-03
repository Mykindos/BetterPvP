package me.mykindos.betterpvp.clans.champions.skills.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.champions.builds.BuildSkill;
import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.roles.RoleManager;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.champions.skills.events.PlayerUseInteractSkillEvent;
import me.mykindos.betterpvp.clans.champions.skills.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.clans.champions.skills.events.PlayerUseToggleSkillEvent;
import me.mykindos.betterpvp.clans.champions.skills.types.*;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.energy.EnergyHandler;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Optional;


@BPvPListener
public class SkillListener implements Listener {

    private final GamerManager gamerManager;
    private final RoleManager roleManager;
    private final CooldownManager cooldownManager;
    private final ClanManager clanManager;
    private final EnergyHandler energyHandler;
    private final EffectManager effectManager;


    @Inject
    public SkillListener(GamerManager gamerManager, RoleManager roleManager,
                         CooldownManager cooldownManager, ClanManager clanManager,
                         EnergyHandler energyHandler, EffectManager effectManager) {
        this.gamerManager = gamerManager;
        this.roleManager = roleManager;
        this.cooldownManager = cooldownManager;
        this.clanManager = clanManager;
        this.energyHandler = energyHandler;
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerUseSkill(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Skill skill = event.getSkill();
        int level = event.getLevel();

        if (!clanManager.canCast(player)) return;

        if (hasNegativeEffect(player)) return;

        if (!skill.canUse(player)) return;

        if (skill instanceof CooldownSkill cooldownSkill) {
            if (!cooldownManager.add(player, skill.getName(), cooldownSkill.getCooldown(level),
                    cooldownSkill.showCooldownFinished(), true, cooldownSkill.isCancellable())) {
                return;
            }
        }

        if (skill instanceof EnergySkill energySkill) {
            if (energySkill.getEnergy(level) > 0) {
                if (!energyHandler.use(player, skill.getName(), energySkill.getEnergy(level), true)) {
                    return;
                }
            }

        }

        if (skill instanceof InteractSkill interactSkill) {
            interactSkill.activate(player, level);
        } else if (skill instanceof ToggleSkill toggleSkill) {
            toggleSkill.toggle(player, level);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {

        Player player = e.getPlayer();
        Material material = player.getInventory().getItemInMainHand().getType();
        if (material == Material.AIR) return;

        Material droppedItem = e.getItemDrop().getItemStack().getType();
        if (!Arrays.asList(SkillWeapons.AXES).contains(droppedItem) && !Arrays.asList(SkillWeapons.SWORDS).contains(droppedItem)) {
            return;
        }

        //if(Polymorph.polymorphed.containsKey(player)){
        //    return;
        //}

        Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId().toString());
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();

            Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
            if (gamerOptional.isPresent()) {
                Gamer gamer = gamerOptional.get();

                RoleBuild build = gamer.getActiveBuilds().get(role.getName());
                if (build == null) return;

                Skill skill = build.getPassiveB().getSkill();
                if (!(skill instanceof ToggleSkill)) return;


                int level = getLevel(player, build.getBuildSkill(SkillType.PASSIVE_B));

                UtilServer.callEvent(new PlayerUseToggleSkillEvent(player, skill, level));

            }
        }

    }


    @EventHandler
    public void onSkillActivate(PlayerInteractEvent event) {

        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();

        Material mainHand = player.getInventory().getItemInMainHand().getType();

        SkillType skillType = getSkillTypeByWeapon(mainHand);
        if (skillType == null) return;

        if (mainHand != Material.BOW & mainHand != Material.CROSSBOW) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                return;
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (UtilBlock.usable(event.getClickedBlock())) return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
                if (block.getType().name().contains("SPONGE")) {
                    // Only cancel if the sponge is below the player
                    if (block.getLocation().getY() < player.getLocation().getY()) {
                        return;
                    }
                } else if (block.getType().name().contains("DOOR")) {
                    return;
                }
            }
        }

        Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId().toString());
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();

            Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
            if (gamerOptional.isPresent()) {
                Gamer gamer = gamerOptional.get();

                RoleBuild build = gamer.getActiveBuilds().get(role.getName());
                if (build == null) return;

                Optional<Skill> skillOptional = build.getActiveSkills().stream().filter(skill -> {
                    return skill instanceof InteractSkill && skill.getSkillType() == skillType;
                }).findFirst();

                if (skillOptional.isPresent()) {
                    Skill skill = skillOptional.get();

                    int level = getLevel(player, build.getBuildSkill(skillType));

                    UtilServer.callEvent(new PlayerUseInteractSkillEvent(player, skill, level));

                }

            }

        }

    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onUseSkillDisabled(PlayerUseSkillEvent event) {
        Player player = event.getPlayer();
        Skill skill = event.getSkill();

        if (!skill.isEnabled()) {
            UtilMessage.message(player, skill.getClassType().getName(), ChatColor.GREEN + skill.getName() + ChatColor.GRAY + " has been disabled by the server.");
            event.setCancelled(true);

        }
    }

    @EventHandler
    public void onUseSkillWhileSlowed(PlayerUseInteractSkillEvent event) {
        if(event.isCancelled()) return;

        Player player = event.getPlayer();
        InteractSkill interactSkill = (InteractSkill) event.getSkill();

        if(interactSkill.canUseSlowed()) return;

        if(player.hasPotionEffect(PotionEffectType.SLOW)){
            UtilMessage.message(player, event.getSkill().getClassType().getName(), "You cannot use "
                    + ChatColor.GREEN + event.getSkill().getName() + ChatColor.GRAY + " while slowed.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUseSkillInLiquid(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Skill skill = event.getSkill();

        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.message(player, skill.getClassType().getName(), "You cannot use " + ChatColor.GREEN
                    + skill.getName() + ChatColor.GRAY + " in water.");
            event.setCancelled(true);
        }
    }


    private int getLevel(Player player, BuildSkill buildSkill) {
        int level = buildSkill.getLevel();

        SkillType skillType = buildSkill.getSkill().getSkillType();
        if (skillType == SkillType.AXE || skillType == SkillType.SWORD || skillType == SkillType.BOW) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getType() == Material.DIAMOND_SWORD || mainHand.getType() == Material.DIAMOND_AXE
                    || mainHand.getType() == Material.NETHERITE_SWORD || mainHand.getType() == Material.NETHERITE_AXE
                    || mainHand.getType() == Material.CROSSBOW) {
                level++;
            }
        }

        return level;
    }

    private SkillType getSkillTypeByWeapon(Material mainHand) {

        if (Arrays.asList(SkillWeapons.SWORDS).contains(mainHand)) {
            return SkillType.SWORD;
        } else if (Arrays.asList(SkillWeapons.AXES).contains(mainHand)) {
            return SkillType.AXE;
        } else if (Arrays.asList(SkillWeapons.BOWS).contains(mainHand)) {
            return SkillType.BOW;
        }

        return null;
    }

    private boolean hasNegativeEffect(Player player) {
        if (effectManager.hasEffect(player, EffectType.SILENCE)
                || player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            return true;
        }
        return false;
    }
}
