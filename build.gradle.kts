// Le cœur du mod (src/), une fois par version de Minecraft : le jeu, sans aucun loader. Il ne
// compile que contre Minecraft et Mixin ; ce qu'il attend d'un loader passe par ses ports
// (com.ajustor.fmab.platform), que chaque loader implémente dans sa branche (fabric/…).
plugins {
	id("net.fabricmc.fabric-loom")
}

val minecraft = sc.current.version
version = "${rootProject.extra["modVersion"]}+$minecraft"
base.archivesName = "fmab-common"

loom {
	splitEnvironmentSourceSets()
	// Ce que le cœur ouvre dans Minecraft (voir le fichier) : appliqué ici à la compilation, par le
	// loader à l'exécution.
	accessWidenerPath = rootProject.file("src/main/resources/fmab.classtweaker")
}

dependencies {
	minecraft("com.mojang:minecraft:$minecraft")
	// Les annotations des mixins : les loaders fournissent Mixin à l'exécution.
	compileOnly("net.fabricmc:sponge-mixin:${sc.properties.get<String>("deps.mixin")}")
	compileOnly("io.github.llamalad7:mixinextras-common:${sc.properties.get<String>("deps.mixinextras")}")

	testImplementation(platform("org.junit:junit-bom:5.13.4"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Les données dont le format change d'une version de Minecraft à l'autre : src/overlays/<version>/
// remplace, pour cette version et les suivantes, les fichiers de même chemin de src/main/resources
// (comme les « overlays » des packs du jeu). Le code, lui, passe par com.ajustor.fmab.compat.
val overlays = rootProject.file("src/overlays").listFiles().orEmpty()
	.filter { it.isDirectory && sc.current.parsed >= it.name }
	.sortedWith(compareBy<File>({ it.name.split('.').getOrNull(0)?.toIntOrNull() }, { it.name.split('.').getOrNull(1)?.toIntOrNull() }))
tasks.processResources {
	overlays.forEach { from(it) }
	// Le dernier copié l'emporte : la version la plus récente qui s'applique.
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

java {
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
	options.encoding = "UTF-8"
}

tasks.test {
	useJUnitPlatform()
	// Les tests lisent des fichiers du dépôt par des chemins relatifs à sa racine.
	workingDir = rootProject.projectDir
	// Les tests lisent les JSON de glyphes et les fichiers de langue directement dans le dépôt,
	// les mêmes que ceux que l'éditeur web publiera.
	systemProperty("fmab.resources", rootProject.file("src/main/resources").absolutePath)
	// Cercles de test partagés avec l'éditeur web ; -PwriteFixtures=true réécrit leurs résultats
	// attendus à partir de l'implémentation Java, qui fait référence.
	systemProperty("fmab.testdata", rootProject.file("testdata").absolutePath)
	systemProperty("fmab.writeFixtures", findProperty("writeFixtures") ?: "false")
	inputs.dir(rootProject.file("testdata"))
	testLogging {
		events("failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
}
