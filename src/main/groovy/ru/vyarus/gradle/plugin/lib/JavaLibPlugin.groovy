package ru.vyarus.gradle.plugin.lib

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.java.archives.Attributes
import org.gradle.api.plugins.*
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.reporting.ConfigurableReport
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.reporting.dependencies.HtmlDependencyReportTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.process.internal.JvmOptions
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.util.GradleVersion
import ru.vyarus.gradle.plugin.pom.PomPlugin

import java.nio.charset.StandardCharsets

/**
 * Plugin performs common configuration for java or groovy library:
 * <ul>
 *     <li> Fills manifest for main jar, add pom and generated pom.properties inside jar (as maven do)
 *     <li> Add sourcesJar and javadocJar tasks (gradle native way)
 *     <li> Configures maven publication named 'maven' (with all jars)
 *     <li> Applies 'ru.vyarus.pom' plugin which fixes pom dependencies, adds 'pom' closure for simpler pom
 *     configuration
 *     <li> Configure signing if "signing" plugin applied
 *     <li> Add 'install' task as shortcut for publishToMavenLocal
 * </ul>
 * <p>
 * When used with java-platform plugin:
 * <ul>
 *     <li> Applies 'ru.vyarus.pom' plugin
 *     <li> Configure `bom` maven publication
 *     <li> Configure signing if "signing" plugin applied
 *     <li> Add 'install' task as shortcut for publishToMavenLocal
 *     <li> Activates platform dependencies (javaPlatform.allowDependencies())
 * </ul>
 * Java-publish might be used in the root project (for multi-module project) and in this case custom artifact name
 * would be required (most likely, BOM artifact should not be called the same as project name. For this case
 * special extension must be used: {@code libJava.bom.artifactId = 'something-bom'}. Name in the generated pom would be
 * changed accordingly. Also, {@code libJava.bom.description} may be used to specify custom description instead
 * of root project description.
 * <p>
 * Also, in the root project reports aggregation might be enabled with {@code javaLib.aggregateReports()}.
 * When enabled aggregates test reports, coverage report (jacoco plugin) and dependency report (project-report plugin).
 * Aggregation might be used with at least "base" plugin (but may be also with java-platform).
 * <p>
 * In case of gradle plugin (java-gradle-plugin + plugin-publish) "pluginMaven" publication will be created for
 * plugins portal publication, but java-lib plugin will still use "maven" publication (for maven central
 * publication).
 * Overall, in case of gradle plugin, 2 maven publications should be used, but exactly the same artifacts would
 * be published everywhere (plugin portal will additionally receive alias publications).
 *
 * @author Vyacheslav Rusakov
 * @since 07.11.2015
 * @see JavaLibExtension for options (javaLib closure in gradle)
 */
