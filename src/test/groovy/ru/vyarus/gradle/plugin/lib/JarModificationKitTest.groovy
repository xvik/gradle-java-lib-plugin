package ru.vyarus.gradle.plugin.lib

import java.util.zip.ZipFile

/**
 * @author Vyacheslav Rusakov 
 * @since 23.11.2015
 */
class JarModificationKitTest extends AbstractKitTest {

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
        def result = run('install')


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

    def "Check jar manifest override"() {

        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.java-lib'
            }

            group 'ru.vyarus'
            version 1.0

            jar {
                manifest {
                    attributes 'Implementation-Title': 'Custom',
                        'Target-JDK': '1.0'
                }
            }
        """

        when: "run pom task"
        def result = run('install')
        String artifactId = projectName()
        String baseName = artifactId + '-1.0'
        ZipFile jar = new ZipFile(file("build/repo/ru/vyarus/$artifactId/1.0/${baseName}.jar"))
        String manifest = jar.getInputStream(jar.getEntry('META-INF/MANIFEST.MF')).text
        println manifest

        then: "user attributes preserved"
        manifest.contains("Implementation-Title: Custom")
        manifest.contains("Target-JDK: 1.0")

        then: "default attributes added"
        manifest.contains("Implementation-Version: 1.0")
        manifest.contains("Built-By: ${System.getProperty('user.name')}")
        manifest.contains("Built-Date:")
        manifest.contains("Built-JDK:")
        manifest.contains("Built-Gradle:")
    }
}
