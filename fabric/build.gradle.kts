// Le mod sur Fabric, une fois par version de Minecraft : l'adaptateur Fabric (fabric/src) et le
// cœur de la même version (src/, nœud frère), réunis dans un seul jar.
plugins {
	id("net.fabricmc.fabric-loom")
}

val minecraft = sc.current.version
val modVersion = rootProject.extra["modVersion"].toString()
version = "$modVersion+$minecraft"
base.archivesName = "fmab-fabric"

// Le cœur de la même version de Minecraft.
val common = requireNotNull(sc.node.sibling("")) { "pas de cœur pour $project" }.project
evaluationDependsOn(common.path)
val commonSets = common.extensions.getByType<SourceSetContainer>()
val commonMain = commonSets["main"]
val commonClient = commonSets["client"]

loom {
	splitEnvironmentSourceSets()
	accessWidenerPath = rootProject.file("src/main/resources/fmab.classtweaker")
	mods {
		register("fmab") {
			sourceSet(sourceSets["main"])
			sourceSet(sourceSets["client"])
			sourceSet("main", common)
			sourceSet("client", common)
		}
	}
}

repositories {
	// Mod Menu : facultatif en jeu, seulement compilé contre son API (bouton de réglages du mod).
	maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
}

dependencies {
	minecraft("com.mojang:minecraft:$minecraft")
	implementation("net.fabricmc:fabric-loader:${sc.properties.get<String>("deps.fabric_loader")}")
	implementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
	"clientCompileOnly"("com.terraformersmc:modmenu:${sc.properties.get<String>("deps.modmenu")}")

	implementation(commonMain.output)
	"clientImplementation"(commonClient.output)
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
	options.encoding = "UTF-8"
}

tasks.processResources {
	val props = mapOf("version" to version.toString(), "minecraft" to sc.properties.get<String>("mod.mc_compat"))
	inputs.properties(props)
	filesMatching("fabric.mod.json") { expand(props) }
}

tasks.jar {
	from(commonMain.output)
	from(commonClient.output)
	from(rootProject.file("LICENSE")) { rename { "${it}_fmab" } }
	// Tous les jars, de chaque loader et de chaque version, au même endroit.
	destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
}

tasks.named<Jar>("sourcesJar") {
	from(commonMain.allSource)
	from(commonClient.allSource)
	destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
}

// Visite guidée des rendus en captures d'écran (./gradlew runClientGameTest) : voir RenderShowcase.
fabricApi {
	configureTests {
		createSourceSet = true
		modId = "fmab-gametest"
		enableGameTests = false
		enableClientGameTests = true
		eula = true
	}
}

// ./gradlew runClientGameTest -PfmabGametest=gate,showcase : ne joue que ces scènes (voir RenderShowcase.selected).
loom.runs.matching { it.name == "clientGameTest" }.configureEach {
	findProperty("fmabGametest")?.let { vmArg("-Dfmab.gametest=$it") }
	findProperty("fmabTowns")?.let { vmArg("-Dfmab.towns=$it") }
}
