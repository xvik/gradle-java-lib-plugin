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

    JavaLibExtension(Project project) {
        this.project = project
    }

    /**
     * Java-platform plugin related configurations.
     */
    JavaPlatform bom = new JavaPlatform()

    /**
     * Automatic-Module-Name meta-inf property value (java 11 modules).
     * Object used for type to allow lazy-evaluated GStrings usage.
     */
    Object autoModuleName

    // -----------------------------------  method-based configuration
    //                                      (properties accessible, but not supposed to be used)

    boolean gradleMetadata = true
    boolean addJavadoc = true
    boolean addSources = true
    boolean signSnapshots = false
    boolean aggregatedReports = false

    /**
     * Disable gradle metadata publishing. Metadata files contains additional gradle dependencies semantic which
     * is impossible to express in pom file. In majority of cases this file is not required and may be excluded
     * to avoid publishing additional artifact (besides, some repos might complain about it).
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
     * Enables artifacts signing for snapshots (disabled by default). By default, signing for snapshot versions
     * disabled to simplify builds (no additional certificates configuration required to build project).
     */
    void enableSnapshotsSigning() {
        signSnapshots = true
    }

    /**
     * Aggregate test, jacoco coverage and dependency reports for subprojects (assuming 1 level hierarchy).
     * This option will work with "base" plugin (often used in root project to allow grouping).
     * <p>
     * IMPORTANT: must be used in the root project or any subproject containing other subprojects.
     * <p>
     * For jacoco reports aggregation jacoco plugin must be active. Jacoco report aggregation is important for
     * coverage services integration (they require single aggregated report; when aggregation enabled, report
     * path would be the same as with single module: build/reports/jacoco/test/jacocoTestReport.xml)
     * <p>
     * Dependencies html report grouping activated if report-plugin is applied.
     */
    void aggregateReports() {
        aggregatedReports = true
    }

    // -----------------------------------  Utility methods required for sub objects configuration

    /**
     * Bom sub-object configuration. Used only with java-platform plugin when platform declared in the root project.
     *
     * @param config configuration closure
     */
    void bom(Closure<?> config) {
        project.configure(bom, config)
    }

    /**
     * Bom  sub-object configuration. Used only with java-platform plugin when platform declared in the root project.
     *
     * @param config configuration action
     */
    void bom(Action<JavaPlatform> config) {
        config.execute(bom)
    }

    /**
     * Shortcut for pom plugin configuration under javaLib.pom closure instead of pomGeneration (unification).
     *
     * @param config configuration closure
     */
    void pom(Closure<?> config) {
        project.configure(pom, config)
    }

    /**
     * Shortcut for pom plugin configuration under javaLib.pom closure instead of pomGeneration (unification).
     *
     * @param config configuration action
     */
    void pom(Action<PomExtension> config) {
        config.execute(pom)
    }

    /**
     * Shortcut for pom plugin configuration extension access. Required for cases like:
     * <pre>javaLib.pom.forceVersions()</pre>
     *
     * @return pom plugin configuration extension
     */
    PomExtension getPom() {
        PomExtension pom = project.extensions.findByType(PomExtension)
        if (pom == null) {
            throw new IllegalStateException("No pom plugin extension found in project '$project.name'")
        }
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
