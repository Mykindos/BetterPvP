package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.mining.CaveCallerSkill;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Objects;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class CaveCaller implements Listener {
    private final ClanManager clanManager;
    private final ProfessionProfileManager professionProfileManager;
    private final ProgressionSkillManager progressionSkillManager;
    private final MiningHandler miningHandler;
    private final CaveCallerSkill caveCallerSkill;

    @Inject
    public CaveCaller(ClanManager clanManager) {
        this.clanManager = clanManager;
        final Progression progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.professionProfileManager = progression.getInjector().getInstance(ProfessionProfileManager.class);
        this.progressionSkillManager = progression.getInjector().getInstance(ProgressionSkillManager.class);
        this.miningHandler = progression.getInjector().getInstance(MiningHandler.class);
        this.caveCallerSkill = progression.getInjector().getInstance(CaveCallerSkill.class);
    }

    @EventHandler
    public void onBreakStoneBlock(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        // Return if invalid block
        long experience = miningHandler.getExperienceFor(event.getBlock().getType());
        if (experience <= 0) return;

        Player player = event.getPlayer();
        Location locationWhereBlockWasMined = event.getBlock().getLocation();

        Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
        Clan clanWhereBlockWasMined = clanManager.getClanByLocation(locationWhereBlockWasMined).orElse(null);

        // Only works inside your own territory
        if (playerClan == null) return;
        if (!playerClan.equals(clanWhereBlockWasMined)) return;

        // Move this to clans b/c u need to check if block is in your territory
        caveCallerSkill.whenPlayerMinesBlockInTerritory(player, locationWhereBlockWasMined);
    }

}
