package me.mykindos.betterpvp.champions.champions.skills.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.champions.energy.EnergyHandler;
import me.mykindos.betterpvp.core.components.champions.ISkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseInteractSkillEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseToggleSkillEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
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
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Optional;


@Singleton
@BPvPListener
public class SkillListener implements Listener {

    private final BuildManager buildManager;
    private final RoleManager roleManager;
    private final CooldownManager cooldownManager;
    private final EnergyHandler energyHandler;
    private final EffectManager effectManager;


    @Inject
    public SkillListener(BuildManager buildManager, RoleManager roleManager, CooldownManager cooldownManager,
                         EnergyHandler energyHandler, EffectManager effectManager) {
        this.buildManager = buildManager;
        this.roleManager = roleManager;
        this.cooldownManager = cooldownManager;
        this.energyHandler = energyHandler;
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUseSkill(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ISkill skill = event.getSkill();
        int level = event.getLevel();

        if (hasNegativeEffect(player)) {
            event.setCancelled(true);
            return;
        }

        if (!skill.canUse(player)) {
            event.setCancelled(true);
            return;
        }

        if (skill instanceof CooldownSkill cooldownSkill) {
            if (!cooldownManager.add(player, skill.getName(), cooldownSkill.getCooldown(level),
                    cooldownSkill.showCooldownFinished(), true, cooldownSkill.isCancellable())) {
                event.setCancelled(true);
                return;
            }
        }

        if (skill instanceof EnergySkill energySkill) {
            if (energySkill.getEnergy(level) > 0) {
                if (!energyHandler.use(player, skill.getName(), energySkill.getEnergy(level), true)) {
                    event.setCancelled(true);
                }
            }

        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinishUseSkill(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ISkill skill = event.getSkill();
        int level = event.getLevel();

        if (skill instanceof InteractSkill interactSkill) {
            interactSkill.activate(player, level);
        } else if (skill instanceof ToggleSkill toggleSkill) {
            toggleSkill.toggle(player, level);
        }

        if(skill.displayWhenUsed()) {
            sendSkillUsed(player, skill, level);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

        Material droppedItem = event.getItemDrop().getItemStack().getType();
        if (!Arrays.asList(SkillWeapons.AXES).contains(droppedItem) && !Arrays.asList(SkillWeapons.SWORDS).contains(droppedItem)) {
            return;
        }

        Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId().toString());
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();

            Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId().toString());
            if (gamerBuildsOptional.isPresent()) {
                GamerBuilds builds = gamerBuildsOptional.get();

                RoleBuild build = builds.getActiveBuilds().get(role.getName());
                if (build == null) return;
                if (build.getPassiveB() == null) return;

                Skill skill = build.getPassiveB().getSkill();
                if (!(skill instanceof ToggleSkill)) return;


                int level = getLevel(player, build.getBuildSkill(SkillType.PASSIVE_B));

                UtilServer.callEvent(new PlayerUseToggleSkillEvent(player, skill, level));
                event.setCancelled(true);

            }
        }

    }

    @EventHandler
    public void onSkillActivate(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
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

        if (UtilBlock.usable(event.getClickedBlock())) return;


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

            Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId().toString());
            if (gamerBuildsOptional.isPresent()) {
                GamerBuilds builds = gamerBuildsOptional.get();

                RoleBuild build = builds.getActiveBuilds().get(role.getName());
                if (build == null) return;

                Optional<Skill> skillOptional = build.getActiveSkills().stream()
                        .filter(skill -> skill instanceof InteractSkill && skill.getType() == skillType).findFirst();

                if (skillOptional.isPresent()) {
                    Skill skill = skillOptional.get();

                    if (skill instanceof InteractSkill interactSkill) {
                        if (!Arrays.asList(interactSkill.getActions()).contains(event.getAction())) {
                            return;
                        }
                    }

                    int level = getLevel(player, build.getBuildSkill(skillType));

                    UtilServer.callEvent(new PlayerUseInteractSkillEvent(player, skill, level));

                }

            }

        }

    }

    private void sendSkillUsed(Player player, ISkill skill, int level) {
        if (skill instanceof PrepareSkill) {
            UtilMessage.simpleMessage(player, skill.getClassType().getName(), "You prepared <green>%s %d<gray>.", skill.getName(), level);

        } else {
            if (!(skill instanceof ChannelSkill)) {
                UtilMessage.simpleMessage(player, skill.getClassType().getName(), "You used <green>%s %d<gray>.", skill.getName(), level);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUseSkillDisabled(PlayerUseSkillEvent event) {
        Player player = event.getPlayer();
        ISkill skill = event.getSkill();

        if (!skill.isEnabled()) {
            UtilMessage.message(player, skill.getClassType().getName(), "%s has been disabled by the server.",
                    ChatColor.GREEN + skill.getName() + ChatColor.GRAY);
            event.setCancelled(true);

        }
    }

    @EventHandler
    public void onUseSkillWhileSlowed(PlayerUseInteractSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        InteractSkill interactSkill = (InteractSkill) event.getSkill();

        if (interactSkill.canUseSlowed()) return;

        if (player.hasPotionEffect(PotionEffectType.SLOW)) {
            UtilMessage.message(player, event.getSkill().getClassType().getName(), "You cannot use %s while slowed.",
                    ChatColor.GREEN + event.getSkill().getName() + ChatColor.GRAY);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUseSkillInLiquid(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ISkill skill = event.getSkill();

        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.message(player, skill.getClassType().getName(), "You cannot use %s in water.",
                    ChatColor.GREEN + skill.getName() + ChatColor.GRAY);
            event.setCancelled(true);
        }
    }


    private int getLevel(Player player, BuildSkill buildSkill) {
        int level = buildSkill.getLevel();

        SkillType skillType = buildSkill.getSkill().getType();
        if (skillType == SkillType.AXE || skillType == SkillType.SWORD || skillType == SkillType.BOW) {
            if (UtilPlayer.isHoldingItem(player, SkillWeapons.BOOSTERS)) {
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
        return effectManager.hasEffect(player, EffectType.SILENCE)
                || player.hasPotionEffect(PotionEffectType.LEVITATION)
                || effectManager.hasEffect(player, EffectType.STUN);
    }



}
