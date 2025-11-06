// Empty root build file
// Plugin versions are managed in subprojects

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
