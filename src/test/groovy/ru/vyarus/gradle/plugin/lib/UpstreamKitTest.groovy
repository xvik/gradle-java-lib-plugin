package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Requires

import java.util.zip.ZipFile

/**
 * @author Vyacheslav Rusakov
 * @since 13.11.2019
 */
@Requires({jvm.java17Compatible})
class UpstreamKitTest extends AbstractKitTest {

    String GRADLE_VERSION = '9.0.0'

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
        def result = runVer(GRADLE_VERSION, 'install', '--warning-mode', 'all')


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
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar"] as Set<String>
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
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar"] as Set<String>
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
                ["${baseName}.jar", "${baseName}.pom", "${baseName}-sources.jar", "${baseName}-javadoc.jar"] as Set<String>
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
        file('settings.gradle') << """
rootProject.name = "test"
"""

        when: "run pom task"
        def result = runVer(GRADLE_VERSION, 'install')


        String artifactId = 'test'
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
        props.contains("artifactId: $artifactId")
        props.contains('version: 1.0')

        cleanup:
        jar?.close()
    }

    def "Check jar only publish"() {
        setup:
        file('src/main/java').mkdirs()
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.java-lib'
            }
            
            javaLib {
                withoutJavadoc()
                withoutSources()
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
                ["${baseName}.jar", "${baseName}.pom"] as Set<String>
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
        def result = runVer(GRADLE_VERSION, 'test', '--warning-mode', 'all')

        then: "task done"
        result.task(":test").outcome == TaskOutcome.SUCCESS

        then: "test report created"
        def test = file('build/reports/tests/test/index.html')
        test.exists()

        when: "run coverage task"
        result = runVer(GRADLE_VERSION, 'clean', 'jacocoTestReport', '--warning-mode', 'all')

        then: "task done"
        result.task(":jacocoTestReport").outcome == TaskOutcome.SUCCESS

        then: "coverage aggregated"
        def cov = file('build/reports/jacoco/test/jacocoTestReport.xml')
        cov.exists()
        cov.length() > 0

        when: "run dependencies task"
        result = runVer(GRADLE_VERSION, 'htmlDependencyReport', '--warning-mode', 'all')

        then: "task done"
        result.task(":htmlDependencyReport").outcome == TaskOutcome.SUCCESS

        then: "aggregated dependency report"
        file('build/reports/project/dependencies/root.sub1.html').exists()
        file('build/reports/project/dependencies/root.sub2.html').exists()
        file('build/reports/project/dependencies/root.html').exists()
    }
}
