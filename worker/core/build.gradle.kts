plugins {
    id("proxy.library")
}

dependencies{
    implementation(libs.slf4j)

    testImplementation(libs.jupiter)
    testRuntimeOnly(libs.junitPlatform)
}
