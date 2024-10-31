plugins {
    id("proxy.application")
}

dependencies {
    implementation(project(":worker:core"))

    implementation(libs.slf4j)
    implementation(libs.netty)
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
    mainClass = "zlhywlf.proxy.server.Launcher"
}
