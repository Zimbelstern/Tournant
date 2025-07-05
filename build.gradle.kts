buildscript {
	repositories {
		google()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:8.11.0")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
	}
}

tasks.register("clean", Delete::class) {
	delete(rootProject.layout.buildDirectory)
}
