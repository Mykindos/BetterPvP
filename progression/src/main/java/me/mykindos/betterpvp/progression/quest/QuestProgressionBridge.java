package me.mykindos.betterpvp.progression.quest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.content.manifest.ManifestCollectEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.QuestManager;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitiveHandlers;
import me.mykindos.betterpvp.progression.profession.Professions;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Wires progression into the quest engine: registers the profession-specific
 * primitives core can't evaluate (profession level, grant XP), forwards fishing
 * catches as a trigger, and contributes the profession list to the game manifest.
 * This is the leaf-module extension pattern — core stays unaware of progression.
 */
@Singleton
@BPvPListener
public class QuestProgressionBridge implements Listener {

    private final QuestManager questManager;
    private final ProfessionProfileManager profileManager;

    @Inject
    public QuestProgressionBridge(QuestManager questManager, ProfessionProfileManager profileManager,
                                  QuestPrimitiveHandlers handlers) {
        this.questManager = questManager;
        this.profileManager = profileManager;

        handlers.registerCondition("condition.profession_level", this::hasProfessionLevel);
        handlers.registerCondition("requirement.profession_level", this::hasProfessionLevel);
        handlers.registerAction("action.give_xp", this::grantXp);
        handlers.registerAction("reward.xp", this::grantXp);
    }

    private boolean hasProfessionLevel(Player player, PrimitiveData data) {
        String profession = data.getString("profession");
        int required = data.getInt("level", 1);
        if (profession == null) return false;
        ProfessionData professionData = findData(player, profession);
        return professionData != null && professionData.getLevelFromExperience(professionData.getExperience()) >= required;
    }

    private void grantXp(Player player, PrimitiveData data) {
        String profession = data.getString("profession");
        int amount = data.getInt("amount", 0);
        if (profession == null || amount <= 0) return;
        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            ProfessionData professionData = profile.getProfessionDataMap()
                    .computeIfAbsent(profession.toUpperCase(), k -> new ProfessionData(player.getUniqueId(), k));
            professionData.grantExperience(amount, player);
            profileManager.getRepository().saveExperience(player.getUniqueId(), professionData.getProfession(), professionData.getExperience());
        });
    }

    private ProfessionData findData(Player player, String profession) {
        ProfessionProfile profile = profileManager.getObject(player.getUniqueId().toString()).orElse(null);
        if (profile == null) return null;
        for (var entry : profile.getProfessionDataMap().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(profession)) return entry.getValue();
        }
        return null;
    }

    @EventHandler
    public void onFish(PlayerCaughtFishEvent event) {
        questManager.recordEvent(event.getPlayer(), "trigger.fish_caught", data -> true, 1);
    }

    @EventHandler
    public void onManifestCollect(ManifestCollectEvent event) {
        for (Professions profession : Professions.values()) {
            event.addProfession(profession.name(), prettify(profession.name()), null);
        }
    }

    private String prettify(String value) {
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
