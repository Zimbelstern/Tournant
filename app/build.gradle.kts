
import java.text.SimpleDateFormat
import java.util.Date

plugins {
	id("com.android.application")
	id("com.github.triplet.play") version "3.12.1"
	id("com.google.devtools.ksp") version "2.2.0-2.0.2"
	id("kotlin-android")
	id("kotlin-kapt")
	id("kotlin-parcelize")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
}

android {
	compileSdk = 36
	namespace = "eu.zimbelstern.tournant"

	defaultConfig {
		applicationId = "eu.zimbelstern.tournant"
		minSdk = 21
		targetSdk = 36
		versionCode = 40
		versionName = "2.11.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables.generatedDensities?.clear()
	}

	val availableLanguages = File("$projectDir/src/main/res").walk()
		.filter { file ->
			file.isDirectory
				&& file.walk().any { it.name == "strings.xml" }
				&& Regex("values-[a-z]{2}(-r?[A-Z]{2})?").matches(file.name)
		}
		.plus(File("$projectDir/src/main/res/values"))
		.sorted()
		.map { localeDir ->
			localeDir.walk().find { it.name == "strings.xml" }!!.readText().run {
				listOf(
					localeDir.name.drop(7).replace("-r", "-").ifEmpty { "en" },
					substringAfter("<string name=\"to\">", "").substringBefore("</string>"),
					substringAfter("<string name=\"hours_for_regex\">", "").substringBefore("</string>"),
					substringAfter("<string name=\"minutes_for_regex\">", "").substringBefore("</string>"),
					substringAfter("<string name=\"seconds_for_regex\">", "").substringBefore("</string>")
				)
			}
		}

	applicationVariants.configureEach {
		resValue("string", "versionName", versionName)
		resValue("string", "buildDate", SimpleDateFormat("dd.MM.yy HH:mm").format(Date()))
		resValue("string", "availableLanguages", availableLanguages.map { it.first() }.joinToString(","))
		resValue("string", "localisedTimeStrings", availableLanguages.map { it.joinToString(":") }.joinToString(";"))
		mergedFlavor.manifestPlaceholders["fileprovider_authority"] = "$applicationId.fileprovider"
	}

	buildFeatures {
		buildConfig = true
		compose = true
		dataBinding = true
		viewBinding = true
	}

	buildTypes {
		getByName("debug") {
			applicationIdSuffix = ".debug"
			versionNameSuffix = "-debug"
		}
		getByName("release") {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	flavorDimensions += "database"
	productFlavors {
		create("demo") {
			dimension = "database"
			applicationIdSuffix = ".demo"
		}
		create("full") {
			dimension = "database"
		}
	}

	playConfigs {
		register("demo") {
			enabled.set(false)
		}
	}

	kotlin {
		jvmToolchain(21)
	}

	ksp {
		arg("room.schemaLocation", "$projectDir/schemas")
	}

	lint {
		baseline = file("lint-baseline.xml")
	}
}

dependencies {
	implementation("androidx.activity:activity-ktx:1.10.1")
	implementation("androidx.appcompat:appcompat:1.7.1")
	implementation("androidx.constraintlayout:constraintlayout:2.2.1")
	implementation("androidx.core:core-ktx:1.16.0")
	implementation("androidx.core:core-splashscreen:1.0.1")
	implementation("androidx.exifinterface:exifinterface:1.4.1")
	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
	implementation("androidx.paging:paging-runtime-ktx:3.3.6")
	implementation("androidx.preference:preference-ktx:1.2.1")
	implementation("com.google.android.material:material:1.12.0")
	implementation("com.google.android.flexbox:flexbox:3.0.0")
	implementation("io.coil-kt.coil3:coil:3.2.0")

	val composeVersion = "1.8.3"
	implementation("androidx.compose.material:material:$composeVersion")
	implementation("androidx.compose.material:material-icons-extended-android:1.7.8")
	implementation("androidx.compose.ui:ui:$composeVersion")

	val roomVersion = "2.7.2"
	implementation("androidx.room:room-ktx:$roomVersion")
	implementation("androidx.room:room-paging:$roomVersion")
	implementation("androidx.room:room-runtime:$roomVersion")
	ksp("androidx.room:room-compiler:$roomVersion")

	val moshiVersion = "1.15.2"
	implementation("com.squareup.moshi:moshi:$moshiVersion")
	implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
	ksp("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")

	val markwonVersion = "4.6.2"
	implementation("io.noties.markwon:core:$markwonVersion")
	implementation("io.noties.markwon:html:$markwonVersion")

	implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.0")

	testImplementation("androidx.test.ext:junit-ktx:1.2.1")
	testImplementation("org.robolectric:robolectric:4.15.1")

	androidTestImplementation("androidx.test:runner:1.6.2")
	androidTestImplementation("androidx.test:rules:1.6.1")
	androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
	androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
}
