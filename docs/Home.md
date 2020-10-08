Tabula developer documentation
==============================

Tabula is written in Scala, using the Spring MVC framework and Freemarker templating language.

- [Code style](Code-Style.md)
- [🎂 Cake](Cake.md)
- [Controllers and autowiring](Controllers.md)
- [Commands](Commands.md)
  - [Applying](Commands.md#applying)
  - [Permissions checking](Commands.md#permissions-checking)
  - [Binding data from the request](Commands.md#binding-data-from-the-request)
  - [Populating defaults in the request data](Commands.md#populating-defaults-in-the-request-data)
  - [Running code on bind (pre-validation)](Commands.md#running-code-on-bind-pre-validation)
  - [Validating data](Commands.md#validating-data)
  - [Audit logging (Describable)](Commands.md#audit-logging-describable)
  - [Read-only commands](Commands.md#read-only-commands)
  - [Notifications](Commands.md#notifications)
    - [Sending notifications when a command runs](Commands.md#sending-notifications-when-a-command-runs)
    - [Scheduling notifications for the future](Commands.md#scheduling-notifications-for-the-future)
    - [Completing notifications](Commands.md#completing-notifications)
  - [Triggers](Commands.md#triggers)
  - [Benchmarking execution time](Commands.md#benchmarking-execution-time)
- [Permissions](Permissions.md)
- [Views](Views.md)
  - [Style guide 🗎](style-guide.pdf)
- [Database schema changes](Schema-migrations.md)
- [Dependency upgrades](Dependency-upgrades.md)
- [Security](Security.md)
