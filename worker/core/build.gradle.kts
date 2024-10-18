plugins {
    id("proxy.library")
}

dependencies{
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.jupiter)
    testRuntimeOnly(libs.junitPlatform)
}
