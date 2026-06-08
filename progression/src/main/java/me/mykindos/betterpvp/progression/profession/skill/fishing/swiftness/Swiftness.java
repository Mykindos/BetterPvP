package me.mykindos.betterpvp.progression.profession.skill.fishing.swiftness;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@Singleton
@NodeId("swiftness")
public class Swiftness extends ProfessionSkill {

    @Inject
    public Swiftness() {
        super("Swiftness");
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Increases catch speed by <green>" + UtilFormat.formatNumber(getSpeedBonus(level), 2) + "%"
        };
    }

    @Override
    public Component[] getDescriptionComponents(int level) {
        return Translations.componentLines("progression.skill.swiftness.desc",
                Component.text(UtilFormat.formatNumber(getSpeedBonus(level), 2) + "%", NamedTextColor.GREEN));
    }

    private double getSpeedBonus(int level) {
        return 0.2 * Math.max(1, level);
    }

    public void onStartFishing(PlayerStartFishingEvent event) {
        Player player = event.getPlayer();
        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Fishing");
            if (profession != null) {
                int skillLevel = getSkillLevel(player);
                if (skillLevel <= 0) return;

                final int wait = (int) (event.getHook().getWaitTime() * (1 - getSpeedBonus(skillLevel) / 100));
                event.getHook().setWaitTime(Math.max(0, wait));
            }
        });

    }

    @Override
    public Material getIcon() {
        return Material.COD;
    }

}
