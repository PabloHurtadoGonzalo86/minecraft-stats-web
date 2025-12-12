plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.apptolast"
version = "0.0.1-SNAPSHOT"
description = "Minecraft Server Statistics Web"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	
	// Caffeine cache
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
	
	// Coroutines
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
	
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.bootJar {
	archiveFileName.set("minecraft-stats-web.jar")
}
