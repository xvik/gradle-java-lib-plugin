package ru.vyarus.gradle.plugin.lib

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.pom.PomExtension

/**
 * Java-lib plugin extension for fine-tuning. Accessible as `javaLib` closure.
 *
 * @author Vyacheslav Rusakov
 * @since 06.06.2021
 */
@CompileStatic
@SuppressWarnings('ConfusingMethodName')
class JavaLibExtension {

    private final Project project
    private final PomExtension pom;

    JavaLibExtension(Project project) {
        this.project = project
        this.pom = project.extensions.findByType(PomExtension)
    }
/**
 * Java-platform plugin related configurations.
 */
    JavaPlatform bom = new JavaPlatform()

    /**
     * Gradle metadata publishing is enabled by default. Set to false to avoid metadata publishing.
     */
    boolean gradleMetadata = true

    /**
     * Javadoc (groovydoc) artifact addition. Enabled by default.
     * Ignored with java-publish plugin.
     */
    boolean addJavadoc = true

    /**
     * Sources artifact addition. Enabled by default.
     * Ignored with java-publish plugin.
     */
    boolean addSources = true
    /**
     * Do not sign snapshot versions (so project could be built without additional certificates configuration).
     */
    boolean signSnapshots = false

    /**
     * Disable gradle metadata publishing.
     */
    void disableGradleMetadata() {
        gradleMetadata = false
    }

    /**
     * Disable javadoc (groovydoc) publication.
     * Ignored with java-publish plugin.
     */
    void disableJavadocPublish() {
        addJavadoc = false
    }

    /**
     * Disable sources publication.
     * Ignored with java-publish plugin.
     */
    void disableSourcesPublish() {
        addSources = false
    }

    /**
     * Enables artifacts signing for snapshots (disabled by default).
     */
    void enableSnapshotsSigning() {
        signSnapshots = true
    }

    // Utility methods required for sub objects configuration

    void bom(Closure<?> config) {
        project.configure(bom, config)
    }

    void bom(Action<JavaPlatform> config) {
        config.execute(bom)
    }

    // shortcuts for pom plugin configuration under javaLib.pom closure instead of pomGeneration
    // (unification shortcuts)
    void pom(Closure<?> config) {
        project.configure(pom, config)
    }

    void pom(Action<PomExtension> config) {
        config.execute(pom)
    }

    /**
     * Shortcut for pom plugin configuration extension access. Required for cases like:
     * {@code javaLib.pom.forceVersions( )}
     *
     * @return pom plugin configuration extension
     */
    PomExtension getPom() {
        return pom
    }

    /**
     * Configuration for java-platform plugin. Required for case when java-platform is declared in the root project,
     * but published bom maven coordinates must differ from root project name.
     */
    static class JavaPlatform {
        /**
         * Used with java-platform plugin when platform is declared in the root module to rename artifact (which is by
         * default equal to project name)
         */
        String artifactId

        /**
         * Used with java-platform plugin when platform is declared in the root module to specify custom description in
         * the generated pom (otherwise it would be root project description).
         */
        String description
    }
}
