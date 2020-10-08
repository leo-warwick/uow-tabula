Code Style
==========

Unless otherwise stated, try to follow the [Effective Scala][1] Guide.
    
The code style for the project has developed as we've worked on it and so there are some bits of code that are in a style
that we later decided was not great. We should decide how to do certain things and fix the old code.

- `Command.apply()` should be defined _and called_ with parentheses. Your `applyInternal`
  may not have side effects, but a command in general does.

- Should always use dots to call methods (e.g. `command.validate(errors)` rather than `command validate errors`) unless it's
  a DSL (specific example being the test framework where you can write `assignment.name should be ("Jim")`)

- Methods that have a side-effect should have parentheses. No-paren methods should only be for getters. So `form.onBind()`, not `form.onBind`.

- Preferred method of doing a foreach loop is `for (foo <- fooList)`

- A `map` operation should always use `map` instead of `for (item <- seq) yield item.mappedValue`; for-comprehensions should
  only be used where there are multiple generators

- Some controllers don't have "Controller" at the end of their name but that turned out to be confusing, so they should all end with Controller.

[1]: http://twitter.github.io/effectivescala/
