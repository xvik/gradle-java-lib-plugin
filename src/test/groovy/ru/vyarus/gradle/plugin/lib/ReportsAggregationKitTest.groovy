package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 12.06.2021
 */
class ReportsAggregationKitTest extends AbstractKitTest {

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
        def result = run('test')

        then: "task done"
        result.task(":test").outcome == TaskOutcome.SUCCESS

        then: "test report created"
        def test = file('build/reports/tests/test/index.html')
        test.exists()

        when: "run coverage task"
        result = run('clean', 'jacocoTestReport')

        then: "task done"
        result.task(":jacocoTestReport").outcome == TaskOutcome.SUCCESS
        result.task(":jacocoMerge").outcome == TaskOutcome.SUCCESS

        then: "coverage aggregated"
        file('build/reports/jacoco/test/jacocoTestReport.xml').exists()
        file('build/jacoco/test.exec').exists()

        when: "run dependencies task"
        result = run('htmlDependencyReport')

        then: "task done"
        result.task(":htmlDependencyReport").outcome == TaskOutcome.SUCCESS

        then: "aggregated dependency report"
        file('build/reports/project/dependencies/root.sub1.html').exists()
        file('build/reports/project/dependencies/root.sub2.html').exists()
        file('build/reports/project/dependencies/root.html').exists()
    }
}
