package me.mykindos.betterpvp.clans.champions.skills.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onUseSkill(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Skill skill = event.getSkill();
        int level = event.getLevel();

        if (!clanManager.canCast(player)) {
            event.setCancelled(true);
            return;
        }

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
        Skill skill = event.getSkill();
        int level = event.getLevel();

        if (skill instanceof InteractSkill interactSkill) {
            interactSkill.activate(player, level);
        } else if (skill instanceof ToggleSkill toggleSkill) {
            toggleSkill.toggle(player, level);
        }

        sendSkillUsed(player, skill, level);

    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

        Material droppedItem = event.getItemDrop().getItemStack().getType();
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

            Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
            if (gamerOptional.isPresent()) {
                Gamer gamer = gamerOptional.get();

                RoleBuild build = gamer.getActiveBuilds().get(role.getName());
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

    private void sendSkillUsed(Player player, Skill skill, int level) {
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
        Skill skill = event.getSkill();

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
        Skill skill = event.getSkill();

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

    @EventHandler
    public void onFetchNearbyEntity(FetchNearbyEntityEvent<?> event) {
        if (!(event.getSource() instanceof Player player)) return;
        event.getEntities().removeIf(entity -> {
            if (entity instanceof Player target) {
                if (target.getGameMode() == GameMode.CREATIVE || target.getGameMode() == GameMode.SPECTATOR) {
                    return true;
                }
                boolean canHurt = clanManager.canHurt(player, target);
                if (event.getEntityProperty() == EntityProperty.FRIENDLY) {
                    return canHurt;
                } else if (event.getEntityProperty() == EntityProperty.ENEMY) {
                    return !canHurt;
                }
            }
            return false;
        });
    }

}
