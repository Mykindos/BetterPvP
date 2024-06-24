package me.mykindos.betterpvp.core.logging.formatters;

import net.kyori.adventure.text.Component;

import java.util.HashMap;

public interface ILogFormatter {

    String getAction();

    Component formatLog(HashMap<String, String> context);

}
