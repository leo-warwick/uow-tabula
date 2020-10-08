Tabula
==========

This is Tabula, the University of Warwick's student administration system including a number of modules. It has an internal [JIRA project](https://bugs.elab.warwick.ac.uk/browse/TAB).

![Screenshot](screenshot.png)

Currently, the modules that are in Tabula are:

- common - Data model and "Commands", which perform actions in Tabula
- web - Static content and web controllers for several major sections:
  - coursework - Coursework Submission, the Assignment Management project that used to be [Horses for Courses](https://bugs.elab.warwick.ac.uk/browse/HFC)
  - exams - Exam Management, exam marks (deprecated) and exam grids
  - profiles - Student Profiles
  - groups - Small Group Teaching management
  - attendance - Monitoring point management and recording
  - mitcircs - Mitigating Circumstances submission and management
  - admin - Administration & Permissions functions
  - reports - Report generation for other modules
- api - API-specific details

Table of Contents
----------

- [Quick start](#quick-start)
  - [Pre-requisites](#pre-requisites)
- [Setting up for development](#setting-up-for-development)
  - [ActiveMQ](#activemq)
  - [Elasticsearch](#elasticsearch)
  - [Java 8 JDK](#java-8-jdk)
  - [Starting Tomcat](#starting-tomcat)
  - [Apache](#apache)
  - [Local properties](#local-properties)
  - [Troubleshooting updates](#troubleshooting-updates)
  - [Deployment](#deployment)
  - [Building assets](#building-assets)
- [Directory structure](#directory-structure)
- [WAR overlays](#war-overlays)
- [Developer documentation](docs/Home.md)

Quick start
-----------

We provide a Docker set up that can be used with `docker-compose` to build and deploy Tabula locally.

### Pre-requisites

1. You're running on Linux or macOS
2. You've installed [Docker Engine](https://docs.docker.com/install/) and [docker-compose](https://docs.docker.com/compose/install/).
3. You've got a stable hostname. If you're on-campus but don't have a hostname registered to your workstation, contact IT Services.
4. You've got an SSO provider ID registered with the IT Services Web Team.
5. You've got a stable IP address and it's whitelisted for the SSO sentry (contact IT Services Web Team if necessary).
6. You've got the latest LTS version (10, at the time of writing) of Node.js installed, with access to npm

### Building Tabula

You should be able to access source and binaries from mvn.elab.warwick.ac.uk when you resolve dependencies. If you're using an IDE (we recommend IntelliJ IDEA)
this should mean you'll get code completion and the original source of dependencies that haven't been fully open-sourced.

The gradle wrapper should be able to now build wars, if you run `./gradlew war` from the root directory it should build `api/build/libs/api.war`
and `web/build/libs/ROOT.war`.

### Running the Tomcat container with dependencies

*macOS note*: The standard `head` on macOS may be incompatible with `docker/init.sh`, you may want to get `ghead` from Homebrew and
replace instances in the script before running.

First, run `docker/init.sh` (i.e. run it from the root of the project, rather than `cd`'ing into the docker directory). This will ask you
three questions:

1. What's the hostname of your machine (i.e. localdev.warwick.ac.uk)
2. What's your email address (for obtaining an SSL certificate from Let's Encrypt)
3. What's your SSO provider ID (or leave blank for `urn:localdev.warwick.ac.uk:tabula:service` - you'll get this when you register for an SSO provider ID with the ITS Web Team).

This generates the necessary properties files to run Tabula in `docker/data`. You shouldn't need to run `docker/init.sh` again except when there's been
a change in the properties required; if you're just changing properties files you can just change these files directly without regenerating them.

You can now start the Docker containers with `sudo docker-compose up --build` (again, you'll only need `--build` when configuration changes have been made).
This will get everything running and you should get a 404 for https://localdev.warwick.ac.uk/ (and a 401 basic auth prompt for http://localdev.warwick.ac.uk:8080/manager).

Now, you can deploy the application with `./gradlew cargoDeployRemote` which will connect to the Manager application on Tomcat and deploy the application there.
If you make changes and want to re-deploy without starting from scratch you can run `./gradlew cargoRedeployRemote`; if you use JRebel and you've set it up with
the remote server correctly you should be able to make changes live without a redeploy.

### Populating data

Once you've got the application running, it'll spew some errors that are expected when there isn't any data in either
the database or Elasticsearch. You'll need to go to https://localdev.warwick.ac.uk/sysadmin (note, this requires that your
logged-in user is a member of the WebGroup `in-tabula-local-dev-sysadmins`, you can edit the generated `docker/data/tomcat/lib/tabula.properties` to use
a different group).

WebGroups can be created in the [WebGroups system](https://webgroups.warwick.ac.uk/) by staff, or you can use an existing group
such as `all-staff` to allow any staff member at the University (for example).

Click in the 3 boxes for "Rebuild audit event index from", "Rebuild profiles index from" and "Rebuild notification stream index from" so
that they're populated with a date, and click "Index". This should complete immediately as no data will be indexed, but it will create the index
structure in Elasticsearch.

Next, start the Imports in the top right:

* Departments, modules, routes etc.
* SITS assignments, ALL YEARS
* SITS module lists
* Profiles (leave deptCode empty)

As `tabula.properties` specifies this is a sandbox instance, this will complete quickly and with completely fake data.

You can then go to "List departments in the system", click a department and "View department admins" to grant permissions, enabling
God mode will allow your user to bypass permissions checks and add ordinary permissions.

Setting up for development
----------

Install the latest Tomcat 8.5 from here: http://tomcat.apache.org/download-80.cgi - on Unix, it makes sense just to extract Tomcat into `/opt` and then symlink it to `/usr/local/tomcat-8` or wherever is convenient. You can then upgrade Tomcat without breaking any configuration by just changing that symlink.

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
│   ├── jtds-1.3.1.jar
│   ├── logback.xml
│   ├── ojdbc8.jar
│   ├── postgresql-42.2.5.jar
│   ├── tabula.properties
│   ├── tabula-sso-config.xml
│   └── warwick-logging-1.1-all.jar
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

### `lib/warwick-logging-1.1.jar`

You can get this from https://pkg.elab.warwick.ac.uk/ch.qos.logback/warwick-logging-1.1-all.jar

Note that this dependency replaces previous dependencies on logback, logstash-logback-encoder, jackson and slf4j-api

### `lib/jtds-1.3.1.jar`

You can get this from https://pkg.elab.warwick.ac.uk/net.sourceforge.jtds/jtds-1.3.1.jar

### `lib/ojdbc8.jar`

You can get this from https://pkg.elab.warwick.ac.uk/oracle.com/ojdbc8.jar

### `lib/postgresql-42.2.5.jar`

You can get this from http://central.maven.org/maven2/org/postgresql/postgresql/42.2.5/postgresql-42.2.5.jar

### `lib/logback.xml`

Logback config. An alternative would be to package this up with the WARs, but that wouldn't allow us to change config per-server.

### `lib/tabula.properties`

Properties passed into the app. You can get one of these off of tabula-test and then personalise it.

#### Security keys

Don't just use the keys from tabula-test or any other instance, generate new ones for your local instance
by running `./gradlew generateEncryptionKey` and using that for `objectstore.encryptionKey`, `turnitin.tca.signingSecret` and `tabula.database.encryptionKey`.

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

#### Install ElasticSearch locally

For Ubuntu:

```
wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | sudo apt-key add -
echo "deb https://artifacts.elastic.co/packages/6.x/apt stable main" | sudo tee -a /etc/apt/sources.list.d/elastic-6.x.list
sudo apt-get update && sudo apt-get install elasticsearch -y
```

You'll need to edit `/etc/elasticsearch/elasticsearch.yml` to set `cluster.name: tabula` (or set `elasticsearch.cluster.name=elasticsearch` in `tabula.properties`.

When I ran it locally, it wouldn't start on boot by default, but I could start it with `sudo systemctl start elasticsearch`. Run `sudo systemctl enable elasticsearch` for it to run on boot.

#### Connect to the tabula-dev Elasticsearch cluster

Set the following properties in your `tabula.properties`:

```
elasticsearch.cluster.nodes=amoru.lnx.warwick.ac.uk:9200,amogu.lnx.warwick.ac.uk:9200,amomu.lnx.warwick.ac.uk:9200
elasticsearch.cluster.name=tabula-dev
elasticsearch.index.prefix=your-name-goes-here
```

*Please* make sure you change your `elasticsearch.index.prefix` or you might end up overwriting someone else's index. If you run into firewall problems, shout in #devops

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

If you run `npm run watch` from inside the `web` folder in a tab, that will constantly build assets and JRebel will
then sync these across to the war, along with WEB-INF content such as Freemarker views.

Some other useful Gradle commands:

- `./gradlew test deployToTomcat` - also run tests during deploy (no tests are run by default)
- `./gradlew web:deployToTomcat` - deploy a single module, but make module dependencies like common
    (Remember Tomcat will still deploy old wars unless you delete them!)
- `./gradlew test` - run tests, showing stack trace of failures in the console and proper output to a HTML file
- `./gradlew web:test --tests *.ApplicationTest` - run a specific test
- `./gradlew -PintegrationTest test -Dtoplevel.url=https://yourhost.warwick.ac.uk` - run functional tests (start Tomcat instance first)
- `./gradlew -PintegrationTest test -Dtoplevel.url=https://yourhost.warwick.ac.uk --tests *.CourseworkModuleManagerTest` - run a specific functional test
- `./gradlew cucumber` - run Cucumber tests (against tabula-test.warwick.ac.uk)
- `./gradlew cucumber -Dserver.environment=production` - run Cucumber tests against production
- `./gradlew cucumber -Dfeature.name=gzip` - run Cucumber scenarios for a single feature
- `./gradlew --continuous` - continuously compile sources as they are saved in your IDE
- `./gradlew test --continuous` - continuously compile sources and run tests as they are saved in your IDE (tests only run if it passes)
- `./gradlew web:test --tests *.ApplicationTest --continuous` - continously compile and run a single test
- `./gradlew generateEncryptionKey` - print a Base64-encoded encryption key for use in `tabula.properties` for object storage or database

### Building assets

The `web` module contains a call to `npm run build` to build assets with Webpack as part of the deployment.
You can run this manually to build new static content into `build/rootContent` (which is monitored by JRebel for changes).

If you don't want to mess with webpack at all, you can call `./gradlew webpack` to rebuild the assets.

Other useful `npm` commands:

- `npm run build` - build all production assets and exit
- `npm run dev` - build assets for development and exit
- `npm run watch` - watch for changes to files and re-build development assets

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
      - `assets` - JS/CSS files etc. to be asset-processed by webpack before being added to the WAR
      - `webapp` - other non-code files that make up the WAR.
      - `artwork` - source graphics not included in the app, but used to generate static images. Usually SVG/Inkscape.
    - `test`
    - `console`

WAR overlays
------------

In a module, Gradle will overlay resources from `common` when it builds a WAR. In an overlay,
files that exist aren't overwritten, so you can define a file with the same name to override behaviour
that would be defined in the overlay.

- `common/.../WEB-INF` -> `WEB-INF` - default `applicationContext.xml` and some includes that can be overridden
- `web/.../WEB-INF/spring-component-scan-context.xml` -> `WEB-INF/spring-component-scan-context.xml` - overrides the default empty one from common

Developer documentation
-----------------------

Available here: **[Developer documentation](docs/Home.md)**
