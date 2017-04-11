/**
 * Defines how various types (mainly Joda types) should be handled
 * by Hibernate. You can either use the short name in your @Type,
 * or more likely if the type matches defaultForType, you can omit
 * the @Type annotation altogether and let Hibernate work it out.
 *
 * TypeDefs can be defined in a package or class and will be global either way.
 * We've gone with a package object here, but due to a bug in scalac
 * it needs to be in an otherwise empty package. So don't add any other
 * classes to this package.
 */

@TypeDefs({
	@TypeDef(name = "dateTime",
			typeClass = org.jadira.usertype.dateandtime.joda.PersistentDateTime.class,
			defaultForType = org.joda.time.DateTime.class,
			parameters = {
				@Parameter(name = "databaseZone", value = "Europe/London"),
				@Parameter(name = "javaZone", value = "Europe/London")
			}),
	@TypeDef(name = "localDate",
			typeClass = org.jadira.usertype.dateandtime.joda.PersistentLocalDate.class,
			defaultForType = org.joda.time.LocalDate.class),
	@TypeDef(name = "localTime",
			typeClass = org.jadira.usertype.dateandtime.joda.PersistentLocalTimeAsString.class,
			defaultForType = org.joda.time.LocalTime.class)
})
package uk.ac.warwick.tabula.data.model.typedefs;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.annotations.Parameter;
