plugins {
    id("proxy.application")
}

dependencies{
    implementation(project(":worker:core"))

    implementation(libs.slf4j)

    testImplementation(libs.jupiter)
    testRuntimeOnly(libs.junitPlatform)
}

application {
    mainClass = "zlhywlf.proxy.server.Launcher"
}
