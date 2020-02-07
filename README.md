# Gradle Java-lib plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-java-lib-plugin.svg)](https://travis-ci.org/xvik/gradle-java-lib-plugin)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-java-lib-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-java-lib-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-java-lib-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-java-lib-plugin)


### About

Plugin do all boilerplate of maven publication configuration (using [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html)) 
for java or groovy library or gradle plugin. Simplifies POM configuration and dependencies management. 
Configured publication is ready for maven central (follows all central rules).

Features:

* Maven-like `jar` configuration
    - put `pom.xml` and `pom.properties` inside jar
    - fill manifest properties 
* Configure tasks for additional artifacts (required for maven central publish): 
    - `sourcesJar`  
    - `javadocJar` or (and) `groovydocJar`
* Prepare maven publication (`maven-publish`)
    - `maven` publication configured with all jars (jar, sources javadock or (and) groovydoc)
* Applies [pom plugin](https://github.com/xvik/gradle-pom-plugin) which:   
    - Fix [dependencies scopes](https://github.com/xvik/gradle-pom-plugin/#dependencies) 
    in generated pom
    - Add `pom` configuration closure to [simplify pom definition](https://github.com/xvik/gradle-pom-plugin#pom-configuration).
    - Add `withPomXml` configuration closure to use if you [need manual xml configuration](https://github.com/xvik/gradle-pom-plugin#manual-pom-modification) 
* Add `install` task as shortcut for `publishToMavenLocal`
* Apply `UTF-8` encoding for:
    - compile tasks: `JavaCompile` and `GroovyCompile`
    - javadoc (encoding, charSet, docencoding) 
    - test tasls: set `file.encoding=UTF-8` system property (only for test tasks)   

If you need [multiple publications](https://docs.gradle.org/current/userguide/publishing_maven.html#N17EB8) from the same project, 
then you will have to perform additional configuration or, maybe (depends on case), use only [pom plugin](https://github.com/xvik/gradle-pom-plugin). 

**Confusion point**: plugin named almost the same as gradle's own [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) plugin,
but plugins do *different things* (gradle plugin only provides `api` and `implementation` configurations) and plugins *could* be used together.

##### Summary

* Configuration closures: `pom`, `withPomXml`
* Tasks: `sourcesJar`, `javadocJar` (`groovydocJar`), `install`      
* [Publication](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:publications): `maven`
* Enable plugins: [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html),
[ru.vyarus.pom](https://github.com/xvik/gradle-pom-plugin)


### Setup

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-java-lib-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-java-lib-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-java-lib-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-java-lib-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/java-lib/ru.vyarus.java-lib.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.java-lib)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-java-lib-plugin:2.1.0'
    }
}
apply plugin: 'ru.vyarus.java-lib'
```

OR

```groovy
plugins {
    id 'ru.vyarus.java-lib' version '2.1.0'
}
```

#### Compatibility

Plugin compiled for java 8, compatible with java 11

Gradle | Version
--------|-------
5.1     | 2.1.0
4.6     | [1.1.2](https://github.com/xvik/gradle-java-lib-plugin/tree/1.1.2)
older   | [1.0.5](https://github.com/xvik/gradle-java-lib-plugin/tree/1.0.5)

#### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>

* Go to [JitPack project page](https://jitpack.io/#ru.vyarus/gradle-java-lib-plugin)
* Select `Commits` section and click `Get it` on commit you want to use 
    or use `master-SNAPSHOT` to use the most recent snapshot

For gradle before 6.0 use `buildscript` block with required commit hash as version:

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'ru.vyarus:gradle-java-lib-plugin:b5a8aee24f'
    }
}
apply plugin: 'ru.vyarus.java-lib'
```

For gradle 6.0 and above:

* Add to `settings.gradle` (top most!) with required commit hash as version:

  ```groovy
  pluginManagement {
      resolutionStrategy {
          eachPlugin {
              if (requested.id.namespace == 'ru.vyarus.java-lib') {
                  useModule('ru.vyarus:gradle-java-lib-plugin:b5a8aee24f')
              }
          }
      }
      repositories {
          maven { url 'https://jitpack.io' }
          gradlePluginPortal()          
      }
  }    
  ``` 
* Use plugin without declaring version: 

  ```groovy
  plugins {
      id 'ru.vyarus.java-lib'
  }
  ```  

</details>  

### Usage

Plugin requires [java](https://docs.gradle.org/current/userguide/java_plugin.html) or 
[groovy](https://docs.gradle.org/current/userguide/groovy_plugin.html) or 
[java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) plugins to be enabled.

#### General

You need to specify general info:

```groovy
group = 'your.group'                    // maven group
version = '1.0.0'                       // project version
description = 'My project description'  // optional (affects jar manifest) 
```

Note: maven `artifactId` will be the same as project name and the default for project name
is current directory name. If you need to change name, add in `settings.gradle`:

```
rootProject.name = 'the-name-you-want'
```        

#### Pom

For maven-central publication you need to fill pom:

```groovy
pom {
    name 'Project Name'
    description 'My awesome project'
    licenses {
        license {
            name "The MIT License"
            url "http://www.opensource.org/licenses/MIT"
            distribution 'repo'
        }
    }
    scm {
        url 'https://github.com/me/my-repo.git'
        connection 'scm:git@github.com:me/my-repo.git'
        developerConnection 'scm:git@github.com:me/my-repo.git'
    }
    developers {
        developer {
            id "dev1"
            name "Dev1 Name"
            email "dev1@email.com"
        }
    }
}
``` 

If your project is hosted on github you may use [github-info](https://github.com/xvik/gradle-github-info-plugin) plugin, 
which fills most pom sections for you automatically.

Read more about pom specifics in [pom plugin description](https://github.com/xvik/gradle-pom-plugin).

Use the following configuration to get correct scopes in the resulted pom:

Maven scope | Gradle configuration
------------| ----------------
compile     | implementation, api
runtime     | runtimeOnly
provided    | provided  (**not** compileOnly!)
optional    | optional, [feature variants](https://github.com/xvik/gradle-pom-plugin#feature-variants)

See [pom plugin doc](https://github.com/xvik/gradle-pom-plugin#dependencies) for more details about dependencies scopes in the generated pom

For BOMs [prefer spring plugin](https://github.com/xvik/gradle-pom-plugin#usage-with-spring-dependency-management-plugin) over gradle native support.

#### Publication

[maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin is used for publication.

By default, plugin configures `maven` publication with javadoc or (and) groovydoc and sources jars. 

Use `install` task to deploy jars into local maven repository.

```bash
$ gradlew install
``` 

If you don't want to publish everything (jar, sources, javadoc) then you can override list of publishing artifacts:

```groovy
publishing.publications.maven.artifacts = [jar, javadocJar]
```

Here sources are excluded from publishing (note that if you going to publish to maven central sources are required).

##### Publish to repository 
 
For actual publication only [repository](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:repositories) must be configured:

```groovy
publishing {
    repositories {
        maven {
            // change to point to your repo, e.g. http://my.org/repo
            url "$buildDir/repo"
        }
    }
}
```

Then [publish task](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:publishing) may be used to perform publish. 

##### Publish to bintray
 
If you publish to [bintray](https://bintray.com/), then you need to reference `maven` publication 
in [bintray plugin](https://github.com/bintray/gradle-bintray-plugin) configuration:
 
```groovy
bintray {    
    publications = ['maven']
    ...
}    
```

##### Gradle plugin

Gradle plugin project will have [java-gradle-plugin](https://docs.gradle.org/current/userguide/java_gradle_plugin.html), 
which declares its own maven publication `pluginMaven` (with main jar as artifact). Also, plugin creates one more 
publication per declared plugin to publish [plugin marker artifact](https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_markers)
(required by gradle plugins dsl).

Java-lib plugin will still create separate publication `maven` and you should use it for publishing with bintray 
(same way as for usual library)

There might be several situations...

###### Gradle plugin repository

For publishing in gradle plugin repository you will use [com.gradle.plugin-publish](https://plugins.gradle.org/docs/publish-plugin) 
plugin. This plugin normally adds its own source and javadoc tasks.

Java-lib plugin will prevent additional sources and javadoc tasks creation. `plugin-publish`
use artifacts from `Project.artifacts` and java-lib will register all required artifacts there.
So overall `maven` publication and artifacts applied to plugins portal will be the same.

Use `maven` publication into maven central or jcenter (or other repo). Ignore `pluginMaven` (containing just one jar).

###### Publishing only to custom repo

This is in-house plugin case, when plugins are published only into corparate repository.

In this case, you most likely will not use bintray and so can't configure exact publication name.
The simplest solution is to disable `pluginMaven` publication tasks (but marker artifact publications should remain!)

```groovy
tasks.withType(AbstractPublishToMaven) { Task task ->
    if (task.name.startsWith("publishPluginMaven")) {
        task.enabled(false)
    }
}
```    

This will disable: `publishPluginMavenPublicationToMavenLocal` and `publishPluginMavenPublicationToMavenRepository`

And you can simply use `publish` task to trigger all required publications without duplicates.

The same way, `install` will install all required artifacts locally (includin markers) and so it is possible
to use plugins from local maven repository too (with plugin syntax).

#### Encodings

UTF-8 applied to:

* (all `CompileJava` tasks).options.encoding 
* (all `CompileGrovy` tasks).options.encoding
* (all `Javadoc`).options.\[encoding, charSet, docEncoding]
* (all `Test`).systemProperty 'file.encoding'

Note that groovydoc task does not have encoding configuration, but it should use UTF-8 by defautl. 

For tests, encoding is important (especially on windows) because test forked process will not inherit root gradle encoding configuration. 

#### Tasks 

- `sourcesJar` task is always applied 
- `javadocJar` if java sources directory present ('src/main/java')
- `groovydocJar` if groovy plugin available and groovy sources present ('src/main/groovy'). Last condition is important because groovy may be used only for tests.

IMPORTANT: if you have only groovy sources then `groovydocJar` will have javadoc` classifier! This is because maven central requires
javadoc jar, so even if you write groovy project you have to name it javadoc.

In case of both groovy and java sources, `groovydocJar` will use `groovydoc` classifier, because `javadocJar` already use `javadoc` and have to produce separate artifacts.   

`install` task added to simplify publication to local maven repository: this is simply shortcut for
gradle's [publishToMavenLocal](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:tasks) task
(simply shorter to type and more common name after maven).  

#### Main Jar

Plugin applies default manifest properties:

```groovy
'Implementation-Title': project.description ?: project.name,
'Implementation-Version': project.version,
'Built-By': System.getProperty('user.name'),
'Built-Date': new Date(),
'Built-JDK': System.getProperty('java.version'),
'Built-Gradle': gradle.gradleVersion,
'Target-JDK': project.targetCompatibility
```

You can override it:

```groovy
jar {
    manifest {
        attributes 'Implementation-Title': 'My Custom value',
            'Built-By': 'Me'
    }
}
```

For all not specified properties default values will be used.

Plugin will include additional files inside jar (like maven do) into `META-INF/maven/group/artifact/`

* pom.xml
* pom.properties

pom.properties contains:

* version
* groupId
* artifactId

### Complete usage example

```groovy
plugins {
    id 'java'  // or groovy or java-library
    id 'ru.vyarus.java-lib' version '2.1.0'
    id 'com.jfrog.bintray' version '1.7.1'
}

group = 'com.sample' 
version = '1.0.0'
description = 'Sample project'

sourceCompatibility = 1.8

repositories { jcenter() }
dependencies {    
    compileOnly 'com.foo:dep-provided:1.0'
    implementation 'com.foo:dep-compile:1.0'        
}

pom {
    name 'Project Name'
    description 'My awesome project'
    licenses {
        license {
            name "The MIT License"
            url "http://www.opensource.org/licenses/MIT"
            distribution 'repo'
        }
    }
    scm {
        url 'https://github.com/me/my-repo.git'
        connection 'scm:git@github.com:me/my-repo.git'
        developerConnection 'scm:git@github.com:me/my-repo.git'
    }
    developers {
        developer {
            id "dev1"
            name "Dev1 Name"
            email "dev1@email.com"
        }
    }
}

bintray {    
    publications = ['maven']
    ...
}  
```   

Example projects:

* [Java library](https://github.com/xvik/dropwizard-guicey), published to maven central
* [Multi-module java library](https://github.com/xvik/dropwizard-guicey-ext) (with BOM), published to maven central
* This project (plugin) is also using java-lib so it is an example of usage in gradle plugin 

### Boilerplate plugin removes

Just for reference, here is what plugin did for java project:

```groovy
apply plugin: 'ru.vyarus.pom'

jar {    
    manifest {
        attributes 'Implementation-Title': project.description ?: project.name,
                'Implementation-Version': project.version,
                'Built-By': System.getProperty('user.name'),
                'Built-Date': new Date(),
                'Built-JDK': System.getProperty('java.version'),
                'Built-Gradle': gradle.gradleVersion,
                'Target-JDK': project.targetCompatibility
    }
}

task generatePomPropertiesFile {
    inputs.properties ([
            'version': "${ -> project.version }",
            'groupId': "${ -> project.group }",
            'artifactId': "${ -> project.name }"
    ])
    outputs.file "$project.buildDir/generatePomPropertiesFile/pom.properties"
    doLast {
        File file = outputs.files.singleFile
        file.parentFile.mkdirs()
        file << inputs.properties.collect{ key, value -> "$key: $value" }.join('\n')
    }
}

model {
    tasks.jar {
        into("META-INF/maven/$project.group/$project.name") {
            from generatePomFileForMavenPublication
            rename ".*.xml", "pom.xml"
            from generatePomPropertiesFile
        }
    }
}

task sourcesJar(type: Jar, dependsOn: classes, group: 'build') {
	from sourceSets.main.allSource
	classifier = 'sources'
}

task javadocJar(type: Jar, dependsOn: javadoc, group: 'build') {
	classifier 'javadoc'
    from javadoc.destinationDir
}

task groovydocJar(type: Jar, dependsOn: groovydoc, group: 'build') {
	classifier 'groovydoc'
    from groovydoc.destinationDir 
    options.encoding = StandardCharsets.UTF_8
    options.charSet = StandardCharsets.UTF_8
    options.docEncoding = StandardCharsets.UTF_8
}    

tasks.withType(JavaCompile).configureEach {
    it.options.encoding = StandardCharsets.UTF_8
}

tasks.withType(GroovyCompile).configureEach {
    it.options.encoding = StandardCharsets.UTF_8
}

tasks.withType(Test).configureEach {
   it.systemProperty JvmOptions.FILE_ENCODING_KEY, StandardCharsets.UTF_8
}

publishing {
	publications {
	    maven(MavenPublication) {
	        from components.java
	        artifact sourcesJar	        
	        artifact javadocJar
	        artifact groovydocJar	 
	    }
	}
}

task install(dependsOn: publishToMavenLocal, group: 'publishing') << {
	logger.warn "INSTALLED $project.group:$project.name:$project.version"
}
```

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [github-info-plugin](https://github.com/xvik/gradle-github-info-plugin) - pre-configure common plugins with github related info
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin) - project documentation generator 
* [java-library generator](https://github.com/xvik/generator-lib-java) - java library project generator

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
