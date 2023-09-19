Repository containing all of BetterPvP's plugins for 1.20.1.

## Dependencies:
- Java 17
- PaperSpigot
- A MySQL server
- [MythicMobs](https://www.spigotmc.org/resources/mythicmobs.5702)
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997)
- [LibsDisguises](https://www.spigotmc.org/resources/libs-disguises-free.81)


## Development Setup
1. Clone this repository and open it with your IDE of choice:
    ```bash
    git clone https://github.com/Mykindos/BetterPvP.git
    ```
2. Compile the project using the Gradle task `reobfJar` to properly shade all project dependencies:
   **NOTE: You must run `reobfJar` to properly obfuscate the Mojang mappings.**
    ```bash
    ./gradlew reobfJar
    ```
   
3. Copy the generated jar files from all modules (`./build/libs/`) into your server's `plugins` folder.
4. In your plugins folder, make sure to also include ProtocolLib, LibsDisguises, and MythicMobs as they are required dependencies.
5. Create a MySQL database and import all `.sql` files located in the resource folders of each module.
6. Start your server and wait for all plugins to load. You should get an error saying that the server couldn't establish a connection to the database.
7. In the generated plugin folders, open the `config.yml` file in each one of them and fill in the required information.
   * For `Core` specifically, fill in all database details:
     * `prefix`: The prefix to use for all tables in this module.
     * `ip`: The host of your MySQL server
     * `databaseName`: The name of the database to use
     * `username`: The username of an account with access to the database
     * `password`: The password of the account with access to the database
   * For all other modules:
     * `prefix`: The prefix to use for all tables in this module. 
8. Restart your server.