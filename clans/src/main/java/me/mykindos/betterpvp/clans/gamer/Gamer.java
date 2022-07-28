package me.mykindos.betterpvp.clans.gamer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.client.Client;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Gamer {

    Client client;
    String uuid;

}
