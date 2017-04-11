package uk.ac.warwick.tabula.system
import org.springframework.beans.PropertyEditorRegistrar
import org.springframework.beans.PropertyEditorRegistry
import java.beans.PropertyEditor
import uk.ac.warwick.util.web.bind.TrimmedStringPropertyEditor
import language.implicitConversions
import language.reflectiveCalls
import scala.reflect.ClassTag

class CommonPropertyEditors extends PropertyEditorRegistrar {

	// define a neater `register` method for P.E.R.
	implicit class CleverRegistry(registry: PropertyEditorRegistry) {
		def register[A](editor: PropertyEditor)(implicit tag: ClassTag[A]): Unit =
			registry.registerCustomEditor(tag.runtimeClass, editor)
	}

	override def registerCustomEditors(registry: PropertyEditorRegistry) {
		registry.register[String](new TrimmedStringPropertyEditor)
	}

}