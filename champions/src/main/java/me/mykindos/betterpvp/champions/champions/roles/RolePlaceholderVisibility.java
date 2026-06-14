package me.mykindos.betterpvp.champions.champions.roles;

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.CombatFeaturesService;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;

@Singleton
public class RolePlaceholderVisibility {

    private final Set<LivingEntity> hidden = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
    private final CombatFeaturesService combatFeaturesService;

    @Inject
    public RolePlaceholderVisibility(CombatFeaturesService combatFeaturesService) {
        this.combatFeaturesService = combatFeaturesService;
    }

    public boolean shouldRender(LivingEntity wearer) {
        if (hidden.contains(wearer)) {
            return false;
        }
        if (wearer instanceof Player player) {
            // Creative is a source-of-truth gamemode: the placeholder we draw into an empty armor slot gets
            // echoed back and materialized as a real item. Don't render placeholders for creative players.
            return player.getGameMode() != GameMode.CREATIVE && combatFeaturesService.isActive(player);
        }
        return true;
    }

    public void setVisible(LivingEntity wearer, boolean visible) {
        if (visible) {
            hidden.remove(wearer);
        } else {
            hidden.add(wearer);
        }
    }

    public void clear(LivingEntity wearer) {
        hidden.remove(wearer);
    }
}
