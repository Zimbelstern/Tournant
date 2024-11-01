buildscript {
	repositories {
		google()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:8.7.1")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
	}
}

tasks.register("clean", Delete::class) {
	delete(rootProject.layout.buildDirectory)
}
