# Gradle Java-lib plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-java-lib-plugin.svg)](https://travis-ci.org/xvik/gradle-java-lib-plugin)

### About

Plugin applies common configuration for java or groovy library: 

* Configure `jar` with default manifest and put pom.xml and pom.properties inside jar (like maven do)
* Add `sourcesJar` task
* Add `javadocJar` or (and) `groovydocJar` tasks
* Configures maven publication named `maven` with all jars (jar, sources javadock or (and) groovydoc)
* Add `install` task as shortcut for publishToMavenLocal
* Applies [pom plugin](https://github.com/xvik/gradle-pom-plugin) which:   
  - Fix [dependencies scopes](https://github.com/xvik/gradle-pom-plugin#java-and-groovy-plugins) 
  in generated pom
  - Add `pom` configuration closure to [simplify pom definition](https://github.com/xvik/gradle-pom-plugin#pom-configuration).
  - Add `withPomXml` configuration closure to use if you [need manual xml configuration](https://github.com/xvik/gradle-pom-plugin#manual-pom-modification) 
  - Adds `optional` and `provided` configurations (affect only resulted pom)

If you need [multiple publications](https://docs.gradle.org/current/userguide/publishing_maven.html#N17EB8) from the same project, 
then you will have to perform additional configuration or, maybe (depends on case), use only [pom plugin](https://github.com/xvik/gradle-pom-plugin). 

**Confusion point**: plugin named almost the same as gradle's own [java-library plugin](https://docs.gradle.org/current/userguide/java_library_plugin.html),
but plugins do different things (gradle plugin only provides api and impl configurations) and could be used together.


### Setup

Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-java-lib-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-java-lib-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.java-lib).

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-java-lib-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-java-lib-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-java-lib-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-java-lib-plugin)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-java-lib-plugin:1.0.5'
    }
}
apply plugin: 'ru.vyarus.java-lib'
```

OR

```groovy
plugins {
    id 'ru.vyarus.java-lib' version '1.0.5'
}
```

Plugin must be applied after java or groovy or java-library plugins. Otherwise it will do nothing.

### Usage

#### General

You need to specify general info:

```groovy
group = 'your.group'                    // maven group
version = '1.0.0'                       // project version
description = 'My project description'  // optional (affects jar manifest) 
```

You may use optional and provided dependencies ([if java-library plugin not used](https://github.com/xvik/gradle-pom-plugin#provided-and-optional-configurations)):

```groovy
dependencies {    
    provided 'com.foo:dep-provided:1.0'
    optional 'com.foo:dep-optional:1.0'        
}
```

#### Tasks 

- `sourcesJar` task is always applied 
- `javadocJar` if java sources directory present ('src/main/java')
- `groovydocJar` if groovy plugin available and groovy sources present ('src/main/groovy'). Last condition is important because groovy may be used only for tests.

IMPORTANT: if you have only groovy sources then `groovydocJar` will have javadoc` classifier! This is because maven central requires
javadoc jar, so even if you write groovy project you have to name it javadoc.

In case of both groovy and java sources, `groovydocJar` will use `groovydoc` classifier, because `javadocJar` already use `javadoc` and have to produce separate artifacts. 

#### Pom

Most likely, you will need to configure pom:

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

Read more about pom specifics in [pom plugin description](https://github.com/xvik/gradle-pom-plugin).

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
            'Built-By': 'Me',
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

#### Publication

[maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin is used for publication.

By default plugin configures `maven` publication with javadoc or (and) groovydoc and sources jars. 

Use 'install' task to deploy jars into local maven repository.

```bash
$ gradlew install
``` 

If you dont want to publish everything (jar, sources, javadoc) then you can override list of publishing artifacts:

```groovy
publishing.publications.maven.artifacts = [jar, javadocJar]
```

Here sources are excluded from publishing.

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

### Complete usage example

```groovy
plugins {
    id 'java'
    id 'ru.vyarus.java-lib' version '1.0.4'
    id 'com.jfrog.bintray' version '1.7.1'
}

group = 'com.sample' 
version = '1.0.0'
description = 'Sample project'

sourceCompatibility = 1.6

repositories { jcenter() }
dependencies {    
    provided 'com.foo:dep-provided:1.0'
    compile 'com.foo:dep-compile:1.0'        
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
* [java-library generator](https://github.com/xvik/generator-lib-java) - java library project generator

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
