package me.mykindos.betterpvp.champions.champions.skills.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.event.LoadBuildsEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.components.champions.ISkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseInteractSkillEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseToggleSkillEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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
import java.util.Objects;
import java.util.Optional;

@Singleton
@BPvPListener
public class SkillListener implements Listener {

    private final BuildManager buildManager;
    private final RoleManager roleManager;
    private final CooldownManager cooldownManager;
    private final EnergyHandler energyHandler;
    private final EffectManager effectManager;
    private final ClientManager clientManager;

    @Inject
    public SkillListener(BuildManager buildManager, RoleManager roleManager, CooldownManager cooldownManager,
                         EnergyHandler energyHandler, EffectManager effectManager, ClientManager clientManager) {
        this.buildManager = buildManager;
        this.roleManager = roleManager;
        this.cooldownManager = cooldownManager;
        this.energyHandler = energyHandler;
        this.effectManager = effectManager;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUseSkill(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ISkill skill = event.getSkill();
        int level = event.getLevel();

        if (!skill.canUse(player)) {
            event.setCancelled(true);
            return;
        }

        if (skill instanceof CooldownSkill cooldownSkill && !(skill instanceof PrepareArrowSkill)) {
            if (!cooldownManager.use(player, skill.getName(), cooldownSkill.getCooldown(level),
                    cooldownSkill.showCooldownFinished(), true, cooldownSkill.isCancellable(), cooldownSkill::shouldDisplayActionBar)) {
                event.setCancelled(true);
                return;
            }
        } else if (skill instanceof PrepareArrowSkill) {
            if (cooldownManager.hasCooldown(player, skill.getName())) {

                if (((PrepareArrowSkill) skill).showCooldownFinished()) {
                    UtilMessage.simpleMessage(player, "Cooldown", "You cannot use <alt>%s</alt> for <alt>%s</alt> seconds.", skill.getName(),
                            Math.max(0, cooldownManager.getAbilityRecharge(player, skill.getName()).getRemaining()));
                }
                event.setCancelled(true);
                return;
            }
        }

        if (skill instanceof EnergySkill energySkill && !(skill instanceof ActiveToggleSkill)) {
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

        if (skill.displayWhenUsed()) {
            sendSkillUsed(player, skill, level);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (!UtilItem.isAxe(droppedItem) && !UtilItem.isSword(droppedItem)) {
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

                for (Skill skill : build.getActiveSkills()) {
                    // Skip if not a toggle skill
                    if (!(skill instanceof ToggleSkill)) continue;

                    // Check if they have booster
                    BuildSkill buildSkill = build.getBuildSkill(skill.getType());
                    int level = getLevel(player, buildSkill);

                    UtilServer.callEvent(new PlayerUseToggleSkillEvent(player, skill, level));
                    event.setCancelled(true);
                }
            }
        }

    }

    // Show shield for channel skills
    @EventHandler
    public void onRightClick(RightClickEvent event) {
        if (Compatibility.SWORD_BLOCKING) {
            return; // Return if sword blocking is enabled
        }

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        SkillType skillType = SkillWeapons.getTypeFrom(mainHand);
        if (skillType == null) {
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

                Optional<Skill> skillOptional = build.getActiveSkills().stream()
                        .filter(skill -> skill instanceof InteractSkill && skill.getType() == skillType).findFirst();

                if (skillOptional.isPresent()) {
                    Skill skill = skillOptional.get();
                    if (skill instanceof ChannelSkill) {
                        event.setUseShield(true);
                        event.setShieldModelData(RightClickEvent.INVISIBLE_SHIELD);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSkillActivate(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (cooldownManager.hasCooldown(event.getPlayer(), "DoorAccess")) return;

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        SkillType skillType = SkillWeapons.getTypeFrom(mainHand);
        if (skillType == null) {
            return;
        }

        if (skillType != SkillType.BOW && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            return;
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
                    cooldownManager.use(event.getPlayer(), "DoorAccess", 0.01, false);
                    return;
                }
            }
        }

        if (event.useItemInHand() == Event.Result.DENY) return;

        if (UtilBlock.usable(event.getClickedBlock())) return;

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
            UtilMessage.simpleMessage(player, skill.getClassType().getName(), "<alt>%s</alt> has been disabled by the server.", skill.getName());
            event.setCancelled(true);

        }
    }

    @EventHandler
    public void onUseSkillWhileSlowed(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        ISkill skill = event.getSkill();
        Player player = event.getPlayer();

        if (skill.canUseWhileSlowed()) return;

        if (player.hasPotionEffect(PotionEffectType.SLOW)) {
            UtilMessage.simpleMessage(player, event.getSkill().getClassType().getName(),
                    "You cannot use <green>%s<gray> while slowed.", event.getSkill().getName());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUseSkillWhileLevitating(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;
        ISkill skill = event.getSkill();
        Player player = event.getPlayer();

        if (skill.canUseWhileLevitating()) return;

        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            UtilMessage.simpleMessage(player, event.getSkill().getClassType().getName(),
                    "You cannot use <green>%s<gray> while levitating.", event.getSkill().getName());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUseSkillInLiquid(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ISkill skill = event.getSkill();

        if (skill.canUseInLiquid()) return;

        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.simpleMessage(player, skill.getClassType().getName(), "You cannot use <green>%s<gray> in water.", skill.getName());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUseSkillWhileSilenced(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ISkill skill = event.getSkill();
        if (skill.ignoreNegativeEffects()) return;
        if (skill.canUseWhileSilenced()) return;
        if (effectManager.hasEffect(player, EffectType.SILENCE)) {
            UtilMessage.simpleMessage(player, skill.getClassType().getName(), "You cannot use <green>%s<gray> while silenced.", skill.getName());
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_HURT, 1.0f, 1.0f);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSkillEquip(SkillEquipEvent event) {
        final Gamer gamer = this.clientManager.search().online(event.getPlayer()).getGamer();
        event.getSkill().trackPlayer(event.getPlayer(), gamer);
    }

    @EventHandler
    public void onSkillDequip(SkillDequipEvent event) {
        final Gamer gamer = this.clientManager.search().online(event.getPlayer()).getGamer();
        event.getSkill().invalidatePlayer(event.getPlayer(), gamer);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoadBuilds(LoadBuildsEvent event) {
        final Player player = event.getPlayer();

        Role role = roleManager.getObject(player.getUniqueId().toString()).orElse(null);
        GamerBuilds builds = event.getGamerBuilds();

        // Track new skills
        String name = role == null ? null : role.getName();
        RoleBuild build = builds.getActiveBuilds().get(name);
        if (build != null) {
            final Gamer gamer = this.clientManager.search().online(player).getGamer();
            build.getActiveSkills().forEach(skill -> skill.trackPlayer(player, gamer));
        }
    }

    @EventHandler
    public void onRoleChange(RoleChangeEvent event) {
        final Role newRole = event.getRole();
        final Role previousRole = event.getPrevious();
        final Player player = event.getPlayer();

        Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId().toString());
        if (gamerBuildsOptional.isPresent()) {
            GamerBuilds builds = gamerBuildsOptional.get();
            final Gamer gamer = this.clientManager.search().online(player).getGamer();

            // Invalidate old skills
            String name = previousRole == null ? null : previousRole.getName();
            RoleBuild build = builds.getActiveBuilds().get(name);
            if (build != null) {
                build.getActiveSkills().stream().filter(Objects::nonNull).forEach(skill -> skill.invalidatePlayer(player, gamer));
            }

            // Track with new skills
            name = newRole == null ? null : newRole.getName();
            build = builds.getActiveBuilds().get(name);
            if (build != null) {
                build.getActiveSkills().stream().filter(Objects::nonNull).forEach(skill -> skill.trackPlayer(player, gamer));
            }
        }
    }

    @EventHandler
    public void onUseSkillWhileStunned(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ISkill skill = event.getSkill();
        if (skill.ignoreNegativeEffects()) return;
        if (skill.canUseWhileStunned()) return;
        if (effectManager.hasEffect(player, EffectType.STUN)) {
            UtilMessage.simpleMessage(player, skill.getClassType().getName(), "You cannot use <green>%s<gray> while stunned.", skill.getName());
            event.setCancelled(true);
        }
    }


    private int getLevel(Player player, BuildSkill buildSkill) {
        int level = buildSkill.getLevel();

        SkillType skillType = buildSkill.getSkill().getType();
        if ((skillType == SkillType.AXE || skillType == SkillType.SWORD || skillType == SkillType.BOW)
                && SkillWeapons.hasBooster(player)) {
            level++;
        }

        return level;
    }
}
