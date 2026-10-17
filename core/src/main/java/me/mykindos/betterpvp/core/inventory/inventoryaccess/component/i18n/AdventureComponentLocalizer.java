package me.mykindos.betterpvp.core.inventory.inventoryaccess.component.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;

public class AdventureComponentLocalizer extends me.mykindos.betterpvp.core.inventory.inventoryaccess.component.i18n.ComponentLocalizer<Component> {
    
    private static final AdventureComponentLocalizer INSTANCE = new AdventureComponentLocalizer();
    
    private AdventureComponentLocalizer() {
        super(Component::text);
    }
    
    public static AdventureComponentLocalizer getInstance() {
        return INSTANCE;
    }
    
    @Override
    public Component localize(String lang, Component component) {
        ComponentBuilder<?, ?> builder = component instanceof TranslatableComponent translatable
                ? localizeTranslatable(lang, translatable).toBuilder()
                : component.toBuilder();

        builder.mapChildrenDeep(child -> {
            if (child instanceof TranslatableComponent translatable)
                return localizeTranslatable(lang, translatable);
            return child;
        });

        return builder.build();
    }

    private Component localizeTranslatable(String lang, TranslatableComponent component) {
        var formatString = Languages.getInstance().getFormatString(lang, component.key());
        if (formatString == null)
            return component;

        var args = component.arguments().stream().map(TranslationArgument::asComponent).toList();
        var children = decomposeFormatString(lang, formatString, component, args);
        return Component.textOfChildren(children.toArray(ComponentLike[]::new)).style(component.style());
    }

}
