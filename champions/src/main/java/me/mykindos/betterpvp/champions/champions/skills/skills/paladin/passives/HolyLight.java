package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class HolyLight extends Skill implements PassiveSkill {

    @Inject
    public HolyLight(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Holy Light";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Create an aura that gives",
                "yourself and all allies within",
                "<val>" + (8 + level) + "</val> blocks Regeneration I"};
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    private void activate(Player player, int level) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 140, 0));
        for (var target : UtilPlayer.getNearbyPlayers(player, player.getLocation(), (8 + level), EntityProperty.FRIENDLY)) {
            target.getKey().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0));
        }
    }


    @UpdateEvent(delay = 500)
    public void updateHolyLight() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            if (level > 0) {
                activate(player, level);
            }
        }
    }


}
