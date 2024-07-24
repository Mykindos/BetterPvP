Repository containing all of BetterPvP's plugins for 1.21.

## Dependencies:
- [Java 21](https://www.oracle.com/java/technologies/downloads/#java21)
- [PaperSpigot](https://papermc.io/downloads)
- [Docker](https://www.docker.com/) or for Windows and Mac users [Docker Desktop](https://www.docker.com/products/docker-desktop)
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997)
- [LibsDisguises](https://www.spigotmc.org/resources/libs-disguises.32453)
- A MariaDB server ([Bundled](docker))
- A Redis server ([Bundled](docker))

## Development Setup
1. Clone this repository and open it with your IDE of choice:
    ```bash
    git clone https://github.com/Mykindos/BetterPvP.git
    ```
2. Create your MariaDB and Redis servers. If you have Docker installed, you can use the bundled `docker-compose.yml` file to create both servers by running the `setup.sh` script in the `docker` folder
3. Compile the project using the Gradle task `assemble` to properly shade all project dependencies:
    ```bash
    ./gradlew assemble
    ```
   
4. Copy the generated jar files from `./build` into your server's `plugins` folder.
5. In your plugins folder, make sure to also include ProtocolLib and LibsDisguises as they are required dependencies.
6. Start your server and wait for all plugins to load. You should get an error saying that the server couldn't establish a connection to the database.
7. In the generated plugin folders, open the `config.yml` file in each one of them and fill in the required information.
   * For `Core` specifically, fill in all database details:
     * `ip`: The host of your MySQL server
     * `databaseName`: The name of the database to use
     * `username`: The username of an account with access to the database
     * `password`: The password of the account with access to the database
8. Restart your server.
