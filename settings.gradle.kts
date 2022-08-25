pluginManagement {
    plugins {
        val kotlinVersion: String by settings
        val ktlintVersion: String by settings
        val versionUpdatesVersion: String by settings
        val dependencyAnalysisVersion: String by settings
        val sonarqubeVersion: String by settings

        kotlin("jvm") version kotlinVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintVersion
        id("com.github.ben-manes.versions") version versionUpdatesVersion
        id("com.autonomousapps.dependency-analysis") version dependencyAnalysisVersion
        id("org.sonarqube") version sonarqubeVersion
    }
}
