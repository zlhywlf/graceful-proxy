plugins {
    application
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

dependencies {
    implementation(libs.nettyHttp)
    implementation(libs.nettyEpoll)
    implementation(libs.slf4j)
    implementation(libs.commonsLang3)
    implementation(libs.cli)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.httpclient)
    testImplementation(libs.jetty)
    testImplementation(libs.jupiter)
    testRuntimeOnly(libs.junitPlatform)
}

application {
    mainClass = "zlhywlf.proxy.Launcher"
}
