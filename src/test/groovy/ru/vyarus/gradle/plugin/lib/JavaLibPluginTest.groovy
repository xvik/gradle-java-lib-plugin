package ru.vyarus.gradle.plugin.lib

import org.gradle.api.Project
import org.gradle.testing.jacoco.tasks.JacocoReport
import ru.vyarus.gradle.plugin.pom.PomPlugin

/**
 * @author Vyacheslav Rusakov 
 * @since 07.11.2015
 */
class JavaLibPluginTest extends AbstractTest {

    def "Check plugin registration"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        Project project = project {
            apply plugin: 'java'
            apply plugin: "ru.vyarus.java-lib"
        }

        then: "java and pom plugins activated"
        project.plugins.findPlugin(PomPlugin)

        then: "mavenJava publication registered"
        project.publishing.publications.maven

        then: "javadoc and sources tasks created"
        project.tasks.javadocJar
        project.tasks.sourcesJar

        then: "install task created"
        project.tasks.install
    }

    def "Check plugin registration for groovy"() {

        when: "activating plugin"
        file('src/main/groovy').mkdirs()
        Project project = project {
            apply plugin: 'groovy'
            apply plugin: "ru.vyarus.java-lib"
        }

        then: "java and pom plugins activated"
        project.plugins.findPlugin(PomPlugin)

        then: "mavenJava publication registered"
        project.publishing.publications.maven

        then: "javadoc and sources tasks created"
        project.tasks.javadocJar
        project.tasks.sourcesJar

        then: "install task created"
        project.tasks.install
    }

    def "Check plugin registration for java-library"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        Project project = project {
            apply plugin: 'java-library'
            apply plugin: "ru.vyarus.java-lib"
        }

        then: "java and pom plugins activated"
        project.plugins.findPlugin(PomPlugin)

        then: "mavenJava publication registered"
        project.publishing.publications.maven

        then: "javadoc and sources tasks created"
        project.tasks.javadocJar
        project.tasks.sourcesJar

        then: "install task created"
        project.tasks.install
    }

    def "Check plugin registration for java-platform"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        Project project = project {
            apply plugin: 'java-platform'
            apply plugin: "ru.vyarus.java-lib"
        }

        then: "pom plugin activated"
        project.plugins.findPlugin(PomPlugin)

        then: "bom publication registered"
        project.publishing.publications.bom

        then: "install task created"
        project.tasks.install
    }

    def "Check jacoco xml report active"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        Project project = project {
            apply plugin: 'java'
            apply plugin: 'jacoco'
            apply plugin: "ru.vyarus.java-lib"
        }

        then: "xml report active"
        (project.tasks.findByName('jacocoTestReport') as JacocoReport).reports.xml.required.get()
    }
}