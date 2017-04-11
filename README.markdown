Tabula
==========

This is Tabula, the MyDepartment system including a number of modules. It has a [JIRA project](https://bugs.elab.warwick.ac.uk/browse/TAB).

Currently, the modules that are in Tabula are:

- web - Static content and homepage
  - coursework - Coursework Submission, the Assignment Management project that used to be [Horses for Courses](https://bugs.elab.warwick.ac.uk/browse/HFC)
  - profiles - Student Profiles
  - groups - Small Group Teaching management
  - attendance - Monitoring point management and recording
  - admin - Administration & Permissions functions
  - reports - Report generation for other modules
- api - API-specific details

Note: This is likely to change to being 3 modules: web, reports and api (possibly with common? commands? who knows).

Setting up for development
----------

Install Tomcat 8 from here: http://tomcat.apache.org/download-80.cgi - on Unix, it makes sense just to extract Tomcat into `/opt` and then symlink it to `/usr/local/tomcat-8` or wherever is convenient. You can then upgrade Tomcat without breaking any configuration by just changing that symlink.

Create a new base directory for Tabula's config and deployment, e.g. `/var/tomcat/instances/tabula`. The structure and contents of this directory should be as follows:

```
tabula/
├── bin/
│   └── setenv.sh
├── conf/
│   ├── context.xml
│   ├── fim-ds.xml
│   ├── server.xml
│   ├── sits-ds.xml
│   ├── tabula-ds.xml
│   └── web.xml
├── lib/
│   ├── jackson-annotations-2.6.5.jar
│   ├── jackson-core-2.6.5.jar
│   ├── jackson-databind-2.6.5.jar
│   ├── jtds-1.3.1.jar
│   ├── logback-access-1.1.7.jar
│   ├── logback-classic-1.1.7.jar
│   ├── logback-core-1.1.7.jar
│   ├── logback.xml
│   ├── logstash-logback-encoder-4.7.jar
│   ├── ojdbc7.jar
│   ├── slf4j-api-1.7.20.jar
│   ├── tabula.properties
│   └── tabula-sso-config.xml
├── logs/
├── temp/
├── webapps/
└── work/
```

You may find it useful to symlink `/usr/local/tomcat-8/instances` to `/var/tomcat/instances` if you're comfortable with Jboss-style layouts.

The contents of these files are as follows:

### `bin/setenv.sh`

This is a script that's run to set environment variables for the running instance. You'll probably want to use it to set up memory management, JRebel, debugging etc.

A sample file can be found in `config/servers/augustus/bin`

### `conf/context.xml`

This contains resources that are to be included in the server - in this case, just JNDI DataSources. You can use entity XML includes to have these in separate files.

There is a sample `context.xml` and some sample datasources in `config/servers/augustus/conf`

### `conf/server.xml`

Instructions on how to run the server, including what ports to bind to. This is pretty much standard.

There is a sample `server.xml` in `config/servers/augustus/conf`

### `conf/web.xml`

This is just copied from the `conf` directory in the Tomcat 8 install. I couldn't get Tomcat to run without it being copied, which sucks a bit.

### `lib/jackson-annotations-2.6.5.jar`

You can get this from http://pkg.elab.warwick.ac.uk/net.logstash.logback/dependencies/jackson-annotations-2.6.5.jar

### `lib/jackson-core-2.6.5.jar`

You can get this from http://pkg.elab.warwick.ac.uk/net.logstash.logback/dependencies/jackson-core-2.6.5.jar

### `lib/jackson-databind-2.6.5.jar`

You can get this from http://pkg.elab.warwick.ac.uk/net.logstash.logback/dependencies/jackson-databind-2.6.5.jar

### `lib/jtds-1.3.1.jar`

You can get this from http://pkg.elab.warwick.ac.uk/net.sourceforge.jtds/jtds-1.3.1.jar

### `lib/logback-access-1.1.7.jar`

You can get this from http://pkg.elab.warwick.ac.uk/ch.qos.logback/logback-access-1.1.7.jar

### `lib/logback-classic-1.1.7.jar`

You can get this from http://pkg.elab.warwick.ac.uk/ch.qos.logback/logback-classic-1.1.7.jar

### `lib/logback-core-1.1.7.jar`

You can get this from http://pkg.elab.warwick.ac.uk/ch.qos.logback/logback-core-1.1.7.jar

### `lib/logstash-logback-encoder-4.7.jar`

You can get this from http://pkg.elab.warwick.ac.uk/net.logstash.logback/logstash-logback-encoder-4.7.jar

### `lib/slf4j-api-1.7.20.jar`

You can get this from http://pkg.elab.warwick.ac.uk/net.logstash.logback/dependencies/slf4j-api-1.7.20.jar

### `lib/ojdbc7.jar`

You can get this from http://pkg.elab.warwick.ac.uk/oracle.com/ojdbc7.jar

### `lib/logback.xml`

Logback config. An alternative would be to package this up with the WARs, but that wouldn't allow us to change config per-server.

### `lib/tabula.properties`

Properties passed into the app. You can get one of these off of tabula-test and then personalise it.

### `lib/tabula-sso-config.xml`

The usual SSO config guff. You will need to get this configuration added to Web Sign-on for SSO to work.
Ensure it includes a `<trustedapps />` section or you _will_ see NPEs from one of the bouncycastle crypto libraries.
Also, ensure that the cluster datasource matches the sample - older configs may not match, which will cause _datasource not found_ exceptions.

### ActiveMQ

Unfortunately, the only way to get this working on Tomcat is to install ActiveMQ locally.

#### Ubuntu 14.04

Install ActiveMQ from the package repo and add a new config with the broker name Tabula.

#### Ubuntu 14.10

The ActiveMQ package in the utopic repo is completely broken, so we'll use an external repo to get the latest version.

```
sudo apt-add-repository 'deb http://dl.bintray.com/jmkgreen/deb /'
sudo apt-get update
sudo apt-get install activemq
```

Ignore the warning about unauthenticated packages. Once it's finished, edit `/etc/activemq/activemq.xml` and look for where it says `brokerName="localhost"` - change that to `brokerName="tabula"`. Restart ActiveMQ with `sudo service activemq restart`.

Set the following in your `tabula.properties`:

```
activemq.broker=tcp://localhost:61616
```

### Elasticsearch

To run this locally, you have a few options:

#### Run an ElasticSearch cluster in the JVM

You should be able to set `elasticsearch.cluster.local_jvm=true` in your `tabula.properties` and just start the app as normal, it'll store in whatever you have `filesystem.index.dir` set as (you'll already have this set as this is where it stores the fs indexes at the moment)

#### Install ElasticSearch locally

For Ubuntu:

```
wget -qO - https://packages.elastic.co/GPG-KEY-elasticsearch | sudo apt-key add -
echo "deb http://packages.elastic.co/elasticsearch/2.x/debian stable main" | sudo tee -a /etc/apt/sources.list.d/elasticsearch-2.x.list
sudo apt-get update && sudo apt-get install elasticsearch -y
```

You'll need to edit `/etc/elasticsearch/elasticsearch.yml` to set `cluster.name: tabula` (or set `elasticsearch.cluster.name=elasticsearch` in `tabula.properties`.

When I ran it locally, it wouldn't start on boot by default, but I could start it with `sudo systemctl start elasticsearch`

#### Connect to the tabula-dev Elasticsearch cluster

Set the following properties in your `tabula.properties`:

```
elasticsearch.cluster.nodes=lamso-tabula-dev-es.lnx.warwick.ac.uk:9300,lamvi-tabula-dev-es.lnx.warwick.ac.uk:9300,lanlo-tabula-dev-es.lnx.warwick.ac.uk:9300
elasticsearch.cluster.name=tabula-dev
elasticsearch.index.prefix=mmannion
```

*Please* make sure you change your `elasticsearch.index.prefix` or you might end up overwriting someone else's index. If you run into firewall problems, shout in #ops

### Java 8 JDK

Note: On Ubuntu, at least, you may need to reboot to clean your environment up after switching JDKs. This sucks, if anyone can improve it, that'll be great.

Add the webupd8team PPA if you haven't already:

```
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
```

Set Java 8 as the default JDK: `sudo apt-get install oracle-java8-set-default`

### Starting Tomcat

You'll need to set two environment variables, `CATALINA_HOME` and `CATALINA_BASE` when starting Tomcat. `CATALINA_HOME` is the directory where Tomcat is installed, and `CATALINA_BASE` is the directory where your config files above are.

You can then use `$CATALINA_HOME/bin/catalina.sh` to start the server in the foreground, or `startup.sh` to start it in the background.

An example startup script for Tabula may be:

```
#!/bin/bash

# Clear out work and temp because I'm a massive pessimist
cd /var/tomcat/instances/tabula
rm -rf work/* temp/*

export CATALINA_HOME=/usr/local/tomcat-8
export CATALINA_BASE=/var/tomcat/instances/tabula

echo "Starting Tomcat server"
${CATALINA_HOME}/bin/catalina.sh run $@
```

### Apache

Set up an Apache vhost referencing the include files in `config/servers/common/vhosts` -
use `rewrites.inc` for full Tabula development.

You need an HTTPS vhost for SSO so if you're only going to set up one vhost,
it should be the HTTPS one. The include files reference a map to get the port to use,
so you may need to define yours with three lines such as

    RewriteMap api txt:/etc/apache2/tabulaport.txt
    RewriteMap proxy txt:/etc/apache2/tabulaport.txt
    RewriteMap scheduler txt:/etc/apache2/tabulaport.txt

The above line should point to a file containing this line (assuming default Tomcat port 8080):

    port 8080

### Local properties

Copy `gradle-local-example.properties` as `gradle-local.properties` and edit the properties in there
to match your system. `gradle-local.properties` will be ignored by Git.

### Troubleshooting updates

If updating from an older version of Tabula, remember to apply recent database migrations from `config/scripts/schema`.
If you get an `IllegalStateException` from Lucene, it means one of your indexes is out of date. Find its location
(look in `tabula.properties` for `base.data.dir` and look inside the `index` directory within). Delete the relevant index
(eg. `rm -fr BASE_DATA_DIR/index/notifications`), and rebuild it from /sysadmin within the app.

### Deployment

Run `./gradlew deployToTomcat` to build the app and copy an exploded WAR to the
location you specified in your properties file.

Gradle will initialise a zinc server for doing incremental compiles, but that only lasts for the duration that the Gradle
daemon runs for - this makes it pretty inefficient for doing lots of compiles (luckily, the compile speed is pretty good,
even for Scala).

Gradle unit test results go to a HTML file which is pretty good, so you probably don't want to spend too long trying to make the
console output more verbose.

If you run `node_modules/.bin/gulp` from inside the `web` folder in a tab, that will constantly build assets and JRebel will
then sync these across to the war, along with WEB-INF content such as Freemarker views.

Some other useful Gradle commands:

- `./gradlew test deployToTomcat` - also run tests during deploy (no tests are run by default)
- `./gradlew web:deployToTomcat` - deploy a single module, but make module dependencies like common
    (Remember Tomcat will still deploy old wars unless you delete them!)
- `./gradlew test` - run tests, showing stack trace of failures in the console and proper output to a HTML file
- `./gradlew web:test --tests *.ApplicationTest` - run a specific test
- `./gradlew integrationTest` - run functional tests (start Tomcat instance first)
- `./gradlew integrationTest --tests *.CourseworkModuleManagerTest` - run a specific functional test
- `./gradlew cucumber` - run Cucumber tests (against tabula-test.warwick.ac.uk)
- `./gradlew cucumber -Dserver.environment=production` - run Cucumber tests against production
- `./gradlew cucumber -Dfeature.name=gzip` - run Cucumber scenarios for a single feature
- `./gradlew --continuous` - continuously compile sources as they are saved in your IDE
- `./gradlew test --continuous` - continuously compile sources and run tests as they are saved in your IDE (tests only run if it passes)
- `./gradlew web:test --tests *.ApplicationTest --continuous` - continously compile and run a single test

### Building assets

The `web` module contains a call to `node_modules/.bin/gulp assets` to build assets as part of the deployment.
You can run this manually to build new static content into `build/rootContent` (which is monitored by JRebel for changes).

If you don't want to mess with gulp at all, you can call `./gradlew gulp_assets` to rebuild the assets (or replace `assets` with another task name as below).

Other useful `gulp` commands:

- `gulp assets` - build all assets and exit
- `gulp` - build all assets then watch for any changes and rebuild
- `gulp watch-assets` - just watch for changes before rebuilding
- `gulp clean` - clean out anything from `build/rootContent` (sometimes necessary if you delete files)
- `gulp concat-scripts-id6` - generate the concatenated JS files for ID6 modules
- `gulp concat-scripts-id7` - generate the concatenated JS files for ID7 modules
- `gulp compile-less` - compile any LESS files to CSS

At the moment, we still have a lot of JS libraries in `src/main/assets/static/libs/` - these should be gradually replaced
with proper dependency management in `package.json` (we already do this for ID7). If you need to update ID7, change the version
number in `package.json` and run `npm install` (the build will do this automatically).

Directory structure
----------

- `config`
  - `servers`
    - `common` - stuff most servers use for Apache config etc.
  - `scripts`
    - `schema` - SQL migration scripts for any database schema changes.
- `api|common|web` - Tabula modules
  - `src`
    - `main`
      - `scala` - Scala source files
      - `java` - Java source files
      - `resources` - non-code files that will be available in the app classpath
      - `assets` - JS/CSS files etc. to be asset-processed by gulp before being added to the WAR
      - `webapp` - other non-code files that make up the WAR.
      - `artwork` - source graphics not included in the app, but used to generate static images. Usually SVG/Inkscape.
    - `test`
    - `integration-test`
    - `console`

WAR overlays
------------

In a module, Gradle will overlay resources from `common` when it builds a WAR. In an overlay,
files that exist aren't overwritten, so you can define a file with the same name to override behaviour
that would be defined in the overlay.

- `common/.../WEB-INF` -> `WEB-INF` - default `applicationContext.xml` and some includes that can be overridden
- `web/.../WEB-INF/spring-component-scan-context.xml` -> `WEB-INF/spring-component-scan-context.xml` - overrides the default empty one from common

Database schema changes
---------

Any SQL for changing the database schema should go in `config/scripts/schema/migrations`. Migrations are done
manually so you need to run it on dev, test and production separately. The recommended route is to
migrate dev first to get it working, and then update test and production _at the same time_. If you
don't do these at the same time then you run the risk of everything apparently working fine on test
until you deploy to live, which then explodes.

It is also best to do these before or soon after you've pushed the new code onto the central develop branch,
so that a deploy to tabula-test won't cause explosions.

Code style
----------

The code style for the project has developed as we've worked on it and so there are some bits of code that are in a style
that we later decided was not great. We should decide how to do certain things and fix the old code. It's possible that for
plain Scala style we can just delegate to http://docs.scala-lang.org/style/ but we also have things in our app that are best
used in a certain way, or Spring offers multiple ways of doing something and we want to document the one we've settled on.

- Should always use dots to call methods (e.g. `command.validate(errors)` rather than `command validate errors`) unless it's
  a DSL (specific example being the test framework where you can write `assignment.name should be ("Jim")`)

- Methods that have a side-effect should have parentheses. No-paren methods should only be for getters. So `form.onBind()`, not `form.onBind`.

- Preferred method of doing a foreach loop is `for (foo <- fooList)`

- A `map` operation should always use `map` instead of `for (item <- seq) yield item.mappedValue`; for-comprehensions should
  only be used where there are multiple generators

- Some controllers don't have "Controller" at the end of their name but that turned out to be confusing, so they should all end with Controller.

### Validation

- Always add the `@Valid` annotation to the controller method argument.

- Use validation annotations on your commands for simple things if you want.

- For custom code, make the command extend `SelfValidating` and in the controller body do `validatesSelf[DeleteFeedbackCommand]`

### Running code on bind (pre-validation)

- Mixin `BindListener` on your command instead of calling `onBind` from a controller
