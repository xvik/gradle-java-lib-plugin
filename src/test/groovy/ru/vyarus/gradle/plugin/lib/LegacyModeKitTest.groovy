package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 11.07.2018
 */
@IgnoreIf({jvm.java9Compatible}) // gradle 4.6 will not work on java 11
class LegacyModeKitTest extends AbstractKitTest {

    String GRADLE_VERSION = '5.1'
    String PLATFORM_GRADLE_VERSION = '5.2' // platform plugin was introduced in 5.2

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
        def result = runVer(GRADLE_VERSION,'install')


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
        def result = runVer(GRADLE_VERSION,'install')


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
        def result = runVer(GRADLE_VERSION, 'install')


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
        def result = runVer(GRADLE_VERSION, 'install')


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
        def result = runVer(GRADLE_VERSION,'install')


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
                    testImplementation 'org.spockframework:spock-core:2.0-groovy-2.5'
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
        def result = runVer(PLATFORM_GRADLE_VERSION, 'test')

        then: "task done"
        result.task(":test").outcome == TaskOutcome.SUCCESS

        then: "test report created"
        def test = file('build/reports/tests/test/index.html')
        test.exists()

        when: "run coverage task"
        result = runVer(PLATFORM_GRADLE_VERSION, 'clean', 'jacocoTestReport')

        then: "task done"
        result.task(":jacocoTestReport").outcome == TaskOutcome.SUCCESS

        then: "coverage aggregated"
        def cov = file('build/reports/jacoco/test/jacocoTestReport.xml')
        cov.exists()
        cov.length() > 0

        when: "run dependencies task"
        result = runVer(PLATFORM_GRADLE_VERSION, 'htmlDependencyReport')

        then: "task done"
        result.task(":htmlDependencyReport").outcome == TaskOutcome.SUCCESS

        then: "aggregated dependency report"
        file('build/reports/project/dependencies/root.sub1.html').exists()
        file('build/reports/project/dependencies/root.sub2.html').exists()
        file('build/reports/project/dependencies/root.html').exists()
    }
}
