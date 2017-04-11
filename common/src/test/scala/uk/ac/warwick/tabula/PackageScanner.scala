package uk.ac.warwick.tabula

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.`type`.filter.AssignableTypeFilter
import uk.ac.warwick.tabula.data.model.Notification
import scala.reflect._
import scala.collection.JavaConverters._
import java.lang.annotation.Annotation

object PackageScanner {
	/**
	 * Returns all non-abstract Classes that extend the given type, within
	 * the given package.
	 */
	def subclassesOf[A : ClassTag](packageName: String): Seq[Class[_]] = {
		val scanner = new ClassPathScanningCandidateComponentProvider(true)
		scanner.addIncludeFilter(new AssignableTypeFilter(classTag[A].runtimeClass))
		val components = scanner.findCandidateComponents(packageName)
		components.asScala.map{_.getBeanClassName}.toSeq.sorted.map(Class.forName).map{_.asInstanceOf[Class[A]]}
	}

}
