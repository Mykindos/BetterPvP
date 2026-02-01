package me.mykindos.betterpvp.core.server;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
public class Season implements Period{
    private final int id;
    @EqualsAndHashCode.Exclude
    private final String name;
    @EqualsAndHashCode.Exclude
    private final LocalDate start;
}
