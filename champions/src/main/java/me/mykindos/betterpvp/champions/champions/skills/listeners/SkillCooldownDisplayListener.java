package me.mykindos.betterpvp.champions.champions.skills.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.cooldowns.events.CooldownDisplayEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
public class SkillCooldownDisplayListener implements Listener {

    private final GamerManager gamerManager;
    private final RoleManager roleManager;
    private final BuildManager buildManager;
    private final CooldownManager cooldownManager;

    @Inject
    public SkillCooldownDisplayListener(GamerManager gamerManager, RoleManager roleManager, BuildManager buildManager, CooldownManager cooldownManager) {
        this.gamerManager = gamerManager;
        this.roleManager = roleManager;
        this.buildManager = buildManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCooldownDisplay(CooldownDisplayEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCooldownName().equals("")) return;

        Player player = event.getPlayer();
        Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId());
        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            buildManager.getObject(player.getUniqueId()).ifPresent(build -> {
                RoleBuild activeBuild = build.getActiveBuilds().get(role.getName());
                if (activeBuild == null) return;

                BuildSkill buildSkill = getCurrentBuildSkill(player, activeBuild);
                if (buildSkill == null) return;
                if (buildSkill.getSkill() instanceof CooldownSkill cooldownSkill) {
                    if (cooldownSkill.showCooldownFinished()) {
                        event.setCooldownName(cooldownSkill.getName());
                    }
                }

            });
        }
    }

    private BuildSkill getCurrentBuildSkill(Player player, RoleBuild roleBuild) {
        SkillType skillType = null;

        if (UtilPlayer.isHoldingItem(player, SkillWeapons.AXES)) {
            skillType = SkillType.AXE;
        } else if (UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
            skillType = SkillType.SWORD;
        } else if (UtilPlayer.isHoldingItem(player, SkillWeapons.BOWS)) {
            skillType = SkillType.BOW;
        }

        if (skillType == null) return null;
        BuildSkill buildSkill = roleBuild.getBuildSkill(skillType);
        if (buildSkill != null) {
            if (cooldownManager.isCooling(player, buildSkill.getSkill().getName())) {
                return buildSkill;
            } else {
                BuildSkill passiveB = roleBuild.getBuildSkill(SkillType.PASSIVE_B);
                if (passiveB != null) {
                    if (passiveB.getLevel() > 0 && cooldownManager.isCooling(player, passiveB.getSkill().getName())) {
                        return passiveB;
                    }
                }
            }
        }

        return null;
    }


}
