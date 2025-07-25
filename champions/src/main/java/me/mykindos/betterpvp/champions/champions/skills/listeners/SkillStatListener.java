package me.mykindos.betterpvp.champions.champions.skills.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillUpdateEvent;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.champions.ChampionsSkillStat;
import me.mykindos.betterpvp.core.client.stats.listeners.TimedStatListener;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;

@BPvPListener
@Singleton
@CustomLog
public class SkillStatListener extends TimedStatListener {
    private final RoleManager roleManager;
    private final BuildManager buildManager;

    @Inject
    public SkillStatListener(ClientManager clientManager, RoleManager roleManager, BuildManager buildManager) {
        super(clientManager);
        this.roleManager = roleManager;
        this.buildManager = buildManager;
    }

    @Override
    public void onUpdate(Client client, long deltaTime) {
        incrementStats(client, ChampionsSkillStat.Action.TIME_PLAYED, deltaTime);
    }

    @EventHandler
    public void onRoleChange(RoleChangeEvent event) {
        if (event.getPrevious() == null) return;
        final GamerBuilds gamerBuilds = buildManager.getObject(event.getPlayer().getUniqueId()).orElseThrow();
        final RoleBuild build = gamerBuilds.getActiveBuilds().get(event.getPrevious().getName());
        earlyUpdateBuild(event.getPlayer(), build);
    }


    @EventHandler
    public void onApplyBuild(ApplyBuildEvent event) {
        log.info("apply build").submit();
        earlyUpdateBuild(event.getPlayer(), event.getOldBuild());
    }

    @EventHandler
    public void onSkillDequip(SkillDequipEvent event) {
        earlyUpdateBuild(event.getPlayer(), event.getPrevious());
    }

    @EventHandler
    public void onSkillUpdate(SkillUpdateEvent event) {
        earlyUpdateBuild(event.getPlayer(), event.getPrevious());
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillEquip(SkillEquipEvent event) {
        earlyUpdateBuild(event.getPlayer(), event.getPrevious());
        final Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);
        final StatContainer statContainer = client.getStatContainer();
        final ChampionsSkillStat skillStat = ChampionsSkillStat.builder()
                .action(ChampionsSkillStat.Action.EQUIP)
                .skill(event.getBuildSkill().getSkill())
                .level(event.getBuildSkill().getLevel())
                .build();
        statContainer.incrementStat(skillStat, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKillContribution(KillContributionEvent event) {
        final Client killer = clientManager.search().online(event.getKiller());
        incrementStats(killer, ChampionsSkillStat.Action.KILL, 1);
        event.getContributions().keySet().stream()
                .map(player -> clientManager.search().online(player))
                .forEach(client -> {
                    incrementStats(client, ChampionsSkillStat.Action.ASSIST, 1);
                });

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Client victim = clientManager.search().online(event.getPlayer());
        incrementStats(victim, ChampionsSkillStat.Action.DEATH, 1);
    }

    private void earlyUpdateBuild(Player player, RoleBuild build) {
        final long currentTime = System.currentTimeMillis();
        final long lastUpdate = lastUpdateMap.computeIfAbsent(player.getUniqueId(), k -> currentTime);
        final Client client = clientManager.search().online(player);
        incrementBuildStats(build, client, ChampionsSkillStat.Action.TIME_PLAYED, currentTime - lastUpdate);
        lastUpdateMap.put(player.getUniqueId(), currentTime);
    }

    private void incrementStats(Client client, ChampionsSkillStat.Action action, double value) {
        final Optional<Role> roleOptional = roleManager.getObject(client.getUniqueId());
        if (roleOptional.isEmpty()) return;
        final Role role = roleOptional.get();
        final GamerBuilds gamerBuilds = buildManager.getObject(client.getUniqueId()).orElseThrow();
        final RoleBuild build = gamerBuilds.getActiveBuilds().get(role.getName());
        incrementBuildStats(build, client, action, value);
    }

    private void incrementBuildStats(RoleBuild build, Client client, ChampionsSkillStat.Action action, double value) {
        build.getActiveSkills().forEach(skill -> {
            final ChampionsSkillStat stat = ChampionsSkillStat.builder()
                    .action(action)
                    .skill(skill.getSkill())
                    .level(skill.getLevel())
                    .build();
            client.getStatContainer().incrementStat(stat, value);
        });
    }
}
