$dest = "C:\Users\Owen\Personal\Minecraft\Server\plugins"
$local = "C:\Users\Owen\IdeaProjects\BetterPvPJavaTest"


Copy-Item -Path $local"\champions\build\libs\champions-1.0.jar" -Destination $dest
Copy-Item -Path $local"\clans\build\libs\clans-1.0.jar" -Destination $dest
Copy-Item -Path $local"\core\build\libs\core-1.0.jar" -Destination $dest
Copy-Item -Path $local"\shops\build\libs\shops-1.0.jar" -Destination $dest