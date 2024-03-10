package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

import java.util.zip.ZipFile

/**
 * @author Vyacheslav Rusakov
 * @since 02.03.2024
 */
class ConfigurationCacheSupportKitTest extends AbstractKitTest {

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
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'install')


        String artifactId = projectName()
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar"] as Set<String>


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'install')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")
    }



    def "Check reports aggregation"() {
        setup:
        build """
            plugins {
                id 'base'
                id 'jacoco'
                id 'project-report'
                id 'ru.vyarus.java-lib'
            }
                
            javaLib {
                aggregateReports()
            }
            
            allprojects {
                repositories { mavenCentral() }
            }
            
            subprojects {
                apply plugin: 'groovy'
                apply plugin: 'jacoco'
                apply plugin: 'project-report'
                apply plugin: 'ru.vyarus.java-lib'
                
                dependencies {
                    testImplementation 'org.spockframework:spock-core:2.3-groovy-3.0'
                }
                
                test {
                    useJUnitPlatform()
                }
            }
        """
        file('settings.gradle') << "include 'sub1', 'sub2'"

        fileFromClasspath('sub1/src/main/java/sample/Sample.java', '/sample/Sample.java')
        fileFromClasspath('sub1/src/test/groovy/sample/SampleTest.groovy', '/sample/SampleTest.groovy')

        fileFromClasspath('sub2/src/main/java/sample/Sample2.java', '/sample/Sample2.java')
        fileFromClasspath('sub2/src/test/groovy/sample/SampleTest2.groovy', '/sample/SampleTest2.groovy')

        when: "run test task"
        def result = run('--configuration-cache', '--configuration-cache-problems=warn', 'test')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task done"
        result.task(":test").outcome == TaskOutcome.SUCCESS

        then: "test report created"
        def test = file('build/reports/tests/test/index.html')
        test.exists()

        when: "run coverage task"
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'clean', 'jacocoTestReport')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task done"
        result.task(":jacocoTestReport").outcome == TaskOutcome.SUCCESS

        then: "coverage aggregated"
        def cov = file('build/reports/jacoco/test/jacocoTestReport.xml')
        cov.exists()
        cov.length() > 0

        when: "run dependencies task"
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'htmlDependencyReport')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task done"
        result.task(":htmlDependencyReport").outcome == TaskOutcome.SUCCESS

        then: "aggregated dependency report"
        file('build/reports/project/dependencies/root.sub1.html').exists()
        file('build/reports/project/dependencies/root.sub2.html').exists()
        file('build/reports/project/dependencies/root.html').exists()



        when: "run test task from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'test')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')

        then: "task done"
        result.task(":test").outcome == TaskOutcome.UP_TO_DATE

        then: "test report created"
        test.exists()

        when: "run coverage task"
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'clean', 'jacocoTestReport')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')

        then: "task done"
        result.task(":jacocoTestReport").outcome == TaskOutcome.SUCCESS

        then: "coverage aggregated"
        cov.exists()
        cov.length() > 0

        when: "run dependencies task"
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'htmlDependencyReport')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')

        then: "task done"
        result.task(":htmlDependencyReport").outcome == TaskOutcome.SUCCESS

        then: "aggregated dependency report"
        file('build/reports/project/dependencies/root.sub1.html').exists()
        file('build/reports/project/dependencies/root.sub2.html').exists()
        file('build/reports/project/dependencies/root.html').exists()
    }


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
                withoutGradleMetadata()
                withoutJavadoc()
                withoutSources()
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
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'install')


        String artifactId = 'test'
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "task done"
        result.task(":install").outcome == TaskOutcome.SUCCESS
        result.output.contains("INSTALLED ru.vyarus:$artifactId:1.0")

        then: "artifacts deployed"
        deploy.exists()
        def baseName = artifactId + '-1.0'
        withoutModuleFile(deploy) ==
                ["${baseName}.jar", "${baseName}.pom", "${baseName}.jar.asc", "${baseName}.pom.asc"] as Set<String>


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'install')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
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
        def result = run('--configuration-cache', '--configuration-cache-problems=warn', 'install')


        String artifactId = 'test'
        File deploy = file("build/repo/ru/vyarus/$artifactId/1.0/")

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

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


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'install')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')

        cleanup:
        jar?.close()
    }
}
