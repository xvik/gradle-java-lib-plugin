package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 07.06.2021
 */
class SigningKitTest extends AbstractKitTest {

    def "Check automatic signing"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'signing'
                id 'ru.vyarus.java-lib'
            }
            
            javaLib {
                disableGradleMetadata()
                disableJavadocPublish()
                disableSourcesPublish()
            }

            group 'ru.vyarus'
            version 1.0
            
            ext['signing.keyId']='78065050'
            ext['signing.password']=
            ext['signing.secretKeyRingFile']='test.gpg'
        """
        fileFromClasspath('test.gpg', '/cert/test.gpg')
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

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}.jar.asc", "${baseName}.pom.asc"] as Set<String>
    }

    def "Check automatic BOM signing"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java-platform'
                id 'signing'
                id 'ru.vyarus.java-lib'
            }
            
            javaLib {
                disableGradleMetadata()
            }

            group 'ru.vyarus'
            version 1.0
            
            ext['signing.keyId']='78065050'
            ext['signing.password']=
            ext['signing.secretKeyRingFile']='test.gpg'
        """
        fileFromClasspath('test.gpg', '/cert/test.gpg')
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

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
                ["${baseName}.pom", "${baseName}.pom.asc"] as Set<String>
    }

    def "Check all artifacts signing"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'signing'
                id 'ru.vyarus.java-lib'
            }           

            group 'ru.vyarus'
            version 1.0
            
            ext['signing.keyId']='78065050'
            ext['signing.password']=
            ext['signing.secretKeyRingFile']='test.gpg'
        """
        fileFromClasspath('test.gpg', '/cert/test.gpg')
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

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
                ["${baseName}.jar", "${baseName}.jar.asc",
                 "${baseName}.pom", "${baseName}.pom.asc",
                 "${baseName}-sources.jar", "${baseName}-sources.jar.asc",
                 "${baseName}-javadoc.jar", "${baseName}-javadoc.jar.asc",
                 "${baseName}.module.asc"] as Set<String>
    }

    def "Check snapshots not signed"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'signing'
                id 'ru.vyarus.java-lib'
            }
            
            javaLib {
                disableGradleMetadata()
                disableJavadocPublish()
                disableSourcesPublish()
            }

            group 'ru.vyarus'
            version '1.0-SNAPSHOT'
        """
        file('settings.gradle') << """
rootProject.name = "test"
"""

        when: "run pom task"
        def result = run('install')


        String artifactId = 'test'
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0-SNAPSHOT/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0-SNAPSHOT")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0-SNAPSHOT'
        withoutModuleFile(deploy) ==
                ["${baseName}.jar", "${baseName}.pom", 'maven-metadata-local.xml'] as Set<String>
    }
}
