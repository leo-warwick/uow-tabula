package uk.ac.warwick.tabula.system.permissions;

import java.lang.annotation.*;

/**
 * Runtime annotation to be added to a field or a method to indicate
 * that its access inside a view is restricted by the permissions passed.
 * The scope is the containing object (although it's not checked for
 * scope-less permissions, obviously).
 *
 * Don't use this annotation directly because the default behaviour
 * is to only add it to the private field. Instead use
 * uk.ac.warwick.tabula.system.permissions.Restricted which is an alias to this
 * with an added `getter` target, so it will appear on the getter method.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RestrictedInternal {

	/**
	 * The name of the permissions to check. We can't pass Permissions directly
	 * here as annotations don't support that. Boo
	 */
	String[] value();

}
