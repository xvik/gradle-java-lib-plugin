package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 09.06.2021
 */
class PomConfigurationShortcutTest extends AbstractKitTest {

    def "Check install task"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.java-lib'
            }
            
            repositories {mavenCentral()}
            dependencies {
                implementation platform('com.google.inject:guice-bom:4.0')
                implementation 'com.google.inject:guice'
            }
            
            javaLib.pom {
                removeDependencyManagement()
            }

            group 'ru.vyarus'
            version 1.0
        """

        when: "run pom task"
        def result = run('install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        when: "find pom"
        def pomFile = new File(deploy, "$artifactId-1.0.pom")
        // for debug
        println pomFile.getText()
        def pom = new XmlParser().parse(pomFile)

        then: "no dependency management section"
        !pom.dependencyManagement
        pom.dependencies.'*'.find { it.artifactId.text() == 'guice' }.version.text() == '4.0'
    }
}
