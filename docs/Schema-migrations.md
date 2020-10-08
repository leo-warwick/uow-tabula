Schema Migrations
=================

We use [Flyway](https://flywaydb.org/) for schema migrations, which is unfortunately
much less sophisticated than Play Evolutions or similar libraries that we use in other 
projects.

Flyway only supports "ups" in the evolution sense that we're used to; each file increments
the database version by 1 and the filenames must follow a specific format:

`V{version_number}__{name}.sql`

In general, you'd start the name with the date, so it'd be something like:

`V1__2019-01-16_create_schema.sql`

Note the *two* underscores after the version number, this is important. Don't change
any migrations after they've been committed to the repo, because Flyway will refuse to
start if the migration file doesn't exactly match the checksum that it has in the
`flyway_schema_history` table in the database.

These schema migrations go in `common/src/main/resources/db/migration`.

We configure the `flyway` bean such that Spring will call
[Flyway#migrate()](https://flywaydb.org/documentation/api/javadoc/org/flywaydb/core/Flyway#migrate())
as the `init-method` and perform necessary migrations when the bean is initialised at application
startup time.

<!-- https://flywaydb.org/documentation/api/#spring-configuration -->
