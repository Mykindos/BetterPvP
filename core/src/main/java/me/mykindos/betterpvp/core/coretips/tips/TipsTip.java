package me.mykindos.betterpvp.core.coretips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.coretips.CoreTip;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.tips.types.IRunCommand;
import me.mykindos.betterpvp.core.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class TipsTip extends CoreTip implements ISuggestCommand, IRunCommand {

    @Inject
    public TipsTip(Core core) {
        super(core, 1, 2);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "tipstip";
    }

    @Override
    public Component generateComponent() {
        Component suggestCommand = suggestCommand("/tip <number>", "/tip ");
        Component settingsComponent = runCommand("/settings");
        return Translations.component("core.tip.tipstip", suggestCommand, settingsComponent);
    }

    @Override
    public boolean isValid(Player player) {
        return true;
    }
}
