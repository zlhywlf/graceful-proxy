plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

version = "1.0.0"
group = "zlhywlf.proxy"

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
