package me.mykindos.betterpvp.champions.champions.skills.skills.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class Tranquility extends Skill implements PassiveSkill, Listener, BuffSkill {

    private double timeOutOfCombat;
    private double timeOutOfCombatDecreasePerLevel;
    private int regenerationStrength;

    @Inject
    public Tranquility(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Tranquility";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "After " + getValueString(this::getTimeOutOfCombat, level) + " seconds out of combat",
                "you will gain <effect>Regeneration " + UtilFormat.getRomanNumeral(regenerationStrength),
        };
    }

    public double getTimeOutOfCombat(int level) {
        return timeOutOfCombat - ((level - 1) * timeOutOfCombatDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @Override
    public SkillType getType() {
        return SkillType.GLOBAL;
    }

    @UpdateEvent(delay = 250)
    public void checkTranquility() {
        for (Player cur : Bukkit.getOnlinePlayers()) {
            int level = getLevel(cur);
            if (level > 0) {
                Gamer gamer = championsManager.getClientManager().search().online(cur).getGamer();
                if (UtilTime.elapsed(gamer.getLastDamaged(), (long) getTimeOutOfCombat(level) * 1000)) {
                    championsManager.getEffects().addEffect(cur, EffectTypes.REGENERATION, regenerationStrength, 300L);
                }
            }
        }
    }

    public void loadSkillConfig() {
        timeOutOfCombat = getConfig("timeOutOfCombat", 15.0, Double.class);
        timeOutOfCombatDecreasePerLevel = getConfig("timeOutOfCombatDecreasePerLevel", 5.0, Double.class);
        regenerationStrength = getConfig("regenerationStrength", 1, Integer.class);
    }
}
