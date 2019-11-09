package ru.vyarus.gradle.plugin.lib

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.java.archives.Attributes
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.process.internal.JvmOptions
import org.gradle.util.GradleVersion
import ru.vyarus.gradle.plugin.pom.PomPlugin

import javax.inject.Inject

/**
 * Plugin performs common configuration for java or groovy library:
 * <ul>
 *     <li> Fills manifest for main jar, add pom and generated pom.properties inside jar (as maven do)
 *     <li> Add sourcesJar task
 *     <li> Add javadocJar or (and) groovydocJar tasks
 *     <li> Configures maven publication named 'maven' with all jars (jar, sources, javadock and maybe groovydoc)
 *     <li> Applies 'ru.vyarus.pom' plugin which fixes pom dependencies, adds provided and optional support and
 *     'pom' closure fot simpler pom configuration
 *     <li> Add 'install' task as shortcut for publishToMavenLocal
 * </ul>
 * Plugin must be registered after java or groovy plugins, otherwise wil do nothing.
 *
 * @author Vyacheslav Rusakov
 * @since 07.11.2015
 */
@SuppressWarnings('DuplicateStringLiteral')
@CompileStatic(TypeCheckingMode.SKIP)
class JavaLibPlugin implements Plugin<Project> {

    private static final String BUILD_GROUP = 'build'
    private static final String JAVADOC_JAR = 'javadocJar'
    private static final String GROOVYDOC_JAR = 'groovydocJar'

    private final FeaturePreviews previews
    private boolean stablePublishing

    // plugin will fail to initialize on gradle before 4.6
    @Inject
    JavaLibPlugin(FeaturePreviews featurePreviews) {
        this.previews = featurePreviews
    }

    @Override
    void apply(Project project) {
        // activated only when java plugin is enabled
        project.plugins.withType(JavaPlugin) {
            project.plugins.apply(PomPlugin)
            /*
             In stable mode (>= 4.8) publication is registered immediately with main jar and other jars are
             added directly after tasks registration (workaround to allow easy overriding of artifacts in
             user build file). In theory, legacy mode should work with stable publishing, but it doesn't work
             under gradle plugins syntax (and works when plugin is applied in the old way).
             In legacy mode (when publication is lazily registered) all jars are registered at once
             */
            stablePublishing = isStablePublishingEnabled()
            if (stablePublishing) {
                configureStableMavenPublication(project)
            }

            configureEncoding(project)
            configureJar(project)
            addSourcesJarTask(project)
            addJavadocJarTask(project)
            addGroovydocJarTask(project)
            if (!stablePublishing) {
                configureLazyMavenPublication(project)
            }
            addInstallTask(project)
        }
    }

    private void configureEncoding(Project project) {
        project.tasks.withType(JavaCompile) {
            options.encoding = 'UTF-8'
        }

        project.tasks.withType(GroovyCompile) {
            options.encoding = 'UTF-8'
        }

        project.tasks.withType(Test) {
            systemProperty JvmOptions.FILE_ENCODING_KEY, 'UTF-8'
        }
    }

    private void configureJar(Project project) {
        project.tasks.create('generatePomPropertiesFile') {
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
        project.configure(project) {
            // delayed to be able to use version
            afterEvaluate {
                // do not override user attributes
                Attributes attributes = jar.manifest.attributes
                putIfAbsent(attributes, 'Implementation-Title', project.description ?: project.name)
                putIfAbsent(attributes, 'Implementation-Version', project.version)
                putIfAbsent(attributes, 'Built-By', System.getProperty('user.name'))
                putIfAbsent(attributes, 'Built-Date', new Date())
                putIfAbsent(attributes, 'Built-JDK', System.getProperty('java.version'))
                putIfAbsent(attributes, 'Built-Gradle', project.gradle.gradleVersion)
                putIfAbsent(attributes, 'Target-JDK', project.targetCompatibility)
            }

            model {
                tasks.jar {
                    into("META-INF/maven/$project.group/$project.name") {
                        from generatePomFileForMavenPublication
                        rename '.*.xml', 'pom.xml'
                        from generatePomPropertiesFile
                    }
                }
            }
        }
    }

    private void addSourcesJarTask(Project project) {
        project.configure(project) {
            tasks.create('sourcesJar', Jar)
            sourcesJar {
                dependsOn classes
                group BUILD_GROUP
                from sourceSets.main.allSource
                classifier = 'sources'
            }
            if (stablePublishing) {
                publishing.publications.maven.artifact sourcesJar
            }
        }
    }

    private void addJavadocJarTask(Project project) {
        // apply only if java sources exist
        boolean hasJavaSources = project.sourceSets.main.java.srcDirs.find { it.exists() }
        if (hasJavaSources) {
            project.configure(project) {
                tasks.create(JAVADOC_JAR, Jar)
                javadocJar {
                    dependsOn javadoc
                    group BUILD_GROUP
                    classifier 'javadoc'
                    from javadoc.destinationDir
                }
                if (stablePublishing) {
                    publishing.publications.maven.artifact javadocJar
                }
            }
        }
    }

    private void addGroovydocJarTask(Project project) {
        // apply only if groovy enabled
        project.plugins.withType(GroovyPlugin) {
            // apply only if groovy sources exist
            boolean hasGroovySources = project.sourceSets.main.groovy.srcDirs.find { it.exists() }
            if (hasGroovySources) {
                project.configure(project) {
                    tasks.create(GROOVYDOC_JAR, Jar)
                    groovydocJar {
                        dependsOn groovydoc
                        group BUILD_GROUP
                        // very important to have at least one javadoc package, because otherwise maven cantral
                        // would not accept package
                        classifier project.tasks.findByName(JAVADOC_JAR) ? 'groovydoc' : 'javadoc'
                        from groovydoc.destinationDir
                    }
                    if (stablePublishing) {
                        publishing.publications.maven.artifact groovydocJar
                    }
                }
            }
        }
    }

    private boolean isStablePublishingEnabled() {
        if (GradleVersion.current() >= GradleVersion.version('5.0')) {
            // since 5.0 stable publishing should be enabled by default
            return true
        }
        // option available from gradle 4.8
        try {
            FeaturePreviews.Feature stablePublishingFeature = FeaturePreviews.Feature
                    .withName('STABLE_PUBLISHING')
            return stablePublishingFeature.isActive() && previews.isFeatureEnabled(stablePublishingFeature)
        } catch (IllegalArgumentException ignored) {
            // do nothing if option doesn't exists anymore (.withName() failed)
            return false
        }
    }

    private void configureLazyMavenPublication(Project project) {
        project.configure(project) {
            // lazy initialized publication (could be directly overridden)
            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                        artifact sourcesJar
                        if (project.tasks.findByName(JAVADOC_JAR)) {
                            artifact javadocJar
                        }
                        if (project.tasks.findByName(GROOVYDOC_JAR)) {
                            artifact groovydocJar
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings('NestedBlockDepth')
    private void configureStableMavenPublication(Project project) {
        project.configure(project) {
            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                        // in stable publication mode extra jars are added directly after tasks registration
                    }
                }
            }
        }
    }

    private void addInstallTask(Project project) {
        project.tasks.create('install') {
            dependsOn 'publishToMavenLocal'
            group 'publishing'
            description 'Alias for publishToMavenLocal task'
            doLast {
                logger.warn "INSTALLED $project.group:$project.name:$project.version"
            }
        }
    }

    private void putIfAbsent(Attributes attributes, String name, Object value) {
        if (!attributes.containsKey(name)) {
            attributes.put(name, value)
        }
    }
}
