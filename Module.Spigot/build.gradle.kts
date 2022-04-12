dependencies {
    api(project(":API"))
    implementation("me.hwiggy:Reflection:1.0.2")
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    relocate("me.hwiggy.reflection", "me.hwiggy.kommander.spigot.reflect")
}