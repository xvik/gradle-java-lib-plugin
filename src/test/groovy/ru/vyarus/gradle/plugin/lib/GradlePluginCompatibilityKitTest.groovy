package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

import java.util.zip.ZipFile

/**
 * @author Vyacheslav Rusakov
 * @since 13.11.2019
 */
class GradlePluginCompatibilityKitTest extends AbstractKitTest {

    def "Check legacy publish plugin integration correctness"() {
        setup:
        fileFromClasspath('src/main/java/ru/vyarus/TestPlugin.java', '/sample/TestPlugin.java')

        build """
            plugins {
                id 'com.gradle.plugin-publish' version '0.21.0'
                id 'java-gradle-plugin'
                id 'ru.vyarus.java-lib'
                id 'java'  
                id 'signing'               
            }
            
            gradlePlugin {
                plugins {
                    testPlugin {
                        id = 'ru.vyarus.test'                        
                        implementationClass = 'ru.vyarus.TestPlugin'
                    }
                }
            }
            pluginBundle {
                description = 'Test plugin'
                tags = ['java']
                
                mavenCoordinates {
                    groupId = project.group
                    artifactId = project.name
                }
            }
            
            maven.pom {
                name = 'customName'
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

        then: "alias publication created"
        result.task(":publishMavenPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS
        result.task(":publishTestPluginPluginMarkerMavenPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS

        then: "custom jar tasks used"
        result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
        result.task(":javadocJar").outcome == TaskOutcome.SUCCESS

        then: "plugin-publish did not register custom tasks"
        result.task(":publishPluginJar") == null
        result.task(":publishPluginJavaDocsJar") == null
        result.task(":publishPluginGroovyDocsJar") == null

        then: "plugin descriptors generation executed"
        result.task(":pluginDescriptors").outcome == TaskOutcome.SUCCESS

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy)  ==
                ["${baseName}.jar", "${baseName}.jar.asc",
                 "${baseName}.pom", "${baseName}.pom.asc",
                 "${baseName}-sources.jar", "${baseName}-sources.jar.asc",
                 "${baseName}-javadoc.jar", "${baseName}-javadoc.jar.asc",
                 "${baseName}.module.asc"] as Set<String>

        then: "jar modifiers applied"
        ZipFile jar = new ZipFile(file("build/repo/ru/vyarus/$artifactId/1.0/${baseName}.jar"))
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
        println jarPom
        jarPom != null
        jarPom.contains("<groupId>ru.vyarus</groupId>")
        jarPom.contains("<artifactId>$artifactId</artifactId>")

        then: "pom closure applied"
        jarPom.contains("<name>customName</name>")

        then: "jar contains pom.properties"
        String props = jar.getInputStream(jar.getEntry("META-INF/maven/ru.vyarus/$artifactId/pom.properties")).text
        println props
        props != null
        props.contains('groupId: ru.vyarus')
        props.contains("artifactId: $artifactId")
        props.contains('version: 1.0')

        then: "jar contains plugin desriptor"
        String gradleDesc = jar.getInputStream(jar.getEntry("META-INF/gradle-plugins/ru.vyarus.test.properties")).text
        println gradleDesc
        gradleDesc.trim()  == 'implementation-class=ru.vyarus.TestPlugin'

        cleanup:
        jar?.close()
    }


    def "Check current publish plugin integration correctness"() {
        setup:
        fileFromClasspath('src/main/java/ru/vyarus/TestPlugin.java', '/sample/TestPlugin.java')

        build """
            plugins {
                id 'com.gradle.plugin-publish' version '1.2.1'
                id 'java-gradle-plugin'
                id 'ru.vyarus.java-lib'
                id 'java'                
            }
            
            gradlePlugin {
                plugins {
                    testPlugin {
                        description = 'Test plugin'
                        tags.set(['java'])
                        id = 'ru.vyarus.test'                        
                        implementationClass = 'ru.vyarus.TestPlugin'
                    }
                }
            }
            
            maven.pom {
                name = 'customName'
            }

            group 'ru.vyarus'
            version 1.0
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

        then: "alias publication created"
        result.task(":publishMavenPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS
        result.task(":publishTestPluginPluginMarkerMavenPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS

        then: "custom jar tasks used"
        result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
        result.task(":javadocJar").outcome == TaskOutcome.SUCCESS

        then: "plugin descriptors generation executed"
        result.task(":pluginDescriptors").outcome == TaskOutcome.SUCCESS

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy)  ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar"] as Set<String>

        then: "jar modifiers applied"
        ZipFile jar = new ZipFile(file("build/repo/ru/vyarus/$artifactId/1.0/${baseName}.jar"))
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
        println jarPom
        jarPom != null
        jarPom.contains("<groupId>ru.vyarus</groupId>")
        jarPom.contains("<artifactId>$artifactId</artifactId>")

        then: "pom closure applied"
        jarPom.contains("<name>customName</name>")

        then: "jar contains pom.properties"
        String props = jar.getInputStream(jar.getEntry("META-INF/maven/ru.vyarus/$artifactId/pom.properties")).text
        println props
        props != null
        props.contains('groupId: ru.vyarus')
        props.contains("artifactId: $artifactId")
        props.contains('version: 1.0')

        then: "jar contains plugin desriptor"
        String gradleDesc = jar.getInputStream(jar.getEntry("META-INF/gradle-plugins/ru.vyarus.test.properties")).text
        println gradleDesc
        gradleDesc.trim()  == 'implementation-class=ru.vyarus.TestPlugin'

        cleanup:
        jar?.close()
    }


    def "Check current publish plugin with signing integration correctness"() {
        setup:
        fileFromClasspath('src/main/java/ru/vyarus/TestPlugin.java', '/sample/TestPlugin.java')

        build """
            plugins {
                id 'com.gradle.plugin-publish' version '1.2.1'
                id 'java-gradle-plugin'
                id 'ru.vyarus.java-lib'
                id 'java'
                id 'signing'                
            }
            
            gradlePlugin {
                plugins {
                    testPlugin {
                        description = 'Test plugin'
                        tags.set(['java'])
                        id = 'ru.vyarus.test'                        
                        implementationClass = 'ru.vyarus.TestPlugin'
                    }
                }
            }
            
            maven.pom {
                name = 'customName'
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

        then: "alias publication created"
        result.task(":publishMavenPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS
        result.task(":publishTestPluginPluginMarkerMavenPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS

        then: "custom jar tasks used"
        result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
        result.task(":javadocJar").outcome == TaskOutcome.SUCCESS

        then: "plugin descriptors generation executed"
        result.task(":pluginDescriptors").outcome == TaskOutcome.SUCCESS

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy)  ==
                ["${baseName}.jar", "${baseName}.jar.asc",
                 "${baseName}.pom", "${baseName}.pom.asc",
                 "${baseName}-sources.jar", "${baseName}-sources.jar.asc",
                 "${baseName}-javadoc.jar", "${baseName}-javadoc.jar.asc",
                 "${baseName}.module.asc"] as Set<String>

        then: "jar modifiers applied"
        ZipFile jar = new ZipFile(file("build/repo/ru/vyarus/$artifactId/1.0/${baseName}.jar"))
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
        println jarPom
        jarPom != null
        jarPom.contains("<groupId>ru.vyarus</groupId>")
        jarPom.contains("<artifactId>$artifactId</artifactId>")

        then: "pom closure applied"
        jarPom.contains("<name>customName</name>")

        then: "jar contains pom.properties"
        String props = jar.getInputStream(jar.getEntry("META-INF/maven/ru.vyarus/$artifactId/pom.properties")).text
        println props
        props != null
        props.contains('groupId: ru.vyarus')
        props.contains("artifactId: $artifactId")
        props.contains('version: 1.0')

        then: "jar contains plugin desriptor"
        String gradleDesc = jar.getInputStream(jar.getEntry("META-INF/gradle-plugins/ru.vyarus.test.properties")).text
        println gradleDesc
        gradleDesc.trim()  == 'implementation-class=ru.vyarus.TestPlugin'

        cleanup:
        jar?.close()
    }


    def "Check publishing only maven publication (with signing)"() {
        setup:
        fileFromClasspath('src/main/java/ru/vyarus/TestPlugin.java', '/sample/TestPlugin.java')

        build """
            plugins {
                id 'com.gradle.plugin-publish' version '1.2.1'
                id 'java-gradle-plugin'
                id 'ru.vyarus.java-lib'
                id 'java'
                id 'signing'                
            }
            
            gradlePlugin {
                plugins {
                    testPlugin {
                        description = 'Test plugin'
                        tags.set(['java'])
                        id = 'ru.vyarus.test'                        
                        implementationClass = 'ru.vyarus.TestPlugin'
                    }
                }
            }
            
            maven.pom {
                name = 'customName'
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
        def result = run('publishMavenPublicationToMavenLocal')


        String artifactId = 'test'
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "task done"
        result.task(":publishMavenPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS

        then: "custom jar tasks used"
        result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
        result.task(":javadocJar").outcome == TaskOutcome.SUCCESS

        then: "plugin descriptors generation executed"
        result.task(":pluginDescriptors").outcome == TaskOutcome.SUCCESS

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy)  ==
                ["${baseName}.jar", "${baseName}.jar.asc",
                 "${baseName}.pom", "${baseName}.pom.asc",
                 "${baseName}-sources.jar", "${baseName}-sources.jar.asc",
                 "${baseName}-javadoc.jar", "${baseName}-javadoc.jar.asc",
                 "${baseName}.module.asc"] as Set<String>

        then: "jar modifiers applied"
        ZipFile jar = new ZipFile(file("build/repo/ru/vyarus/$artifactId/1.0/${baseName}.jar"))
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
        println jarPom
        jarPom != null
        jarPom.contains("<groupId>ru.vyarus</groupId>")
        jarPom.contains("<artifactId>$artifactId</artifactId>")

        then: "pom closure applied"
        jarPom.contains("<name>customName</name>")

        then: "jar contains pom.properties"
        String props = jar.getInputStream(jar.getEntry("META-INF/maven/ru.vyarus/$artifactId/pom.properties")).text
        println props
        props != null
        props.contains('groupId: ru.vyarus')
        props.contains("artifactId: $artifactId")
        props.contains('version: 1.0')

        then: "jar contains plugin desriptor"
        String gradleDesc = jar.getInputStream(jar.getEntry("META-INF/gradle-plugins/ru.vyarus.test.properties")).text
        println gradleDesc
        gradleDesc.trim()  == 'implementation-class=ru.vyarus.TestPlugin'

        cleanup:
        jar?.close()
    }
}
