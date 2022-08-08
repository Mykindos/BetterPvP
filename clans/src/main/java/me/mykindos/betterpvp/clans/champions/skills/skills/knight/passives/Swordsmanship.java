package me.mykindos.betterpvp.clans.champions.skills.skills.knight.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Swordsmanship extends Skill implements PassiveSkill {

    @Inject
    @Config(path="skills.knight.swordsmanship.timeBetweenCharges", defaultValue = "2")
    private double timeBetweenCharges;

    @Inject
    @Config(path="skills.knight.swordsmanship.timeOutOfCombat", defaultValue = "2.5")
    private double timeOutOfCombat;

    private final WeakHashMap<Player, Integer> charges = new WeakHashMap<>();

    @Inject
    public Swordsmanship(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }

    @Override
    public String getName() {
        return "Swordsmanship";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Prepare a powerful sword attack,",
                "You gain 1 charge every 3 seconds.",
                "You can store a maximum of " + ChatColor.GREEN + (level) + ChatColor.GRAY + " charges",
                "",
                "When you attack, your damage is",
                "increased by the number of your charges",
                "and your charges are reset to 0.",
                "",
                "This only applies to swords."
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!charges.containsKey(player)) return;
        if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) return;

        int level = getLevel(player);
        if (level > 0) {
            int charge = charges.get(player);
            event.setDamage(event.getDamage() + (charge * 0.5));
            charges.remove(player);
        }
    }

    @UpdateEvent(delay = 250)
    public void addCharge() {

        for (Player cur : Bukkit.getOnlinePlayers()) {
            int level = getLevel(cur);
            if (level > 0) {
                if (charges.containsKey(cur)) {
                    Optional<Gamer> gamerOptional = championsManager.getGamers().getObject(cur.getUniqueId().toString());
                    gamerOptional.ifPresent(gamer -> {
                        if (UtilTime.elapsed(gamer.getLastDamaged(), (long) timeOutOfCombat * 1000)) {
                            if (!championsManager.getCooldowns().add(cur, getName(), timeBetweenCharges, false)) return;
                            int charge = charges.get(cur);
                            if (charge < level) {
                                charge = Math.min(level, charge + 1);
                                UtilMessage.simpleMessage(cur, getClassType().getName(), "Swordsmanship charge: <yellow>%d", charge);
                                charges.put(cur, charge);
                            }
                        }
                    });

                }
            } else {
                charges.put(cur, 0);
            }
        }

    }

}


