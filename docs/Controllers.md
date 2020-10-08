Controllers and autowiring
==========================

Tabula's Spring controllers tend to be quite lightweight, they simply declare any request mappings
(i.e. URIs to access the controller); typically a single Command as a `@ModelAttribute` to bind
the request to; any other `@ModelAttribute`s to pass information to views and then the views
themselves.

Controllers should always be in the `web` or `api` component of Tabula, whereas commands tend to
be in the `common` component so they can be shared between the two.

Minimal example
---------------

```scala
// Define path variables to bind to inside {}
@Controller
@RequestMapping(Array("/admin/{department}/{module}"))
class MyController extends BaseController {

  // Declare that any Command that implements SelfValidating will validate itself
  validatesSelf[SelfValidating]

  // The CurrentUser parameter here is automatically bound to the current user
  @ModelAttribute("myCommand")
  def myCommand(@PathVariable department: Department, @PathVariable module: Module, user: CurrentUser): MyCommand.CommandType = {
    // The converters for Department, Module etc may return null. mandatory() will throw an
    // ItemNotFoundException (which will generate a 404) if a null value is passed to it
    // mustBeLinked is a helper to ensure that one entity relates to another
    mustBeLinked(mandatory(module), mandatory(department))
    MyCommand(department, module, user)
  }
  
  // Use a @RequestMapping (or a @GetMapping) with no arguments to catch all requests that aren't caught
  // by a more specific mapping
  // Typically these methods return a plain String if just a view name, or a Mav() to add model attributes
  @RequestMapping
  def form(): String = "admin/module" // Automatically maps to src/main/webapp/WEB-INF/freemarker/admin/module.ftlh
  
  // Catches all POST requests. The Errors parameter *must* always follow *immediately* after the
  // @Valid parameter that it's catching the errors for
  @PostMapping
  def submit(@Valid @ModelAttribute("myCommand") command: MyCommand.CommandType, errors: Errors): Mav =
    if (errors.hasErrors) Mav(form())
    else {
      val result = command.apply()
      // Redirect() takes into account a redirectTo parameter. RedirectForce ignores that
      RedirectForce(Routes.component.admin.thing(result))
    }

}
```

Autowiring
----------

It's relatively uncommon that you'd need to wire a service into a Controller, but if you need to
you can use the same cake style as for wiring services into commands by having the controller
extend `AutowiringMyServiceComponent` (e.g.).

Adding breadcrumbs into the navigation
--------------------------------------

You can call `.crumbs()` and `.secondCrumbs()` on a `Mav` to add context into the navigation.
Typically you'd do this particularly for [academic-year scoped controllers](#academic-year-scoped-controllers), 
in the case of `.secondCrumbs()` (see below).

For example:

```scala
def form(@PathVariable department: Department): Mav =
  Mav("admin/department")
    .crumbs(MyComponentBreadcrumbs.admin, MyComponentBreadcrumbs.admin.department(department))
```

Department scoped controllers
-----------------------------

In order to have contextual navigation between controllers that act on a Department (and nothing
more specific), you can mix in `DepartmentScopedController` to your controller. This has a contract
that also requires:

* The controller fits the self-type `BaseController with UserSettingsServiceComponent with ModuleAndDepartmentServiceComponent with MaintenanceModeServiceComponent`,
  which would normally mean that your controller would mixin `DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent with AutowiringMaintenanceModeServiceComponent`
* Two methods, `departmentPermission: Permission` and `activeDepartment(department: Department): Option[Department]`
  to restrict to just departments that the user has a permission on. Typically this would look like:
  
```scala
@Controller
@RequestMapping(Array("/admin/{department}"))
class MyController
  extends BaseController
    with DepartmentScopedController
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringUserSettingsServiceComponent
    with AutowiringMaintenanceModeServiceComponent {
    
  override val departmentPermission: Permission = MyCommand.RequiredPermission

  // Just delegate to the PathVariable for the active department
  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] =
    retrieveActiveDepartment(Option(department))

  ...    
    
}
```

This then populates model attributes that can be called in the view to provide a drop-down to
navigate between departments for the same page, including sub-departments. For example:

```ftl
<#-- Provide a function to generate a route based on a passed department -->
<#function route_function dept>
  <#local result><@routes.component.myAction dept /></#local>
  <#return result />
</#function>
<@fmt.id7_deptheader "My action" route_function "for" />
```

Academic year scoped controllers
--------------------------------

Similarly to department-scoped controllers, can be used to generate tertiary navigation on a page
to swap between academic year views of a page. Mix in `AcademicYearScopedController` and abide by
its contract:

* The controller fits the self-type `BaseController with UserSettingsServiceComponent with MaintenanceModeServiceComponent`,
  which would normally mean that your controller would mixin `AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent`
* Override _either_ `def activeAcademicYear(academicYear: AcademicYear): Option[AcademicYear]` or
  `def activeAcademicYear: Option[AcademicYear]` to provide an entry that returns a `Some`. Which
  one is overridden depends on whether your controller has the `academicYear` available as a
  `@PathVariable` or whether it just defaults to the current year. Typically this looks like:

```scala
@Controller
@RequestMapping(Array("/admin/{department}/{academicYear:\d{4}}"))
class MyController
  extends BaseController
    with AcademicYearScopedController
    with AutowiringUserSettingsServiceComponent
    with AutowiringMaintenanceModeServiceComponent {

  // Just delegate to the PathVariable for the active academic year
  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    retrieveActiveAcademicYear(Option(academicYear))

  @RequestMapping
  def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav =
    Mav("admin/department-year")
      .crumbs(MyComponentBreadcrumbs.Admin.HomeForYear(department, academicYear, active = true))
      .secondCrumbs(academicYearBreadcrumbs(academicYear)(Routes.component.admin.home(department, _)): _*)
    
}
```
