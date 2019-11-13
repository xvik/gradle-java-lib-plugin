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

        then: "maven-publish created it's own tasks"
        project.tasks.publishPluginJar // sources
        project.tasks.publishPluginJavaDocsJar
        project.tasks.publishPluginGroovyDocsJar

        then: "artifacts initialized"
        project.configurations.archives.allArtifacts.collect {it.file.name} == ['test.jar', 'test-sources.jar', 'test-javadoc.jar', 'test-groovydoc.jar']
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
        project.publishing.publications.names == ['pluginMaven'] as Set

        then: "javadoc and sources tasks created"
        project.tasks.javadocJar
        project.tasks.sourcesJar

        then: "maven-publish does not created it's own tasks"
        !project.tasks.findByName('publishPluginJar') // sources
        !project.tasks.findByName('publishPluginJavaDocsJar')
        !project.tasks.findByName('publishPluginGroovyDocsJar')

        then: "install task created"
        project.tasks.install

        then: "artifacts initialized"
        project.configurations.archives.allArtifacts.collect {it.file.name} == ['test.jar', 'test-sources.jar', 'test-javadoc.jar']
    }
}
