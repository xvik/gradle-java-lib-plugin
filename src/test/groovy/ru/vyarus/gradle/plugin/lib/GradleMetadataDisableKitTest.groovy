package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 06.06.2021
 */
class GradleMetadataDisableKitTest extends AbstractKitTest {

    def "Check metadata disable"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.java-lib'
            }

            group 'ru.vyarus'
            version 1.0
            
            dependencies {
                implementation 'ru.vyarus:guice-validator:2.0.0'
            }
            
            javaLib {
                disableGradleMetadata()
            }
        """

        when: "run install task"
        def result = run('install')

        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        deploy.list().size() == 4
        withoutModuleFile(deploy) ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar"] as Set<String>
    }

    def "Check metadata disable for bom"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java-platform'
                id 'ru.vyarus.java-lib'
            }

            group 'ru.vyarus'
            version 1.0
            
            dependencies {
                constraints {
                    api 'ru.vyarus:guice-validator:2.0.0'
                }
            }
            
            javaLib {
                disableGradleMetadata()
            }
        """

        when: "run install task"
        def result = run('install')

        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        deploy.list().size() == 1
        withoutModuleFile(deploy) == ["${baseName}.pom"] as Set<String>
    }
}
