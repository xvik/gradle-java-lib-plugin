package ru.vyarus.gradle.plugin.lib

import groovy.transform.CompileStatic

/**
 * Java-lib plugin extension for fine-tuning. Accessible as `javaLib` closure.
 *
 * @author Vyacheslav Rusakov
 * @since 06.06.2021
 */
@CompileStatic
class JavaLibExtension {

    /**
     * Used with java-platform plugin when platform is declared in the root module to rename artifact (which is by
     * default equal to project name)
     */
    String bomArtifactId

    /**
     * Used with java-platform plugin when platform is declared in the root module to specify custom description in
     * the generated pom (otherwise it would be root project description).
     */
    String bomDescription

    /**
     * Gradle metadata publishing is enabled by default. Set to false to avoid metadata publishing.
     */
    boolean gradleMetadata = true

    /**
     * Disable gradle metadata publishing.
     */
    void disableGradleMetadata() {
        gradleMetadata = false
    }
}
