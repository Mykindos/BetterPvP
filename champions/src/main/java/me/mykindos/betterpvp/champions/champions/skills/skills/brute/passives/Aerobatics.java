package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.block.BlockFace;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Aerobatics extends Skill implements PassiveSkill {

    private double percentIncreasePerLevel;
    private double percent;

    @Inject
    public Aerobatics(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Aerobatics";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "While in the air you deal <val>" + (int)(getPercent(level) * 100) + "%</val> more damage",
        };
    }

    private double getPercent(int level) {
        return percent + ((level - 1) * percentIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player damager) {
            Player damagee = event.getDamagee();
            int level = getLevel(damager);
            if (level > 0) {
                boolean isPlayerGrounded = UtilBlock.isGrounded(player) || player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid();
                if(!isPlayerGrounded){
                    double modifier = getPercent(level);
                    event.setDamage(event.getDamage() * (1.0 + modifier));
                    damagee.getWorld().playSound(damagee.getLocation(), Sound.BLOCK_LARGE_AMETHYST_BUD_BREAK, 0.5F, 2.0F);
                }
            }
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        percent = getConfig("percent", 0.3, Double.class);
        percentIncreasePerLevel = getConfig("percentIncreasePerLevel", 0.1, Double.class);
    }
}
