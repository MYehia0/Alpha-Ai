plugins {
    id("alpha.jvm.library")
}

dependencies {
    // Only the @Qualifier annotation is used, for the dispatcher qualifiers.
    api(libs.javax.inject)
}
