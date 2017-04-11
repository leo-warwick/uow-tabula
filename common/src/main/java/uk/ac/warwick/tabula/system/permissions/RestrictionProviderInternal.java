package uk.ac.warwick.tabula.system.permissions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runtime annotation to be added to a field or a method to indicate
 * that its access inside a view is restricted by some permissions,
 * which must be obtained at runtime by calling a function on an instance
 * of the object.
 * The scope is the containing object (although it's not checked for
 * scope-less permissions, obviously).
 *
 * Don't use this annotation directly because the default behaviour
 * is to only add it to the private field. Instead use
 * uk.ac.warwick.tabula.system.permissions.RestrictionProvider which is an alias to this
 * with an added `getter` target, so it will appear on the getter method.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RestrictionProviderInternal {

    /**
     * Name of the function to call. The function must return Seq[Permission]
     */
    String value();

}