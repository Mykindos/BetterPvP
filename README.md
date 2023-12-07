Repository containing all of BetterPvP's plugins for 1.20.2.

## Dependencies:
- [Java 17](https://www.oracle.com/java/technologies/downloads/#java17)
- [PaperSpigot](https://papermc.io/downloads)
- [Docker](https://www.docker.com/) or for Windows and Mac users [Docker Desktop](https://www.docker.com/products/docker-desktop)
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997)
- A MariaDB server ([Bundled](docker))
- A Redis server ([Bundled](docker))

## Development Setup
1. Clone this repository and open it with your IDE of choice:
    ```bash
    git clone https://github.com/Mykindos/BetterPvP.git
    ```
2. Compile the project using the Gradle task `assemble` to properly shade all project dependencies:
    ```bash
    ./gradlew assemble
    ```
   
3. Copy the generated jar files from all modules (`./build/libs/`) into your server's `plugins` folder.
4. In your plugins folder, make sure to also include ProtocolLib it is a required dependency.
5. Start your server and wait for all plugins to load. You should get an error saying that the server couldn't establish a connection to the database.
6. In the generated plugin folders, open the `config.yml` file in each one of them and fill in the required information.
   * For `Core` specifically, fill in all database details:
     * `prefix`: (Optional) Overrides the default prefix for all tables in this module.
     * `ip`: The host of your MySQL server
     * `databaseName`: The name of the database to use
     * `username`: The username of an account with access to the database
     * `password`: The password of the account with access to the database
   * For all other modules:
       * `prefix`: (Optional) Overrides the default prefix for all tables in this module.
7. Restart your server.
