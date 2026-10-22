// module-inflater/build.gradle.kts
// Module 1 — Inflater : XML parser → View tree
// @author jo@Dev

plugins {
    id("maven-publish")
    alias(libs.plugins.android.library)
}

android {
    // Publication : variante unique release (AGP 9 — le composant
    // « release » de maven-publish n'existe qu'une fois la variante
    // déclarée ici).
    publishing {
        singleVariant("release")
    }
    namespace = "jo.layoutlib.inflater"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Dépendances vers les autres modules du mini-layoutlib
    api(project(":module-resources"))
    api(project(":module-attributes"))
    api(project(":module-layout"))
    // Drawables : AttributeApplier expose DrawableResolver dans son API publique
    api(project(":module-drawables"))
    // Themes : AttributeApplier expose ThemeResolver dans son API publique
    api(project(":module-themes"))

    // AndroidX core
    implementation(libs.androidx.core)
    implementation(libs.androidx.annotation)

    // v3.4.9 — VRAIES VUES, MOINS DE MOCKVIEW (retour propriétaire
    // 2026-10-22 : « un chip tombe sur MockView, bien d'autres vues
    // aussi ») : le ViewTagRegistry référence des classes AndroidX et
    // Material (Chip, MaterialButton, CoordinatorLayout, TabLayout,
    // ViewPager2, TextInputLayout…). Sans elles sur le classpath de
    // l'HÔTE, la réflexion échoue et BridgeInflater dégrade vers
    // MockView. En les portant DANS le module, elles sont garanties
    // partout : l'app de démonstration, CodeIDE, tout consommateur —
    // et le repli thème Material (BridgeInflater) peut référencer les
    // styles Material3 à la compilation.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.swiperefreshlayout)

    // Tests unitaires JVM (JUnit 5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kxml2)
    testImplementation(libs.mockito.core)

    // Tests instrumentés Android
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
}

tasks.withType<Test> {
    useJUnitPlatform()
}


// Publication GitHub Packages (miroir codeide-tooling, mise en place du
// 2026-10-17 — PLAN_SEPARATION_DEPOTS.md §9) : composant release,
// coordonnées com.github.jjoblab.LayoutLib:<module> — les MÊMES coordonnées que la
// consommation composite de CodeIDE (substitution, version déclarée
// ignorée en dev ; l'épinglage = version du catalogue + tag du dépôt).


// Publication GitHub Packages (miroir codeide-tooling, mise en place du
// 2026-10-17 — PLAN_SEPARATION_DEPOTS.md §9) : composant RELEASE (AAR),
// coordonnées com.github.jjoblab.LayoutLib:<module> — les MÊMES coordonnées que la
// consommation composite de CodeIDE (substitution, version déclarée
// ignorée en dev ; l'épinglage = version du catalogue + tag du dépôt).
// AGP 9 : le composant « release » n'est créé qu'en afterEvaluate — la
// publication est donc déclarée DANS afterEvaluate (accès direct =
// « SoftwareComponent with name 'release' not found »).
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/jjoblab/LayoutLib")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
