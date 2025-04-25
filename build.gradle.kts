buildscript {
	repositories {
		google()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:8.9.2")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
	}
}

tasks.register("clean", Delete::class) {
	delete(rootProject.layout.buildDirectory)
}
