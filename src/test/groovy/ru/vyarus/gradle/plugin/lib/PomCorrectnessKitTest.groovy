package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 17.07.2018
 */
class PomCorrectnessKitTest extends AbstractKitTest {

    def "Check install task"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.java-lib'
            }
            
            dependencies { 
                provided 'com.google.code.findbugs:annotations:3.0.0'               
                compile 'org.javassist:javassist:3.16.1-GA'             
            }
            
            pom {
                name 'sample'
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

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar"] as Set<String>

        when: "get pom"
        def pomFile = new File(deploy, "${baseName}.pom")
        def pom = new XmlParser().parse(pomFile)
        // for debug
        println pomFile.getText()

        then: "pom is correct"
        pom.dependencies.'*'.find { it.artifactId.text() == 'javassist' }.scope.text() == 'compile'
        pom.dependencies.'*'.find { it.artifactId.text() == 'annotations' }.scope.text() == 'provided'
        pom.name.text() == 'sample'
    }
}
