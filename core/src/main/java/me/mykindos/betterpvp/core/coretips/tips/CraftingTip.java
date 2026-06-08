package me.mykindos.betterpvp.core.coretips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.coretips.CoreTip;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class CraftingTip extends CoreTip  implements ISuggestCommand {

    @Inject
    public CraftingTip(Core core) {
        super(core, 1, 1);
        setComponent(generateComponent());
    }

    @Override
    public Component generateComponent() {
        Component suggestComponent = suggestCommand("/items", "/items");
        return Translations.component("core.tip.craftingtip", suggestComponent).color(NamedTextColor.GRAY);
    }

    @Override
    public String getName() {
        return "craftingtip";
    }

    @Override
    public boolean isValid(Player player) {
        return true;
    }
}
