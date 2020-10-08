🎂 Cake
=======

Tabula extensively uses the [Cake Pattern][1] to provide runtime dependency injection to
a number of areas, most notably [Commands](Commands.md). This allows code to be abstracted into
traits which are themselves self-contained, which are then combined together (like making a cake mix)
to allow them to be reusable and easily testable.

Consider the example of a `Command`; if this all existed in a single class then to test
the Command, you'd have to declare all of the dependencies for the Command as a whole, but
if it's separated into components then each individual component can be tested and fulfill its
own contract.

Example
-------

If you have a `TabulaService` that's being wired from Spring, you might be able to get
it at any point at runtime by running `Wire[TabulaService]`. This doesn't make it very
easy to test as the whole autowiring system needs to be available for testing, either with
a mock context or by making `Wire` return `null` and then relying on overriding it in the test,
which isn't ideal:

```scala
class StartTabulaAction(val enabled: Boolean) {
  var tabulaService: TabulaService = Wire[TabulaService]

  def doIt(): Unit = if (enabled) {
    tabulaService.start()
  }
}
```

Consider instead a component-based example, we can define a `TabulaServiceComponent` and
a default implementation as follows:

```scala
trait TabulaServiceComponent {
  def tabulaService: TabulaService
}

trait AutowiringTabulaServiceComponent extends TabulaServiceComponent {
  override val tabulaService: TabulaService = Wire[TabulaService]
}
```

We can then rewrite our action with a self-type that defines the contract that needs to be
fulfilled; i.e. that it needs a TabulaService:

```scala
abstract class StartTabulaActionComponent(val enabled: Boolean) {
  self: TabulaServiceComponent =>
 
  def doIt(): Unit = if (enabled) {
    tabulaService.start()
  }
}

object StartTabulaAction {
  def apply(enabled: Boolean) =
    new StartTabulaActionComponent(enabled)
      with AutowiringTabulaServiceComponent
}
```

In a test case we can fulfill this contract with a mock implementation:

```scala
trait StartTabulaActionTestSupport extends TabulaServiceComponent {
  override val tabulaService: TabulaService = smartMock[TabulaService]
}

val action = new StartTabulaActionComponent(enabled = true) with StartTabulaActionTestSupport
```

[1]: http://jonasboner.com/real-world-scala-dependency-injection-di/
