plugins {
	id("com.android.application")
	id("kotlin-android")
	id("kotlin-parcelize")
	id("kotlin-kapt")
	id("com.google.devtools.ksp") version "1.9.10-1.0.13"
	id("org.jetbrains.kotlin.android")
}

android {
	compileSdk = 34

	defaultConfig {
		applicationId = "eu.zimbelstern.tournant"
		minSdk = 21
		targetSdk = 34
		versionCode = 24
		versionName = "2.7.2"

		ksp {
			arg("room.schemaLocation", "$projectDir/schemas")
		}

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	applicationVariants.configureEach {
		resValue("string", "versionName", versionName)
	}

	buildFeatures {
		viewBinding = true
		dataBinding = true
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

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions {
		jvmTarget = "17"
	}

	namespace = "eu.zimbelstern.tournant"
}

tasks.register("assembleReleaseSigned", Exec::class) {
	group = "zimbelstern"
	description = "Assembles a release APK and signs it with my key"
	dependsOn("assembleRelease")
	commandLine("../zimbelstern/sign.sh")
	args(
		workingDir.path
	)
}

tasks.register("deployRelease", Exec::class) {
	group = "zimbelstern"
	description = "Deploys signed release APK to my F-Droid repository"
	dependsOn("assembleReleaseSigned")
	commandLine("../zimbelstern/deploy.sh")
	args(
		workingDir.path,
		android.defaultConfig.applicationId,
		android.defaultConfig.applicationId!!.split('.').last(),
		android.defaultConfig.versionCode,
		android.defaultConfig.versionName
	)
}

dependencies {
	implementation("androidx.core:core-ktx:1.13.1")
	implementation("androidx.appcompat:appcompat:1.7.0")
	implementation("androidx.constraintlayout:constraintlayout:2.1.4")
	implementation("androidx.preference:preference-ktx:1.2.1")
	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
	implementation("androidx.paging:paging-runtime-ktx:3.3.0")
	implementation("com.google.android.material:material:1.12.0")
	implementation("com.google.android.flexbox:flexbox:3.0.0")
	implementation("com.github.bumptech.glide:glide:4.16.0")
	implementation("androidx.core:core-splashscreen:1.0.1")

	val roomVersion = "2.6.1"
	implementation("androidx.room:room-ktx:$roomVersion")
	implementation("androidx.room:room-runtime:$roomVersion")
	implementation("androidx.room:room-paging:$roomVersion")
	ksp("androidx.room:room-compiler:$roomVersion")

	val moshiVersion = "1.15.0"
	implementation("com.squareup.moshi:moshi:$moshiVersion")
	ksp("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")

	val markwonVersion = "4.6.2"
	implementation("io.noties.markwon:core:$markwonVersion")
	implementation("io.noties.markwon:html:$markwonVersion")

	testImplementation("androidx.test.ext:junit-ktx:1.2.1")
	testImplementation("org.robolectric:robolectric:4.12")

	androidTestImplementation("androidx.test:runner:1.6.1")
	androidTestImplementation("androidx.test:rules:1.6.1")
	androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
	androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
	androidTestImplementation("tools.fastlane:screengrab:2.1.1")
}
