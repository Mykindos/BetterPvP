package me.mykindos.betterpvp.core.inventory.inventoryaccess.component;

import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.i18n.AdventureComponentLocalizer;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.i18n.Languages;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.util.AdventureComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class AdventureComponentWrapper implements ComponentWrapper {
    
    public static final AdventureComponentWrapper EMPTY = new AdventureComponentWrapper(Component.empty());
    
    private final Component component;
    
    public AdventureComponentWrapper(Component component) {
        this.component = component;
    }
    
    @Override
    public @NotNull String serializeToJson() {
        return GsonComponentSerializer.gson().serialize(component);
    }
    
    @Override
    public @NotNull AdventureComponentWrapper localized(@NotNull String lang) {
        if (!Languages.getInstance().doesServerSideTranslations())
            return this;
        
        return new AdventureComponentWrapper(AdventureComponentLocalizer.getInstance().localize(lang, component));
    }
    
    @Override
    public @NotNull AdventureComponentWrapper withoutPreFormatting() {
        return new AdventureComponentWrapper(AdventureComponentUtils.withoutPreFormatting(component));
    }
    
    @Override
    public @NotNull AdventureComponentWrapper clone() {
        try {
            return (AdventureComponentWrapper) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
    
}
