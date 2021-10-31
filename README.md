# Gradle Java-lib plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![CI](https://github.com/xvik/gradle-java-lib-plugin/actions/workflows/CI.yml/badge.svg)](https://github.com/xvik/gradle-java-lib-plugin/actions/workflows/CI.yml)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-java-lib-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-java-lib-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-java-lib-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-java-lib-plugin)


### About

Plugin do all boilerplate of maven publication configuration (using [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html)) 
for java (or groovy) library or gradle plugin. Simplifies POM configuration and dependencies management (BOM).
Also, changes some defaults common for java projects (like UTF-8 usage).

Makes gradle more "maven" (in sense of simplicity, some behaviours and for [multi-module projects](#maven-like-multi-module-project)).

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
* Add `install` task as shortcut for `publishToMavenLocal` (simpler to use)
* Apply `UTF-8` encoding for:
    - compile tasks: `JavaCompile` and `GroovyCompile`
    - javadoc (encoding, charSet, docEncoding) 
    - test tasks: set `file.encoding=UTF-8` system property (only for test tasks)   
* Prepares BOM publication for [java-platform](https://docs.gradle.org/current/userguide/java_platform_plugin.html) plugin
* Test and coverage reports aggregation for multi-module projects
* Simplifies gradle platforms usage in multi-module projects (avoid "platform" leaking in published poms)

If you need [multiple publications](https://docs.gradle.org/current/userguide/publishing_maven.html#N17EB8) from the same project, 
then you will have to perform additional configuration or, maybe (depends on case), use only [pom plugin](https://github.com/xvik/gradle-pom-plugin). 

**Confusion point**: plugin named almost the same as gradle's own [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) plugin,
but plugins do *different things* (gradle plugin only provides `api` and `implementation` configurations) and plugins *could* be used together.

##### Summary

* Configuration closures: `pom`, `withPomXml`, `javaLib`
* Tasks: `sourcesJar`, `javadocJar` (`groovydocJar`), `install`, `openDependencyReport`      
* [Publication](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:publications): `maven`, `bom`
* Enable plugins: [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html),
[ru.vyarus.pom](https://github.com/xvik/gradle-pom-plugin)
  
### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-java-lib-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-java-lib-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/java-lib/ru.vyarus.java-lib.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.java-lib)

```groovy
buildscript {
    repositories {
      gradlePluginPortal()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-java-lib-plugin:2.3.0'
    }
}
apply plugin: 'ru.vyarus.java-lib'
```

OR

```groovy
plugins {
    id 'ru.vyarus.java-lib' version '2.3.0'
}
```

#### Compatibility

Plugin compiled for java 8, compatible with java 11

Gradle | Version
--------|-------
5.1     | 2.3.0
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

Plugin activate features based on registered plugins. Plugin support several usage scenarios.

In case of multi-module projects, plugin activate features only in applied module, ignoring submodules or root module
(so to apply it in all submodules use `allprojects` or `subprojects` section)

Example projects:

* [Java library](https://github.com/xvik/dropwizard-guicey), published to maven central
* [Multi-module java library](https://github.com/xvik/dropwizard-guicey-ext) (with BOM), published to maven central
* [Simple multi-module library](https://github.com/xvik/yaml-updater) (without BOM), published to maven central
* [This project](https://github.com/xvik/gradle-java-lib-plugin/blob/master/build.gradle) is an example of gradle plugin publication to maven central and plugins portal


#### Java module

```groovy
plugins {
  id 'java' // groovy or java-library
  // id 'signing'
  // id 'project-report'
  id 'ru.vyarus.java-lib'
}

group = 'your.group'                    
version = '1.0.0'                       
description = 'My project description'

// configure target pom
pom {
  name 'Project Name'
  description 'My awesome project'
  ...
}

repositories { mavenLocal(); mavenCentral() }
dependencies {
  ...
}

javaLib {
  // withoutJavadoc()
  // withoutSources()
  withoutGradleMetadata()
  
  // autoModuleName = 'project-module-name'
  
  pom {
    // removeDependencyManagement()
    // forceVersions()
    // disableScopesCorrection()
    // disableBomsReorder()
  }
}

```

Activates with [java](https://docs.gradle.org/current/userguide/java_plugin.html),
[groovy](https://docs.gradle.org/current/userguide/groovy_plugin.html) or 
[java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) plugin.  
Typical usage: single-module gradle project which must be published to maven central (or any other maven repo) 

* Adds `javadocJar` task (maybe `groovydocJar`) task for javadocs publishing
* Adds `sourcesJar` task for sources publishing  
* Registers `maven` publication for pom, jar, javadoc and sources artifacts
* Applies UTF-8 encoding for java/groovy compile, javadoc/groovydoc and test executions
* If [signing](https://docs.gradle.org/current/userguide/signing_plugin.html) plugin active, [configures publication signing](#signing) (required for maven central publication)
* Adds `install` task for installation into local repository (like maven; simply shortcut for `publishToMavenLocal` task)
* Enables xml jacoco reports (if jacoco enabled; required for coverage services)
* Register [ru.vyarus.pom](https://github.com/xvik/gradle-pom-plugin) plugin which:
    - adds `optional` and `provided` configurations (in maven sense)
    - fixes dependency scopes in the generated pom
    - moves up dependencyManagement section in the generated pom (if platforms used)
    - adds `pom` closure for pom declaration (`withPomXml` might be used for low-level modification)
* Utilities:
  - simple Auto-Module-Name declaration (java 9 modules)
  - option to disable gradle metadata publication (maven central fails on it sometimes)
  - option to remove dependencyManagement section in the generated pom (appears if platforms used): use resolved dependencies versions instead (pom plugin feature)
* Adds [openDependencyReport](#dependency-report) task added if [project-report](https://docs.gradle.org/current/userguide/project_report_plugin.html) plugin enabled  
  (opens `htmlDependencyReport` directly in browser)

#### BOM module

```groovy
plugins {
  id 'java-platform' 
  // id 'signing'
  // id 'project-report'
  id 'ru.vyarus.java-lib'
}

group = 'your.group'                    
version = '1.0.0'                       
description = 'My project description'

pom {
  ...
}

repositories { mavenLocal(); mavenCentral() }
dependencies {
  api platform('ru.vyarus.guicey:guicey-bom:5.2.0-1')
  constraints {
    api 'org.webjars:webjars-locator:0.40'
  }
  // add subprojects to published BOM
  project.subprojects.each { api it }
}

javaLib {
  bom {
    // change artifact from project name, if required
    artifactId = 'something-bom'
    description = 'Different from project description'
  }
  withoutGradleMetadata()
}
```  

Activates with [java-platform](https://docs.gradle.org/current/userguide/java_platform_plugin.html) plugin.  
Typical usage: BOM module (might be root project) in multi-module project

* Activates platform dependencies (javaPlatform.allowDependencies()) to allow single dependencies declaration
* Registers `bom` publication for bom artifact (produced from declared platform)
* Register [ru.vyarus.pom](https://github.com/xvik/gradle-pom-plugin) plugin (with the same features as above)
* If [signing](https://docs.gradle.org/current/userguide/signing_plugin.html) plugin active, [configures publication signing](#signing) (required for maven central publication)
* Adds `install` task for installation into local repository (like maven; simply shortcut for `publishToMavenLocal` task)
* Utilities:
  - option to disable gradle metadata publication (maven central fails on it sometimes)
  - option to change bom artifact id (useful when platform declared in the root module)
* Adds [openDependencyReport](#dependency-report) task added if [project-report](https://docs.gradle.org/current/userguide/project_report_plugin.html) plugin enabled  
  (opens `htmlDependencyReport` directly in browser) 
 
#### Root project reports aggregation

```groovy
plugins {
  id 'base' 
  id 'jacoco'
  //id 'project-report'
  id 'ru.vyarus.java-lib'
}

javaLib {
  aggregateReports()
}

// sub modules - simple java projects
subprojects {
  apply plugin: 'java'
  
  ...
}
```

Activates with [base](https://docs.gradle.org/current/userguide/base_plugin.html) plugin.  
Used to aggregate test and coverage reports from java submodules.

By default, will only register `openDependencyReport` task added if [project-report](https://docs.gradle.org/current/userguide/project_report_plugin.html) plugin enabled.  
Reports aggregation must be explicitly triggered:
  - Adds `test` task which would simply aggregate (run if required) java submodules test reports
  - If jacoco plugin active, add `jacocoMerge` (not for direct usage) and
`jacocoTestReport` tasks to aggregate jacoco xml and html reports from java submodules
  - If project-report plugin active, will aggregate dependency reports for submodules

In short: it adds absolutely the same tasks as in java modules and generates
reports exactly into the same locations so there would be no difference in paths
when configuring external services (e.g. coveralls).

NOTE: aggregation will work with `java-platform` plugin too if it used in the root module (see [complete multi-module example](#maven-like-multi-module-project)).

### Options

```groovy
javaLib {

  /**
   * Do not publish javadoc (groovydoc) with `maven` publication. 
   */
  withoutJavadoc()

  /**
   * Do not publish sources with `maven` publication. 
   */
  withoutSources()

  /**
   * Do not publish gradle metadata artifact. 
   * Affects all publications (not just registered by plugin).
   */
  withoutGradleMetadata()

  /**
   * Disable all publications. Might be used to disable configured BOM publication or any sub-module publication.
   */
  withoutPublication()

  /**
   * Shortcut for Auto-Module-Name meta-inf header declaration
   */
  autoModuleName = 'project-module-name'

  /**
   * Used ONLY with java-platform plugin if published artifact must differ from
   * project name (for example, when declared in the root project).
   */
  bom {
    // when not declared, project.name used
    artifactId = 'name'
    // when not declared, project.description used
    description = 'desc'
  }

  /**
   * Shortcut for ru.vyarus.pom plugin's pomGeneration configuration
   * (visually groups configurations because pom plugin registered implicitly).
   * See pom plugin docs for options description.
   */
  pom {
    removeDependencyManagement()
    forceVersions()
    disableScopesCorrection()
    disableBomsReorder()
  }

  /**
   * Used in the root project (project with child projects) to aggregate
   * test, coverage (jacoco) and dependency (project-report) reports. 
   * Requires at least `base` plugin. Will work java-platform plugin
   * (will not work with java plugin because such module can't aggregate).
   */
  aggregateReports()
}
```

### POM

You need to specify general project info:

```groovy
group = 'your.group'                    // maven group
version = '1.0.0'                       // project version
description = 'My project description'  // optional (affects jar manifest) 
```

Note: maven `artifactId` will be the same as project name, and the default for project name
is current directory name. If you need to change name, add in `settings.gradle`:

```
rootProject.name = 'the-name-you-want'
```

For maven-central publication you need to fill all required pom sections:

```groovy
pom {
    // name and desciption set automatically from project, but you can override them here
    //name 'Project Name'
    //description 'My awesome project'
    licenses {
        license {
            name "The MIT License"
            url "http://www.opensource.org/licenses/MIT"
            distribution 'repo'
        }
    }
    scm {
        url 'https://github.com/me/my-repo'
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

Read more about pom configuration in the [pom plugin's docs](https://github.com/xvik/gradle-pom-plugin#pom-configuration). 

If your project hosted on github you may use [github-info](https://github.com/xvik/gradle-github-info-plugin) plugin, 
which fills most github-related pom sections for you automatically.

Use the following configurations to get correct scopes in the resulted pom:

Maven scope | Gradle configuration
------------| ----------------
compile     | implementation, api
runtime     | runtimeOnly
provided    | provided  (**not** compileOnly!)
optional    | optional, [feature variants](https://github.com/xvik/gradle-pom-plugin#feature-variants)

See [pom plugin doc](https://github.com/xvik/gradle-pom-plugin#dependencies) for more details about dependencies scopes in the generated pom

#### Using BOMs

When you use BOMs (for dependencies versions management) with spring plugin or gradle platform you'll have 
`dependencyManagement` section generated in the target pom. Often it is not desired:to use only resolved
versions and avoid `dependencyManagent` use:

```groovy
javaLib.pom.removeDependencyManagement()
```

Read more in the [pom plugin's docs](https://github.com/xvik/gradle-pom-plugin#improving-boms-usage)

IMPORTANT: Pom plugin will mention `pomGeneration` closure: `javaLib.pom` is just a shortcut (to group configurations).
Both configurations lead to the same object (and could be used simultaneously).

#### BOM declaration

The simplest way to declare BOM is using [java-platform](https://docs.gradle.org/current/userguide/java_platform_plugin.html)

```groovy
plugins {
  id 'java-platform'
  id 'ru.vyarus.java-lib'
}

repositories { mavenLocal(); mavenCentral() }
dependencies {
  api platform('ru.vyarus.guicey:guicey-bom:5.2.0-1')
  constraints {
    api 'org.webjars:webjars-locator:0.40'
  }
  // add subprojects to published BOM
  project.subprojects.each { api it }
}
```

Java-lib plugin would automatically activate dependencies declaration (`constraints` block).

I propose to mix dependencies and modules into single BOM declaration, but you can always split
dependencies management and modules BOM by declaring two platforms in two different modules.

If you use `java-platform` in the root project, then you might want to change name of published artifact
(by default it would be root project name in this case). To change it use:

```groovy
javaLib {
  bom {
    artifactId = 'some-bom'
    description = 'overridden description'
  }
}
```

### Publication

[maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin used for publication.

By default, plugin configures `maven` publication with javadoc or (and) groovydoc and sources jars for `java` 
(`groovy` or `java-library`) plugins and `bom` publication for `java-platform` plugin. 

Use `install` task to deploy everything into local maven repository.

```bash
$ gradlew install
``` 

If you don't want to publish everything (jar, sources, javadoc) then you can:

```groovy
javaLib {
  withtouSources()
  withoutJavadoc()
}
```

OR override list of publishing artifacts:

```groovy
publishing.publications.maven.artifacts = [jar, javadocJar]
```

NOTE that for maven central publication sources and javadocs are required

#### Gradle metadata

Since gradle 6, gradle would always publish its [metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html):

```
Gradle Module Metadata is a unique format aimed at improving dependency resolution by making it multi-platform and variant-aware.
```

Essentially, it's an additional `.module` file containing json representation of dependencies.
This is really necessary only when advanced gradle features used (constraints (not in platform), variants).

But this would mean that gradle and maven projects would use *different* dependencies
after publication: maven use pom, gradle would load .module file with additional dependencies info.

It would be more honest to publish only pom (especially for public projects) and disable metadata publishing:

```groovy
javaLib {
  withoutMavenMetadata()
}
```

Also note, that maven central could complain about metadata file (if published).

#### Publish to repository 
 
You must configure repository for actual publication [repository](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:repositories) must be configured:

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

#### Publish to maven-central

For maven-central publication use [nexus publish plugin](https://github.com/gradle-nexus/publish-plugin)
which automates full maven central release cycle. 

```groovy
plugins {
  ...
  id 'io.github.gradle-nexus.publish-plugin' version '1.1.0'
}

nexusPublishing {
  repositories {
    sonatype {
      username = findProperty('sonatypeUser')
      password = findProperty('sonatypePassword')
    }
  }
}
```

For release, you would need to call two tasks: `publishToSonatype`, `closeAndReleaseSonatypeStagingRepository`

You'll need to configure `sonatypeUser` and `sonatypePassword` properties in global gradle file:
`~/.gradle/gradle.properties`

IMPORTANT artifacts must be [signed](#signing)!

#### Gradle plugin

Gradle plugin project will have [java-gradle-plugin](https://docs.gradle.org/current/userguide/java_gradle_plugin.html), 
which declares its own maven publication `pluginMaven` (with main jar as artifact). Also, plugin creates one more 
publication per declared plugin to publish [plugin marker artifact](https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_markers)
(required by gradle plugins dsl).

Java-lib plugin will still create separate publication `maven` and you should use it for publishing with bintray 
(same way as for usual library)

There might be several situations...

##### Gradle plugin repository

For publishing in gradle plugin repository you will use [com.gradle.plugin-publish](https://plugins.gradle.org/docs/publish-plugin) 
plugin. This plugin normally adds its own source and javadoc tasks.

Java-lib plugin will prevent additional sources and javadoc tasks creation. `plugin-publish`
use artifacts from `Project.artifacts` and java-lib will register all required artifacts there.
So overall `maven` publication and artifacts applied to plugins portal will be the same.

Use `maven` publication into maven central or jcenter (or other repo). Ignore `pluginMaven` (containing just one jar).

##### Publishing only to custom repo

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

### Encodings

UTF-8 applied to:

* (all `CompileJava` tasks).options.encoding 
* (all `CompileGrovy` tasks).options.encoding
* (all `Javadoc`).options.\[encoding, charSet, docEncoding]
* (all `Test`).systemProperty 'file.encoding'

Note that groovydoc task does not have encoding configuration, but it should use UTF-8 by defautl. 

For tests, encoding is important (especially on windows) because test forked process will not inherit root gradle encoding configuration. 

### Tasks 

- `sourcesJar` task always applied 
- `javadocJar` if java sources directory present ('src/main/java')
- `groovydocJar` if groovy plugin available and groovy sources present ('src/main/groovy'). Last condition is important because groovy may be used only for tests.
- `openDependencyReport` if `project-report` plugin active - opens html dependency report in browser

IMPORTANT: if you have only groovy sources then `groovydocJar` will have javadoc` classifier! This is because maven central requires
javadoc jar, so even if you write groovy project you have to name it javadoc.

In case of both groovy and java sources, `groovydocJar` will use `groovydoc` classifier, because `javadocJar` already use `javadoc` and have to produce separate artifacts.   

`install` task added to simplify publication to local maven repository: this is simply shortcut for
gradle's [publishToMavenLocal](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:tasks) task
(simply shorter to type and more common name after maven).  

### Main Jar

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

### Signing

Plugin will configure signing automatically for *configured publications*: `maven` and `bom` 
(note that in case of gradle plugin, gradle use its own publication for portal publication
and it would not be signed (no need)).

You only need to apply [signing](https://docs.gradle.org/current/userguide/signing_plugin.html) plugin:

```groovy
plugins {
  id 'java'
  id 'signing'
  id 'ru.vyarus.java-lib'
}
```

No additional configuration required, except properties in the global gradle config `~/.gradle/gradle.properties`:

```properties
signing.keyId = 78065050
signing.password =
signing.secretKeyRingFile = /path/to/certs.gpg
```

IMPORTANT: password property (empty) required even if no password used!

Note that project build will not complain while building snapshot versions
(version ending with `-SNAPSHOT`) - signing task would be simply ignored.
But, on release gradle would fail if signing not configured properly.

#### Signing certificate

Certificate generation described in many articles around the web, for example, sonatype 
[gpg guide](https://central.sonatype.org/publish/requirements/gpg/).

I will just show required commands for generation and obtaining keyring file:

Certificate generation:

```
gpg --gen-key
``` 

(if you want, you can leave passphrase blank - just hit enter several times)

Alternatively, `gpg --full-gen-key` may be used to set exact algorithm and expiration (by default generated key would expire in few years)

List keys:

```
gpg --list-keys
gpg --list-secret-keys
```

You can always edit key if required (for example change expiration):

```
gpg --edit-key (key id)
gpg> key 1
gpg> expire
(follow prompts)
gpg> save
```

Create keyring file:

```
gpg --export-secret-keys (key id) > cert.gpg
```

Put `cert.gpg` somewhere and set full path to it in `signing.secretKeyRingFile`

You also need short key id:

```
gpg --list-secret-keys --keyid-format SHORT

Example output:
  sec   rsa3072/78065050 2021-06-06 [SC]
```

Here `78065050` is your keyid which should be set as `signing.keyId`

If you set passphrase, set it in `signing.password`, otherwise leave it blank 

IMPORTANT: for maven central, you'll need to register your public key with

```
gpg --keyserver keyserver.ubuntu.com --send-keys (short key id)
```

That's all.

### Dependency report

When [project-report](https://docs.gradle.org/current/userguide/project_report_plugin.html) plugin active,
`openDependencyReport` task created. 

This is pure utility task: it calls `htmlDependencyReport` and opens it directly
in the browser (directly on page with dependencies, instead of index).

This simply faster: manual `htmlDependencyReport` requires several clicks to open required report. 

### Maven-like multi-module project

Here is an example of how plugin could be used in multi-module project to apply
maven configuration style: root project manage all dependency versions.

```groovy
plugins {
    id 'jacoco'
    id 'java-platform'
    id 'ru.vyarus.java-lib'
}

description = 'Maven-like project'

// dependency versions management
dependencies {
    api platform("ru.vyarus:dropwizard-guicey:$guicey")
    constraints {
        api 'com.h2database:h2:1.4.200'

        // add subprojects to BOM
        project.subprojects.each { api it }
    }
}

javaLib {
    aggregateReports()
    // publish root BOM as custom artifact
    bom {
        artifactId = 'sample-bom'
        description = 'Sample project BOM'
    }
  
    // OR disable BOM publication
    // withoutPublication()
}

// maven publication related configuration applied to all projects
allprojects {
    //apply plugin: 'project-report'
    //apply plugin: 'signing'

    repositories { mavenCentral(); mavenLocal() }

    group = 'com.test'

    // delay required because java plugin is activated only in subprojects and without it
    // pom closure would reference root project only 
    afterEvaluate {
      pom {
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
        //...
      }
    }

    javaLib.withoutGradleMetadata()
}

// all sub-modules are normal java modules, using root BOM (like maven)
subprojects {
    apply plugin: 'groovy'
    apply plugin: 'jacoco'
    apply plugin: 'ru.vyarus.java-lib'

    sourceCompatibility = 1.8

    // common dependencies for all modules
    dependencies {
        implementation platform(project(':'))

        compileOnly 'com.github.spotbugs:spotbugs-annotations:4.2.3'
        implementation 'ru.vyarus:dropwizard-guicey'

        testImplementation 'org.spockframework:spock-core'
        testImplementation 'io.dropwizard:dropwizard-testing'
    }

    javaLib {
        // java 9 auto module name
        autoModuleName = "com.sample.module"
        // use only direct dependencies in the generated pom, removing BOM
        pom.removeDependencyManagement()
    }
}
```

Here required dependency versions declared in the root project using gradle platform.
Platform published as BOM with custom artifact name (dual BOM: both project modules and dependencies).

Sub-projects are java modules which use platform declared in the root project for dependency management.
`pom.removeDependencyManagement()` prevents "leaking" platform into module poms
(generated poms would contain just required dependencies with resolved versions)

`groovy` plugin used just as an example (used for spock tests, main sources might be java-only): it could be `java` or `java-library` plugin.

The complete multi-module project example could be generated with [java-library generator](https://github.com/xvik/generator-lib-java).

* [dropwizard-guicey-ext](https://github.com/xvik/dropwizard-guicey-ext) - multi-module project with (published) bom
* [yaml-updater](https://github.com/xvik/yaml-updater) - simple multi-module without bom (simple case)

### APPENDIX: boilerplate plugin removes

Section briefly shows what plugin configures so if plugin defaults didn't fit your needs, you can
easily reproduce parts of it in your custom build. 

#### Java module boilerplate

```groovy
plugins { id 'java' }

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

jar.manifest {
  attributes 'Automatic-Module-Name': 'module-name'
}  

publishing.publications {
    maven(MavenPublication) {
        from components.java
        artifact sourcesJar	        
        artifact javadocJar
        artifact groovydocJar	 
    }
}

task.jacocoTestReport.xml.required.set(true)

task install(dependsOn: publishToMavenLocal, group: 'publishing') << {
	logger.warn "INSTALLED $project.group:$project.name:$project.version"
}
```

#### Java platform boilerplate

```groovy
plugins { id 'java-platform' }

apply plugin: 'ru.vyarus.pom'

javaPlatform.allowDependencies()

pom {
  name 'custom-name'                // if differs from project name
  description 'custom description'
}

publishing.publications {
  bom(MavenPublication) {
    from components.javaPlatform
    artifactId = 'custom-name'      // if differs from project name
  }
}

jacocoTestReport.reports.xml.required.set(true)

task install(dependsOn: publishToMavenLocal, group: 'publishing') << {
  logger.warn "INSTALLED $project.group:custom-name:$project.version"
}
```

#### Reports aggregation boilerplate

```groovy
task test (type: TestReport, description: 'Generates aggregated test report') {
    group = 'verification'
    destinationDir = project.file("${project.buildDir}/reports/tests/test")
    reportOn project.subprojects.findAll { it.plugins.hasPlugin(JavaPlugin) }.test
}

def projectsWithCoverage = project.subprojects.findAll { it.plugins.hasPlugin(JacocoPlugin) }

task jacocoTestReport (type: JacocoReport, description: 'Generates aggregated jacoco coverage report') {
    dependsOn 'test'
    group = 'verification'
    executionData project.files(projectsWithCoverage
            .collect { it.file("${it.buildDir}/jacoco/test.exec") })
            .filter { it.exists() }
    sourceDirectories.from = project.files(projectsWithCoverage.sourceSets.main.allSource.srcDirs)
    classDirectories.from = project.files(projectsWithCoverage.sourceSets.main.output)
    reports.xml.destination = project.file("$project.buildDir/reports/jacoco/test/jacocoTestReport.xml")
    reports.xml.required.set(true)
    reports.html.destination = project.file("$project.buildDir/reports/jacoco/test/html/")
}

htmlDependencyReport.projects = project.allprojects
```

#### Utility boilerplate

Signing:

```groovy
signing {
  sign publishing.publications.maven    // or bom
  required = { !project.version.toString().endsWith('SNAPSHOT') }
}
```

Gradle metadata disabling

```groovy
tasks.withType(GenerateModuleMetadata).configureEach {
    enabled = false
}
```

Open report:

```groovy
task openDependencyReport(description: 'Opens gradle htmlDependencyReport in browser', group: 'help') {
  dependsOn 'htmlDependencyReport'
  doLast {
      java.awt.Desktop.desktop.open(file("build/reports/project/dependencies/root.${project.name}.html))
  }
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
