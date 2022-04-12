rootProject.name = "Kommander"
include("Module.API")
include("Module.Spigot")
project(":Module.API").also { it.name = "API" }
project(":Module.Spigot").also { it.name = "Spigot" }
