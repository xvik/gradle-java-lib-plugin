package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

import java.util.zip.ZipFile

/**
 * @author Vyacheslav Rusakov
 * @since 13.11.2019
 */
class UpstreamKitTest extends AbstractKitTest {

    String GRADLE_VERSION = '6.0'

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
        def result = runVer(GRADLE_VERSION, 'install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
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
        def result = runVer(GRADLE_VERSION, 'install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        // javadoc will be produced instead of groovydoc! important for maven central
        withoutModuleFile(deploy) ==
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
        def result = runVer(GRADLE_VERSION, 'install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
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
        def result = runVer(GRADLE_VERSION, 'install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
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
        def result = runVer(GRADLE_VERSION, 'install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy)  ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar"] as Set<String>
    }

    def "Check jar modification"() {

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
        def result = runVer(GRADLE_VERSION, 'install')


        String artifactId = projectName()
        String baseName = artifactId + '-1.0'
        ZipFile jar = new ZipFile(file("build/repo/ru/vyarus/$artifactId/1.0/${baseName}.jar"))

        then: "jar manifest correct"
        String manifest = jar.getInputStream(jar.getEntry('META-INF/MANIFEST.MF')).text
        println manifest
        manifest.contains("Implementation-Title: $artifactId")
        manifest.contains("Implementation-Version: 1.0")
        manifest.contains("Built-By: ${System.getProperty('user.name')}")
        manifest.contains("Built-Date:")
        manifest.contains("Built-JDK:")
        manifest.contains("Built-Gradle:")
        manifest.contains("Target-JDK:")

        then: "jar contains pom"
        String jarPom = jar.getInputStream(jar.getEntry("META-INF/maven/ru.vyarus/$artifactId/pom.xml")).text
        jarPom != null
        println jarPom

        then: "jar contains pom.properties"
        String props = jar.getInputStream(jar.getEntry("META-INF/maven/ru.vyarus/$artifactId/pom.properties")).text
        props != null
        println props
        props.contains('groupId: ru.vyarus')
        props.contains("artifactId: ${projectName()}")
        props.contains('version: 1.0')
    }
}
