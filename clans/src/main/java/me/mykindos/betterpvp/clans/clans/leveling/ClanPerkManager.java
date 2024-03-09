package me.mykindos.betterpvp.clans.clans.leveling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Singleton
public class ClanPerkManager extends Manager<ClanPerk> {

    @Getter
    private static ClanPerkManager instance;
    private final Clans clans;

    @Inject
    public ClanPerkManager(Clans clans) {
        this.clans = clans;
        if (instance == null) {
            instance = this;
        }
    }

    public void scan() {
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends ClanPerk>> classes = reflections.getSubTypesOf(ClanPerk.class);
        for (Class<? extends ClanPerk> perkClass : classes) {
            if (Modifier.isAbstract(perkClass.getModifiers()) || Modifier.isInterface(perkClass.getModifiers())) continue;
            // Filter out anonymous classes
            if (perkClass.isAnonymousClass()) continue;
            final ClanPerk perk = clans.getInjector().getInstance(perkClass);
            addObject(perk.getName(), perk);
        }
    }

    public Collection<ClanPerk> getPerks(Clan clan) {
        return objects.values().stream().filter(perk -> perk.getMinimumLevel() <= clan.getLevel()).toList();
    }

    public List<ClanPerk> getPerksSortedByLevel() {
        return objects.values().stream().sorted(Comparator.comparingInt(ClanPerk::getMinimumLevel)).toList();
    }

    public boolean hasPerk(Clan clan, ClanPerk perk) {
        return getPerks(clan).contains(perk);
    }

    public boolean hasPerk(Clan clan, Class<?> perk) {
        return getPerks(clan).stream().anyMatch(perk::isInstance);
    }

}
