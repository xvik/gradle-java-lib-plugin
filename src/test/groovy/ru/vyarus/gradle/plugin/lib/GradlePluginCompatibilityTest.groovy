package ru.vyarus.gradle.plugin.lib

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.pom.PomPlugin

/**
 * java-gradle-plugin crete separate pluginMaven publication + publication-link for each plugin (alias for plugin).
 * plugin-publish creates source and javadoc tasks but avoid this if project.archives already contains them
 *
 * @author Vyacheslav Rusakov
 * @since 13.11.2019
 */
class GradlePluginCompatibilityTest extends AbstractTest {

    def "Check default plugins behaviour"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        Project project = project {
            apply plugin: 'com.gradle.plugin-publish'
            apply plugin: 'java-gradle-plugin'
            apply plugin: 'groovy'
            apply plugin: 'maven-publish'
        }

        then: "mavenJava publication registered"
        project.publishing.publications.names == ["pluginMaven"] as Set

        then: "artifacts initialized"
        project.publishing.publications.pluginMaven.artifacts.collect {it.file.name} as Set == ['test.jar', 'test-sources.jar', 'test-javadoc.jar'] as Set
    }

    def "Check plugin registration"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        Project project = project {
            apply plugin: 'com.gradle.plugin-publish'
            apply plugin: 'java-gradle-plugin'
            apply plugin: 'ru.vyarus.java-lib'
            apply plugin: 'groovy'
        }

        then: "java and pom plugins activated"
        project.plugins.findPlugin(PomPlugin)

        then: "mavenJava publication used"
        project.publishing.publications.names == ['maven', 'pluginMaven'] as Set

        then: "javadoc and sources tasks created"
        project.tasks.javadocJar
        project.tasks.sourcesJar

        then: "install task created"
        project.tasks.install

        then: "artifacts initialized"
        project.publishing.publications.maven.artifacts.collect {it.file.name} as Set == ['test.jar', 'test-sources.jar', 'test-javadoc.jar'] as Set
        project.publishing.publications.pluginMaven.artifacts.collect {it.file.name} as Set == ['test.jar', 'test-sources.jar', 'test-javadoc.jar'] as Set
    }
}
