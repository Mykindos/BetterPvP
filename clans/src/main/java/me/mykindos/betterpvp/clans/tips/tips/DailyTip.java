package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.command.commands.general.DailyCommand;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class DailyTip extends ClanTip implements ISuggestCommand {

    private final CooldownManager cooldownManager;
    @Inject
    public DailyTip(Clans clans, CooldownManager cooldownManager) {
        super(clans, 10, 1);
        this.cooldownManager = cooldownManager;
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "daily";
    }

    @Override
    public Component generateComponent() {
        Component suggestComponent = suggestCommand("/daily", "/daily");
        return Component.text("Claim your daily reward by running ", NamedTextColor.GRAY).append(suggestComponent);
    }

    @Override
    public boolean isValid(Player player, Clan clan) {
        return !cooldownManager.hasCooldown(player, DailyCommand.DAILY);
    }
}
