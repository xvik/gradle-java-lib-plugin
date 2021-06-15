package ru.vyarus.gradle.plugin.lib

import org.gradle.api.Project

/**
 * @author Vyacheslav Rusakov
 * @since 10.06.2021
 */
class OpenDepsReportTasksTest extends AbstractTest {

    def "Check show dependencies task appear"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        Project project = project {
            apply plugin: 'project-report'
            apply plugin: "ru.vyarus.java-lib"
        }

        then: "task applied"
        project.tasks.findByName('openDependencyReport')
    }

    def "Check show dependencies task appear in multi-module configuration"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        Project project = project {
            apply plugin: "ru.vyarus.java-lib"

            allprojects {
                apply plugin: 'project-report'
            }
        }

        then: "task applied"
        project.tasks.findByName('openDependencyReport')
    }

}
