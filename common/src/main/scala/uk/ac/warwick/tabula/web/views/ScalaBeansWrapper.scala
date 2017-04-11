package uk.ac.warwick.tabula.web.views

/*
 * Taken from http://code.google.com/p/sweetscala
 */

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

import freemarker.ext.beans.BeanModel
import freemarker.template.{DefaultObjectWrapper, _}
import org.hibernate.proxy.HibernateProxyHelper
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.{RequestInfo, ScalaConcurrentMapHelpers}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget, ScopelessPermission}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityService, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{Restricted, RestrictionProvider}

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

/**
	* A implementation of BeansWrapper that support native Scala basic and collection types
	* in Freemarker template engine.
	*/
class ScalaBeansWrapper extends DefaultObjectWrapper(Configuration.VERSION_2_3_21) with Logging
	with AutowiringSecurityServiceComponent {

	// On startup, ensure this is empty. This is mainly for hot reloads (JRebel), which
	// won't know to clear this singleton variable.
	ScalaHashModel.gettersCache.clear()

	def superWrap(obj: Object): TemplateModel = {
		super.wrap(obj)
	}

	// scalastyle:off
	override def wrap(obj: Object): TemplateModel = {
		obj match {
			case Some(x: Object) => wrap(x)
			case Some(null) => null
			case None => null
			case jcol: java.util.Collection[_] => superWrap(jcol)
			case jmap: JMap[_, _] => superWrap(jmap)
			case smap: scala.collection.SortedMap[_, _] => superWrap(JLinkedHashMap(smap.toSeq: _*))
			case smap: scala.collection.Map[_, _] => superWrap(mapAsJavaMapConverter(smap).asJava)
			case sseq: scala.Seq[_] => superWrap(seqAsJavaListConverter(sseq).asJava)
			case scol: scala.Iterable[_] => superWrap(asJavaCollectionConverter(scol).asJavaCollection)
			case directive: TemplateDirectiveModel => superWrap(directive)
			case method: TemplateMethodModel => superWrap(method)
			case model: TemplateModel => superWrap(model)
			case sobj if isScalaCompiled(sobj) =>
				new ScalaHashModel(obj, this, useWrapperCache) with SecurityServiceComponent {
					def securityService: SecurityService = ScalaBeansWrapper.this.securityService
				}
			case _ => superWrap(obj)
		}
	}

	// scalastyle:on

	// whether or not to cache results of get() methods for the life of this wrapper.
	var useWrapperCache = true

	private def isScalaCompiled(obj: Any) = Option(obj) match {
		case Some(o) if !o.getClass.isArray => o.getClass.getPackage.getName.startsWith("uk.ac.warwick.tabula")
		case _ => false
	}

}

/**
	* A model that will expose all Scala getters that has zero parameters
	* to the FM Hash#get method so can retrieve it without calling with parenthesis.
	*/

/**
	* Also understands regular JavaBean getters, useful when a Java bean has been extended
	* in Scala to implement Any. If both getter type is present, one will overwrite
	* the other in the map but this doesn't really matter as they do the same thing
	*/
class ScalaHashModel(sobj: Any, wrapper: ScalaBeansWrapper, useWrapperCache: Boolean = true) extends BeanModel(sobj, wrapper) with Logging {
	self: SecurityServiceComponent =>

	import ScalaHashModel._

	private val objectClass = HibernateProxyHelper.getClassWithoutInitializingProxy(sobj)

	private def lowercaseFirst(camel: String) = camel.head.toLower + camel.tail

	private def isGetter(m: Getter) = !m.getName.endsWith("_$eq") && m.getParameterTypes.isEmpty

	private val getters = gettersCache.getOrElseUpdate(objectClass, generateGetterInformation(objectClass))

