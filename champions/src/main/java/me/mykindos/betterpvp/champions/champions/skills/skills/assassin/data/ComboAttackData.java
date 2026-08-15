package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ComboAttackData {
    private double damageIncrement;
    private long last;
}
