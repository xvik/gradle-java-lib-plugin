package ru.vyarus.gradle.plugin.lib

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact
import org.gradle.api.java.archives.Attributes
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.ProjectReportsPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.process.internal.JvmOptions
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoMerge
import org.gradle.testing.jacoco.tasks.JacocoReport
import ru.vyarus.gradle.plugin.pom.PomPlugin

import java.nio.charset.StandardCharsets

/**
 * Plugin performs common configuration for java or groovy library:
 * <ul>
 *     <li> Fills manifest for main jar, add pom and generated pom.properties inside jar (as maven do)
 *     <li> Add sourcesJar task
 *     <li> Add javadocJar or (and) groovydocJar tasks
 *     <li> Configures maven publication named 'maven' with all jars (jar, sources, javadoc and maybe groovydoc)
 *     <li> Applies 'ru.vyarus.pom' plugin which fixes pom dependencies, adds 'pom' closure fot simpler pom
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
 * publication). Plugin-publish also creates it's own javadoc and sources tasks, so manually registering
 * artifacts (only) to prevent this.
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

    private static final String BUILD_GROUP = 'build'
    private static final String JAVADOC_JAR = 'javadocJar'
    private static final String GROOVYDOC_JAR = 'groovydocJar'

    @Override
    void apply(Project project) {
        // always creating extension to avoid hard to track mis-references in multi-module projects
        JavaLibExtension extension = project.extensions.create('javaLib', JavaLibExtension, project)

        // partial activation for java-platform plugin (when root module is a BOM)
        // not by type because plugin was added in gradle 5.2
        project.plugins.withId('java-platform') {
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
            addSourcesJarTask(project, publication, extension)
            addJavadocJarTask(project, publication, extension)
            addGroovydocJarTask(project, publication, extension)
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
                outputs.file "$project.buildDir/generatePomPropertiesFile/pom.properties"
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
                    putIfAbsent(attributes, 'Target-JDK', project.targetCompatibility)
                }
            }

            project.tasks.named('processResources', Copy).configure {
                it.with {
                    into("META-INF/maven/$project.group/$project.name") {
                        from project.tasks.getByName("generatePomFileFor${publication.name.capitalize()}Publication")
                        rename '.*.xml', 'pom.xml'
                        from project.tasks.getByName('generatePomPropertiesFile')
                    }
                }
            }
        }
    }

    private void addSourcesJarTask(Project project, MavenPublication publication, JavaLibExtension extension) {
        TaskProvider<Jar> sourcesJar = project.tasks.register('sourcesJar', Jar) {
            it.with {
                dependsOn project.tasks.named('classes')
                group = BUILD_GROUP
                description = 'Assembles a jar archive containing the main sources'
                from project.sourceSets.main.allSource
                archiveClassifier.set('sources')
            }
        }
        registerArtifact(project, publication, sourcesJar)
        project.afterEvaluate {
            sourcesJar.configure { enabled = extension.addSources }
        }
    }

    private void addJavadocJarTask(Project project, MavenPublication publication, JavaLibExtension extension) {
        // apply only if java sources exist
        boolean hasJavaSources = project.sourceSets.main.java.srcDirs.find { it.exists() }
        if (hasJavaSources) {
            TaskProvider<Jar> javadocJar = project.tasks.register(JAVADOC_JAR, Jar) {
                it.with {
                    dependsOn project.tasks.javadoc
                    group = BUILD_GROUP
                    description = 'Assembles a javadoc jar'
                    archiveClassifier.set('javadoc')
                    // configuration is delayed so it is ok to reference task instance here
                    from project.tasks.javadoc.destinationDir
                }
            }
            registerArtifact(project, publication, javadocJar)
            project.afterEvaluate {
                javadocJar.configure { enabled = extension.addJavadoc }
            }
        }
    }

    private void addGroovydocJarTask(Project project, MavenPublication publication, JavaLibExtension extension) {
        // apply only if groovy enabled
        project.plugins.withType(GroovyPlugin) {
            // apply only if groovy sources exist
            boolean hasGroovySources = project.sourceSets.main.groovy.srcDirs.find { it.exists() }
            if (hasGroovySources) {
                TaskProvider<Jar> groovydocJar = project.tasks.register(GROOVYDOC_JAR, Jar) {
                    it.with {
                        dependsOn project.tasks.groovydoc
                        group = BUILD_GROUP
                        description = 'Assembles a groovydoc jar'
                        // very important to have at least one javadoc package, because otherwise maven cantral
                        // would not accept package
                        archiveClassifier.set(project.tasks.findByName(JAVADOC_JAR) ? 'groovydoc' : 'javadoc')
                        from project.tasks.groovydoc.destinationDir
                    }
                }
                registerArtifact(project, publication, groovydocJar)
                project.afterEvaluate {
                    groovydocJar.configure { enabled = extension.addJavadoc }
                }
            }
        }
    }

    private MavenPublication configureMavenPublication(Project project) {
        // java-gradle-plugin will create its own publication pluginMaven, but still plugin configures separate
        // maven publication because java-gradle-plugin most likely will be applied after java-lib and so
        // it's not possible to detect it for sure. But it's not a problem: pom will be corrected for both
        // (and in any case portal does not use this pom). More importantly, to prevent plugin-publish to create
        // its own javadoc and sources tasks (its done in later in artifacts method)
        // NOTE: java-gradle-plugin will also create alias publications for each plugin, but we will simply don't
        // use them during bintray publication
        // configure publication
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
            project.signing.sign publication
            project.signing.required = { !project.version.toString().endsWith('SNAPSHOT') }
        }
    }

    private void applyAutoModuleName(Project project, JavaLibExtension extension) {
        project.afterEvaluate {
            if (extension.autoModuleName) {
                // java 11 auto module name
                project.tasks.jar.manifest {
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
            JacocoReport task = project.tasks.findByName('jacocoTestReport') as JacocoReport
            if (task) {
                task.reports.xml.enabled = true
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
                    destinationDir = project.file("${project.buildDir}/reports/tests/test")
                    reportOn project.subprojects
                            .findAll { it.plugins.hasPlugin(JavaPlugin) }.test
                }
            }

            // aggregate jacoco coverage from subprojects
            project.plugins.withType(JacocoPlugin) {
                Set<Project> projectsWithCoverage = project.subprojects
                        .findAll { it.plugins.hasPlugin(JacocoPlugin) }
                TaskProvider jacocoMerge = project.tasks.register('jacocoMerge', JacocoMerge) {
                    it.with {
                        // test report would trigger all tests in subprojects
                        dependsOn('test')
                        // no group specified because it's a helper task not intended to be called directly
                        description = 'Merge jacoco coverage results for aggregated report generation'
                        // use same name as in modules for unification
                        destinationFile = project.file("${project.buildDir}/jacoco/test.exec")
                        executionData = project.files(projectsWithCoverage
                                .collect { it.file("${it.buildDir}/jacoco/test.exec") })
                    }
                }

                project.tasks.register('jacocoTestReport', JacocoReport) {
                    it.with {
                        description = 'Generates aggregated jacoco coverage report'
                        dependsOn jacocoMerge
                        // show task in common place
                        group = 'verification'
                        executionData jacocoMerge.get().destinationFile
                        sourceDirectories.from = project.files(projectsWithCoverage.sourceSets.main.allSource.srcDirs)
                        classDirectories.from = project.files(projectsWithCoverage.sourceSets.main.output)
                        // use same location as in single-module case
                        reports.xml.destination = project
                                .file("$project.buildDir/reports/jacoco/test/jacocoTestReport.xml")
                        reports.xml.enabled = true
                        reports.html.destination = project
                                .file("$project.buildDir/reports/jacoco/test/html/")
                    }
                }
            }

            // aggregated html dependency report
            project.plugins.withType(ProjectReportsPlugin) {
                project.htmlDependencyReport.projects = project.allprojects
            }
        }
    }

    private void putIfAbsent(Attributes attributes, String name, Object value) {
        if (!attributes.containsKey(name)) {
            attributes.put(name, value)
        }
    }

    private void registerArtifact(Project project, MavenPublication publication, TaskProvider task) {
        // https://github.com/gradle/gradle/issues/6246
        PublishArtifact artifact = new LazyPublishArtifact(task)

        // maven publication registration
        publication.artifact artifact

        // plugin-publish create its own sources and javadoc tasks, but only if
        // archives configuration contains anything except main jar, so register custom tasks directly to avoid
        // duplicate artifacts (see PublishPlugin implementation)
        if (project.plugins.hasPlugin('com.gradle.plugin-publish')) {
            Configuration archives = project.configurations.findByName('archives')
            if (archives && !archives.artifacts.contains(artifact)) {
                project.artifacts.add('archives', artifact)
            }
        }
    }

}