	private def generateGetterInformation(cls: Class[_]) = {
		val javaGetterRegex = new Regex("^(is|get)([A-Z]\\w*)")
		def isJavaGetter(m: Method) = javaGetterRegex.pattern.matcher(m.getName).matches

		def parse(m: Method, name: String) = {
			val restrictedAnnotation = m.getAnnotation(classOf[Restricted])
			val restrictionProviderAnnotation = m.getAnnotation(classOf[RestrictionProvider])
			val perms: PermissionsFetcher =
				if (restrictedAnnotation != null) {
					(_) => restrictedAnnotation.value map { name => Permissions.of(name) }
				}
				else if (restrictionProviderAnnotation != null) {
					Try(cls.getMethod(restrictionProviderAnnotation.value())) match {
						case Success(method) => (x) => method.invoke(x).asInstanceOf[Seq[Permission]]
						case Failure(e) => throw new IllegalStateException(
							"Couldn't find restriction provider method %s():Seq[Permission]".format(restrictionProviderAnnotation.value()), e)
					}
				}
				else (_) => Nil

			name ->(m, perms)
		}

		val scalaPairs = for (m <- cls.getMethods if isGetter(m) && !isJavaGetter(m)) yield {
			parse(m, m.getName)
		}
		val javaPairs = for (m <- cls.getMethods if isGetter(m) && isJavaGetter(m)) yield {
			val name = m.getName match {
				case javaGetterRegex(isGet, propName) => lowercaseFirst(propName)
				case _ => throw new IllegalStateException("Couldn't match java getter syntax")
			}
			parse(m, name)
		}

		// TAB-766 Add the scala pairs after so they override the java ones of the same name
		javaPairs.toMap ++ scalaPairs.toMap
	}

	// Cache child properties for the life of this model, so that their caches are useful when a property is accessed twice.
	// Not the same as TAB-469, which would cache for longer than the life of a request, causing memory leaks. This cache
	// will use a bit more memory but it won't leak outside of a request.
	private val cachedResults = new ResultsCache

	class ResultsCache {

		def getOrElseUpdate(key: String, updater: => TemplateModel): TemplateModel = {
			if (useWrapperCache) {
				cache.getOrElseUpdate(key, updater)
			} else {
				updater
			}
		}

		var cache: ConcurrentHashMap[String, TemplateModel] with ScalaConcurrentMapHelpers[String, TemplateModel] = JConcurrentMap[String, TemplateModel]()

		def clear(): Unit = cache.clear()
	}

	private def user = RequestInfo.fromThread.get.user

	private def canDo(perms: Seq[Permission]) = perms forall {
		case permission: ScopelessPermission => securityService.can(user, permission)
		case permission if classOf[PermissionsTarget].isInstance(sobj) =>
			securityService.can(user, permission, sobj.asInstanceOf[PermissionsTarget])
		case permission =>
			logger.warn("Couldn't check for permission %s against object %s because it isn't a PermissionsTarget".format(permission.toString, sobj.toString))
			false
	}

	override def get(key: String): TemplateModel = {
		cachedResults.getOrElseUpdate(key, {
			val gtr = getters.get(key)
			gtr match {
				case Some((getter, permissions)) =>
					if (canDo(permissions(sobj))) {
						val res = getter.invoke(sobj)
						wrapper.wrap(res)
					}
					else null
				case None => checkInnerClasses(key) match {
					case Some(method) => method
					case None => super.get(key)
				}
			}
		})
	}

	private def checkInnerClasses(key: String): Option[TemplateModel] = {
		try {
			Some(wrapper.wrap(Class.forName(objectClass.getName + key + "$").getField("MODULE$").get(null)))
		} catch {
			case e @ (_: ClassNotFoundException | _: NoSuchFieldException) =>
				None
		}
	}

	override def isEmpty = false

	def clearCaches() {
		cachedResults.clear()
	}
}

object ScalaHashModel {
	type Getter = java.lang.reflect.Method
	type PermissionsFetcher = Any => Seq[Permission]
	val gettersCache: ConcurrentHashMap[Class[_], Map[String, (Getter, PermissionsFetcher)]] with ScalaConcurrentMapHelpers[Class[_], Map[String, (Getter, PermissionsFetcher)]] = JConcurrentMap[Class[_], Map[String, (Getter, PermissionsFetcher)]]()
}