@SuppressWarnings('DuplicateStringLiteral')
@CompileStatic(TypeCheckingMode.SKIP)
class JavaLibPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // always creating extension to avoid hard to track mis-references in multi-module projects
        JavaLibExtension extension = project.extensions.create('javaLib', JavaLibExtension, project)

        // partial activation for java-platform plugin (when root module is a BOM)
        project.plugins.withType(JavaPlatformPlugin) {
            project.plugins.apply(PomPlugin)
            // different name used for publication
            MavenPublication bom = configureBomPublication(project)
            configurePlatform(project, extension, bom)
            configureGradleMetadata(project, extension)
            disablePublication(project, extension)
            configureSigning(project, bom)
            addInstallTask(project, extension) {
                project.logger.warn "INSTALLED $project.group:$bom.artifactId:$project.version"
            }
        }

        // full activation when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            project.plugins.apply(PomPlugin)
            // assume gradle 5.0 and above - stable publishing enabled
            MavenPublication publication = configureMavenPublication(project)
            configureEncoding(project)
            configureJar(project, publication)
            addJavadocAndSourceJars(project, extension)
            configureGradleMetadata(project, extension)
            disablePublication(project, extension)
            configureSigning(project, publication)
            applyAutoModuleName(project, extension)
            enableJacocoXmlReport(project)
            addInstallTask(project, extension) {
                project.logger.warn "INSTALLED $project.group:$project.name:$project.version"
            }
        }

        // extension applied with base plugin because it's often used in the root project for grouping
        project.plugins.withType(BasePlugin) {
            aggregateReports(project, extension)
        }

        // helper task to open dependency report in browser
        addOpenDependencyReportTask(project)
    }

    private void configurePlatform(Project project, JavaLibExtension extension, MavenPublication bom) {
        project.configure(project) {
            // allow dependencies declaration in BOM
            javaPlatform.allowDependencies()

            afterEvaluate {
                if (extension.bom?.artifactId) {
                    // by default artifact name is project name and if root bom would be published it should
                    // have a different name
                    bom.artifactId = extension.bom.artifactId
                    pom {
                        name extension.bom.artifactId
                    }
                }
                if (extension.bom?.description) {
                    pom {
                        description extension.bom.description
                    }
                }
            }
        }
    }

    private void configureEncoding(Project project) {
        project.tasks.withType(JavaCompile).configureEach {
            it.options.encoding = StandardCharsets.UTF_8
        }

        project.tasks.withType(GroovyCompile).configureEach {
            it.options.encoding = StandardCharsets.UTF_8
        }

        project.tasks.withType(Test).configureEach {
            it.systemProperty JvmOptions.FILE_ENCODING_KEY, StandardCharsets.UTF_8
        }

        project.tasks.withType(Javadoc).configureEach {
            it.with {
                options.encoding = StandardCharsets.UTF_8
                // StandardJavadocDocletOptions
                options.charSet = StandardCharsets.UTF_8
                options.docEncoding = StandardCharsets.UTF_8
            }
        }
    }

    private void configureJar(Project project, MavenPublication publication) {
        project.tasks.register('generatePomPropertiesFile') {
            it.with {
                inputs.properties([
                        'version': "${ -> project.version }",
                        'groupId': "${ -> project.group }",
                        'artifactId': "${ -> project.name }",
                ])
                outputs.file project.layout.buildDirectory.file('generatePomPropertiesFile/pom.properties')
                doLast {
                    File file = outputs.files.singleFile
                    file.parentFile.mkdirs()
                    file << inputs.properties.collect { key, value -> "$key: $value" }.join('\n')
                }
            }
        }
        project.configure(project) {
            // delayed to be able to use version
            afterEvaluate {
                // do not override user attributes
                tasks.named('jar').configure {
                    Attributes attributes = it.manifest.attributes
                    putIfAbsent(attributes, 'Implementation-Title', project.description ?: project.name)
                    putIfAbsent(attributes, 'Implementation-Version', project.version)
                    putIfAbsent(attributes, 'Built-By', System.getProperty('user.name'))
                    putIfAbsent(attributes, 'Built-Date', new Date())
                    putIfAbsent(attributes, 'Built-JDK', System.getProperty('java.version'))
                    putIfAbsent(attributes, 'Built-Gradle', project.gradle.gradleVersion)
                    putIfAbsent(attributes, 'Target-JDK', project.extensions
                            .getByType(JavaPluginExtension).targetCompatibility)
                }
            }

            project.tasks.named('processResources', Copy).configure {
                it.with {
                    into("META-INF/maven/$project.group/$project.name") {
                        from project.tasks.named("generatePomFileFor${publication.name.capitalize()}Publication")
                        rename '.*.xml', 'pom.xml'
                        from project.tasks.named('generatePomPropertiesFile')
                    }
                }
            }
        }
    }

    @SuppressWarnings('Indentation')
    private void addJavadocAndSourceJars(Project project, JavaLibExtension extension) {
        // use gradle native configuration method to aovid clashes with plugin-publish 1.0
        JavaPluginExtension javaExt = project.extensions.getByType(JavaPluginExtension)
        javaExt.withJavadocJar()
        javaExt.withSourcesJar()
        project.afterEvaluate {
            if (!extension.addJavadoc || !extension.addSources) {
                project.extensions.getByType(JavaPluginExtension).sourceSets
                        .named('main').configure { sourceSet ->
                    if (!extension.addJavadoc) {
                        project.tasks.named(sourceSet.javadocJarTaskName).configure {
                            it.enabled = false
                        }
                    }
                    if (!extension.addSources) {
                        project.tasks.named(sourceSet.sourcesJarTaskName).configure {
                            it.enabled = false
                        }
                    }
                }
            }
        }
    }

    private MavenPublication configureMavenPublication(Project project) {
        // Configure publication:
        // java-gradle-plugin will create its own publication pluginMaven, but still plugin configures separate
        // maven publication because java-gradle-plugin most likely will be applied after java-lib and so
        // it's not possible to detect it for sure. But it's not a problem: pom will be corrected for both
        // (and in any case portal does not use this pom).
        // NOTE: java-gradle-plugin will also create alias publications for each plugin, but we will simply don't
        // use them
        MavenPublication publication = project.extensions
                .findByType(PublishingExtension)
                .publications
                .maybeCreate('maven', MavenPublication)
        publication.from(project.components.getByName('java'))
        // in stable publication mode extra jars added directly after tasks registration
        return publication
    }

    private MavenPublication configureBomPublication(Project project) {
        MavenPublication publication = project.extensions
                .findByType(PublishingExtension)
                .publications
                .maybeCreate('bom', MavenPublication)
        publication.from(project.components.getByName('javaPlatform'))
        // in stable publication mode extra jars added directly after tasks registration
        return publication
    }

    private void addInstallTask(Project project, JavaLibExtension extension, Closure last) {
        project.tasks.register('install') {
            it.with {
                dependsOn 'publishToMavenLocal'
                group = 'publishing'
                description = 'Publish to local maven repository (alias for publishToMavenLocal)'
                doLast {
                    // show message only if publication not disabled
                    if (extension.publication) {
                        last.call()
                    }
                }
            }
        }
    }

    private void configureGradleMetadata(Project project, JavaLibExtension extension) {
        project.afterEvaluate {
            // disable gradle metadata publishing (usually it confuse a lot)
            if (!extension.gradleMetadata) {
                project.tasks.withType(GenerateModuleMetadata).configureEach {
                    enabled = false
                }
            }
        }
    }

    private void disablePublication(Project project, JavaLibExtension extension) {
        project.afterEvaluate {
            if (!extension.publication) {
                project.tasks.withType(PublishToMavenRepository).configureEach {
                    enabled = extension.publication
                }
                project.tasks.withType(PublishToMavenLocal).configureEach {
                    enabled = false
                }
            }
        }
    }

    private void configureSigning(Project project, MavenPublication publication) {
        project.plugins.withType(SigningPlugin) {
            // https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
            SigningExtension ext = project.extensions.getByType(SigningExtension)
            ext.sign publication
            ext.required = { !project.version.toString().endsWith('SNAPSHOT') }
        }
    }

    private void applyAutoModuleName(Project project, JavaLibExtension extension) {
        project.afterEvaluate {
            if (extension.autoModuleName) {
                // java 11 auto module name
                (project.tasks.jar as Jar).manifest {
                    attributes 'Automatic-Module-Name': extension.autoModuleName
                }
            }
        }
    }

    private void addOpenDependencyReportTask(Project project) {
        project.plugins.withType(ProjectReportsPlugin) {
            project.tasks.register('openDependencyReport').configure {
                (it as DefaultTask).with {
                    group = 'help'
                    dependsOn 'htmlDependencyReport'
                    description = 'Opens gradle htmlDependencyReport in browser'
                    // prevent calling task on all subprojects
                    impliesSubProjects = true
                    doLast {
                        File report = project.file("build/reports/project/dependencies/root.${project.name}.html")
                        if (!report.exists()) {
                            // for multi-module project root, if reports aggregation enabled name would be different
                            report = project.file('build/reports/project/dependencies/root.html')
                        }
                        java.awt.Desktop.desktop.open(report)
                    }
                }
            }
        }
    }

    private void enableJacocoXmlReport(Project project) {
        // by default jacoco xml report is disabled, but its required for coverage services
        project.plugins.withType(JacocoPlugin) {
            project.tasks.named('jacocoTestReport').configure {
                (it as JacocoReport).reports.xml.required.set(true)
            }
        }
    }

    @SuppressWarnings(['AbcMetric', 'MethodSize'])
    private void aggregateReports(Project project, JavaLibExtension extension) {
        project.afterEvaluate {
            if (!extension.aggregatedReports) {
                return
            }
            // makes no sense otherwise
            if (project.subprojects.empty) {
                throw new GradleException('javaLib.aggregateReports() could not be used on project ' +
                        "'$project.name' because does not contain subprojects")
            }
            if (project.plugins.hasPlugin(JavaPlugin)) {
                throw new GradleException('javaLib.aggregateReports() could not be used on project ' +
                        "'$project.name' because it contains java sources. If this is a root project use 'base' " +
                        'plugin instead.')
            }

            // aggregate test reports from subprojects
            project.tasks.register('test', TestReport) {
                it.with {
                    description = 'Generates aggregated test report'
                    // show task in common place
                    group = 'verification'
                    if (GradleVersion.current() < GradleVersion.version('8.0')) {
                        destinationDir = project.file("${project.buildDir}/reports/tests/test")
                        reportOn project.subprojects
                                .findAll { it.plugins.hasPlugin(JavaPlugin) }.test
                    } else {
                        destinationDirectory.set(project.layout.buildDirectory.dir('reports/tests/test'))
                        testResults.from(project.subprojects
                                .findAll { it.plugins.hasPlugin(JavaPlugin) }.test)
                    }
                }
            }

            // aggregate jacoco coverage from subprojects
            project.plugins.withType(JacocoPlugin) {
                Set<Project> projectsWithCoverage = project.subprojects
                        .findAll { it.plugins.hasPlugin(JacocoPlugin) }

                project.tasks.register('jacocoTestReport', JacocoReport) {
                    it.with {
                        description = 'Generates aggregated jacoco coverage report'
                        dependsOn 'test'
                        // show task in common place
                        group = 'verification'
                        executionData project.files(projectsWithCoverage
                                .collect { it.layout.buildDirectory.file('jacoco/test.exec') })
                                .filter { it.exists() }
                        sourceDirectories.from = selectFiles(projectsWithCoverage) { SourceSetContainer sourceSets ->
                            sourceSets.main.allSource.srcDirs
                        }
                        classDirectories.from = selectFiles(projectsWithCoverage) { SourceSetContainer sourceSets ->
                            sourceSets.main.output.files
                        }
                        // use same location as in single-module case
                        reportDestination(reports.xml, project, 'reports/jacoco/test/jacocoTestReport.xml')
                        reportDestination(reports.html, project, 'reports/jacoco/test/html/')
                        reports.xml.required.set(true)
                    }
                }
            }

            // aggregated html dependency report
            project.plugins.withType(ProjectReportsPlugin) {
                project.tasks.named('htmlDependencyReport').configure {
                    (it as HtmlDependencyReportTask).projects = project.allprojects
                }
            }
        }
    }

    private Set<File> selectFiles(Set<Project> projects, Closure<Set<File>> extractor) {
        Set<File> res = []
        projects.forEach {
            res.addAll(
                    extractor.call(
                            GradleVersion.current() < GradleVersion.version('7.6')
                                    ? it.sourceSets
                                    : it.extensions.getByType(JavaPluginExtension).sourceSets // added in 7.1
                    )
            )
        }
        res
    }

    private void putIfAbsent(Attributes attributes, String name, Object value) {
        if (!attributes.containsKey(name)) {
            attributes.put(name, value)
        }
    }

    @SuppressWarnings('Instanceof')
    private void reportDestination(ConfigurableReport report, Project project, String path) {
        if (GradleVersion.current() < GradleVersion.version('8.0')) {
            report.destination = project.file("$project.buildDir/$path")
        } else {
            if (report instanceof SingleFileReport) {
                report.outputLocation.set(project.layout.buildDirectory.get().file(path))
            } else {
                report.outputLocation.set(project.layout.buildDirectory.dir(path))
            }
        }
    }
}
