package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 06.06.2021
 */
class JavaPlatformKitTest extends AbstractKitTest {

    def "Check install task for BOM"() {
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
        """

        when: "run install task"
        def result = run('install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
                ["${baseName}.pom"] as Set<String>
    }

    def "Check custom BOM artifact name"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java-platform'
                id 'ru.vyarus.java-lib'
            }
            
            javaLib.bom {
                artifactId = 'bom'
                description = 'Sample BOM'
            }

            group 'ru.vyarus'
            version 1.0
            
            dependencies {
                constraints {
                    api 'ru.vyarus:guice-validator:2.0.0'
                }
            }
        """

        when: "run install task"
        def result = run('install')


        String artifactId = 'bom'
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
                ["${baseName}.pom"] as Set<String>

        when: "get pom"
        def pomFile = new File(deploy, "${baseName}.pom")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "pom is correct"
        pom.dependencyManagement.dependencies.'*'.find { it.artifactId.text() == 'guice-validator' }
        pom.name.text() == 'bom'
        pom.description.text() == 'Sample BOM'
    }
}