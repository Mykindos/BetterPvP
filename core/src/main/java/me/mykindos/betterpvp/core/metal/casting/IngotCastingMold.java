package me.mykindos.betterpvp.core.metal.casting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemKey;

@Singleton
@ItemKey("core:ingot_casting_mold")
public class IngotCastingMold extends CastingMold {

    @Inject
    private IngotCastingMold() {
        super("Ingot Casting Mold", "ingot");
    }
}
