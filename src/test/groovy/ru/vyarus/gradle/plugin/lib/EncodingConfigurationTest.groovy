package ru.vyarus.gradle.plugin.lib

import org.gradle.api.Project

/**
 * @author Vyacheslav Rusakov
 * @since 09.11.2019
 */
class EncodingConfigurationTest extends AbstractTest {

    def "Check encoding set for java tasks"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        Project project = project {
            apply plugin: 'java'
            apply plugin: "ru.vyarus.java-lib"
        }

        then: "compile tasks affected"
        project.tasks.compileJava.options.encoding == 'UTF-8'
        project.tasks.compileTestJava.options.encoding == 'UTF-8'

        then: "javadoc affected"
        project.tasks.javadoc.options.encoding == 'UTF-8'
        project.tasks.javadoc.options.charSet == 'UTF-8'
        project.tasks.javadoc.options.docEncoding == 'UTF-8'

        then: "test sys property set"
        project.tasks.test.allJvmArgs.contains('-Dfile.encoding=UTF-8')
    }

    def "Check encoding set for groovy tasks"() {

        when: "activating plugin"
        file('src/main/java').mkdirs()
        file('src/main/groovy').mkdirs()
        Project project = project {
            apply plugin: 'groovy'
            apply plugin: "ru.vyarus.java-lib"
        }

        then: "compile tasks affected"
        project.tasks.compileJava.options.encoding == 'UTF-8'
        project.tasks.compileGroovy.options.encoding == 'UTF-8'
        project.tasks.compileTestJava.options.encoding == 'UTF-8'
        project.tasks.compileTestGroovy.options.encoding == 'UTF-8'

        then: "javadoc affected"
        project.tasks.javadoc.options.encoding == 'UTF-8'
        project.tasks.javadoc.options.charSet == 'UTF-8'
        project.tasks.javadoc.options.docEncoding == 'UTF-8'

        then: "test sys property set"
        project.tasks.test.allJvmArgs.contains('-Dfile.encoding=UTF-8')
    }
}
