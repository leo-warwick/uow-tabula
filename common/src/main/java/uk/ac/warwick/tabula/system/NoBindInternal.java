package uk.ac.warwick.tabula.system;

import java.lang.annotation.*;

/**
 * Runtime annotation to be added to a getter/setter
 * method to indicate that it is not to be bound by
 * Spring binding.
 *
 * Don't use this annotation directly because the default behaviour
 * is to only add it to the private field. Instead use
 * uk.ac.warwick.tabula.system.NoBind which is an alias to this
 * with an added `getter` target, so it will appear on the getter method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoBindInternal {}
