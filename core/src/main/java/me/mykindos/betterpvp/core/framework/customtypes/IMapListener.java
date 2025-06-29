package me.mykindos.betterpvp.core.framework.customtypes;

import org.jetbrains.annotations.Nullable;

public interface IMapListener {

    void onMapValueChanged(String key, Object newValue, @Nullable Object oldValue);
}
