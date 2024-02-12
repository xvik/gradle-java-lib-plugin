package ru.vyarus.gradle.plugin.lib

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.testing.TestReport
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * @author Vyacheslav Rusakov
 * @since 12.06.2021
 */
class ReportsAggregationTest extends AbstractTest {

    def "Check aggregated reports"() {

        when: "apply plugin"
        file('sub1/src/main/java').mkdirs()
        file('sub1/src/test/java').mkdirs()
        file('sub2/src/main/java').mkdirs()
        file('sub2/src/test/java').mkdirs()

        file('sub1/src/main/java/Sample.java').createNewFile()
        file('sub1/src/test/java/SampleTest.java').createNewFile()
        file('sub2/src/main/java/Sample.java').createNewFile()
        file('sub2/src/test/java/SampleTest.java').createNewFile()

        Project project = projectBuilder {
            apply plugin: 'base'
            apply plugin: 'jacoco'
            apply plugin: 'project-report'
            apply plugin: 'ru.vyarus.java-lib'

            javaLib {
                aggregateReports()
            }
        }
                .child('sub1') {
                    apply plugin: 'java'
                    apply plugin: 'jacoco'
                    apply plugin: 'project-report'
                    apply plugin: 'ru.vyarus.java-lib'
                }
                .child('sub2') {
                    apply plugin: 'java'
                    apply plugin: 'jacoco'
                    apply plugin: 'project-report'
                    apply plugin: 'ru.vyarus.java-lib'
                }
                .build()

        then: "aggregation tasks created"
        JacocoReport cov = project.tasks.jacocoTestReport
        // execution data counts only existing files
        cov.executionData.size() == 0

        then: "tests aggregation task"
        TestReport test = project.tasks.test
        test
        // testResults contains not only test reports but also coverage
        test.testResults.files.findAll {it.name == 'binary'}.size() == 2

        then: "dependencies reports aggregated"
        project.htmlDependencyReport.projects.size() == 3
    }

    def "Check not same submodules case"() {

        when: "apply plugin"
        file('sub2/src/main/java').mkdirs()
        file('sub2/src/test/java').mkdirs()

        file('sub2/src/main/java/Sample.java').createNewFile()
        file('sub2/src/test/java/SampleTest.java').createNewFile()

        Project project = projectBuilder {
            apply plugin: 'base'
            apply plugin: 'jacoco'
            apply plugin: 'project-report'
            apply plugin: 'ru.vyarus.java-lib'

            javaLib {
                aggregateReports()
            }
        }
                .child('sub1')
                .child('sub2') {
                    apply plugin: 'java'
                    apply plugin: 'jacoco'
                    apply plugin: 'project-report'
                    apply plugin: 'ru.vyarus.java-lib'
                }
                .build()

        then: "aggregation tasks created"
        JacocoReport cov = project.tasks.jacocoTestReport
        // execution data counts only existing files
        cov.executionData.size() == 0

        then: "tests aggregation task"
        TestReport test = project.tasks.test
        test
        test.testResults.files.findAll {it.name == 'binary'}.size() == 1

        then: "dependencies reports aggregated"
        project.htmlDependencyReport.projects.size() == 3
    }

    def "Check aggregation on simple module"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        project {
            apply plugin: 'base'
            apply plugin: 'jacoco'
            apply plugin: "ru.vyarus.java-lib"

            javaLib {
                aggregateReports()
            }
        }

        then: "xml report active"
        def ex = thrown(GradleException)
        ex.cause.message == 'javaLib.aggregateReports() could not be used on project \'test\' because does not contain subprojects'
    }

    def "Check aggregation on java module"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        projectBuilder {
            apply plugin: 'java'
            apply plugin: 'jacoco'
            apply plugin: "ru.vyarus.java-lib"

            javaLib {
                aggregateReports()
            }
        }.child('sub') {
            apply plugin: 'java'
        }
                .build()

        then: "xml report active"
        def ex = thrown(GradleException)
        ex.cause.message == 'javaLib.aggregateReports() could not be used on project \'test\' because it contains java sources. If this is a root project use \'base\' plugin instead.'
    }
}
