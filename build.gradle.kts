buildscript {
	repositories {
		google()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:8.5.0")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
	}
}

tasks.register("clean", Delete::class) {
	delete(rootProject.buildDir)
}
