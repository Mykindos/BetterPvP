package me.mykindos.betterpvp.core.logging.menu.button.type;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IStringFilterValueButton extends IRefreshButton {
    void addValue(String context, String value);

    @Nullable String getSelected();

    void setSelectedContext(String newContext);

    java.util.HashMap<String, List<String>> getContextValues();
}
