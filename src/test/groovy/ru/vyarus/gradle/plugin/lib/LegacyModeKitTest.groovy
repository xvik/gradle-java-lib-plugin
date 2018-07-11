package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 11.07.2018
 */
class LegacyModeKitTest extends AbstractKitTest {

    def "Check install task"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.java-lib'
            }

            group 'ru.vyarus'
            version 1.0
        """

        when: "run pom task"
        def result = runVer('4.6','install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        deploy.list() as Set ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar"] as Set<String>
    }

    def "Check install task for groovy"() {
        setup:
        file('src/main/groovy').mkdirs()
        build """
            plugins {
                id 'groovy'
                id 'ru.vyarus.java-lib'
            }

            group 'ru.vyarus'
            version 1.0
        """

        when: "run pom task"
        def result = runVer('4.6', 'install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        // javadoc will be produced instead of groovydoc! important for maven central
        deploy.list() as Set ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar"] as Set<String>
    }

    def "Check install for both sources"() {
        setup:
        file('src/main/java').mkdirs()
        file('src/main/groovy').mkdirs()
        build """
            plugins {
                id 'groovy'
                id 'ru.vyarus.java-lib'
            }

            group 'ru.vyarus'
            version 1.0
        """

        when: "run pom task"
        def result = runVer('4.6','install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        deploy.list() as Set ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar", "${baseName}-groovydoc.jar"] as Set<String>
    }

    def "Check install for no sources"() {
        setup:
        build """
            plugins {
                id 'groovy'
                id 'ru.vyarus.java-lib'
            }

            group 'ru.vyarus'
            version 1.0
        """

        when: "run pom task"
        def result = runVer('4.6', 'install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        deploy.list() as Set ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar"] as Set<String>
    }

    def "Check behaviour on test sources"() {
        setup:
        file('src/test/java').mkdirs()
        file('src/test/groovy').mkdirs()
        build """
            plugins {
                id 'groovy'
                id 'ru.vyarus.java-lib'
            }

            group 'ru.vyarus'
            version 1.0
        """

        when: "run pom task"
        def result = runVer('4.6', 'install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        deploy.list() as Set ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar"] as Set<String>
    }

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

        when: "run pom task"
        def result = runVer('4.6','install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed, but without sources"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        deploy.list() as Set ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-javadoc.jar"] as Set<String>
    }
}
