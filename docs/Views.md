Views
=====

Tabula uses [Freemarker](https://freemarker.apache.org/docs/index.html) template engine for rendering
HTML and text (for notifications/emails). This has the advantage over JSP that it can be easily rendered
from a unit test, so templates can be tested (and this is vital for text templates where spacing is
important), but has the downside that the templates aren't compiled so errors won't be picked up until
the template is evaluated.

The views use Apache Tiles to compose the layout, behind the scenes, but for the most part this can
be ignored - you wouldn't edit the Tiles `common-views.xml` when adding or modifying a view.

Writing safe templates
----------------------

When you're creating or modifying a template that generates HTML, you should use an `.ftlh` file to
ensure that HTML escaping is enabled by default. When you're modifying older templates you'll probably
find that all the content is wrapped in `<#escape x as x?html>` which has the same effect as giving
the file a `.ftlh` extension.

Macros
------

Commonly your template will use formatting macros from `formatters.ftl` and others, which are available under
the `fmt` prefix. Macros available to all templates are imported in `prelude.ftl`.

If you have a common set of macros for a set of templates or a component, you can put them in a separate
file and then import them with (e.g.) `<#import "*/group_components.ftlh" as components />` and then
call them with the `components` prefix.
