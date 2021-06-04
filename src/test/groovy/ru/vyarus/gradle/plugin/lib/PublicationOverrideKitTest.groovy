package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 05.12.2015
 */
class PublicationOverrideKitTest extends AbstractKitTest {

    def "Check publication override"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.java-lib'
            }

            group 'ru.vyarus'
            version 1.0

            publishing.publications.maven.artifacts = [jar, javadocJar]
        """
        file('settings.gradle') << """
rootProject.name = "test"
"""

        when: "run pom task"
        def result = run('install')


        String artifactId = 'test'
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed, but without sources"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-javadoc.jar"] as Set<String>
    }
}