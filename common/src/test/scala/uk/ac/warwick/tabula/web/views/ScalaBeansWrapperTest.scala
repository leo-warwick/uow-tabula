package uk.ac.warwick.tabula.web.views

import scala.collection.mutable
import scala.collection.mutable.Buffer
import freemarker.template.SimpleSequence
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.system.permissions.RestrictionProvider
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.Permission
import freemarker.template.TemplateBooleanModel
import freemarker.ext.beans.SimpleMethodModel

class MyObject extends PermissionsTarget {
	var name = "text"
	def getMotto() = "do be good, don't be bad"
	def grotto = "Santa's"

	def getGreeting(name:String): String = "Hello %s!" format (name)
	def getGreeting():String = getGreeting("you")

	def isSomething = true
	def something = "steve"

	def departments: List[String] = "ah" :: List("ch", "cs")

	@Restricted(Array("GodMode")) var permsName = "text"
	@Restricted(Array("Module.ManageAssignments")) def getPermsMotto() = "do be good, don't be bad"
	@Restricted(Array("Module.ManageAssignments")) def permsGrotto = "Santa's"

	@Restricted(Array("Module.ManageAssignments")) def getPermsGreeting(name:String): String = "Hello %s!" format (name)
	@Restricted(Array("Module.ManageAssignments", "Module.Delete", "GodMode")) def getPermsGreeting():String = getPermsGreeting("you")

	override def id = ""
	override def permissionsParents: Stream[Nothing] = Stream.empty


	var restrictAccess:Boolean = false
	def providePermission():Seq[Permission] = if (restrictAccess) Seq(Permissions.Module.ManageAssignments) else Nil
	@RestrictionProvider("providePermission") def getRuntimeRestricted() ="Ho Ho Ho"

}

object World {
	object England {
		val plant = "Rose"
	}
	object Scotland {
		def plant = "Thistle"
	}
}

class ScalaBeansWrapperTest extends TestBase with Mockito {


	@Test def nestedObjects {
		World.Scotland.plant should be ("Thistle")

		val wrapper = new ScalaBeansWrapper()
		wrapper.wrap(World) match {
			case hash: ScalaHashModel => {
				hash.get("Scotland") match {
					case hash: ScalaHashModel => {
						hash.get("plant").toString should be ("Thistle")
					}
				}
			}
			case somethingElse => fail("unexpected match; expected hash:ScalaHashModel but was a " + somethingElse + ":" + somethingElse.getClass.getSimpleName)
		}
	}

	/**
	 * def getGreeting(name:String="you") should be able to access the
	 * default no-param version as if it were a regular getGreeting() getter.
	 */
	@Test def defaultParameters {
		val wrapper = new ScalaBeansWrapper()
		wrapper.wrap(new MyObject) match {
			case hash: ScalaHashModel => {
				hash.get("greeting").toString should be ("Hello you!")
			}
			case somethingElse => fail("unexpected match; expected hash:ScalaHashModel but was a " + somethingElse + ":" + somethingElse.getClass.getSimpleName)
		}
	}

	@Test def scalaGetter {
		val wrapper = new ScalaBeansWrapper()
		wrapper.wrap(new MyObject) match {
			case hash: ScalaHashModel => {
				hash.get("name").toString should be("text")
				hash.get("motto").toString should be("do be good, don't be bad")
				hash.get("grotto").toString should be("Santa's")
				hash.get("departments").getClass should be (classOf[SimpleSequence])
			}
			case _ => fail()
		}
		val list:JList[String] = collection.JavaConversions.bufferAsJavaList(Buffer("yes","yes"))
		wrapper.wrap(list) match {
			case listy:SimpleSequence =>
			case nope => fail("nope" + nope.getClass().getName())
		}

		class ListHolder {
			val list:JList[String] = collection.JavaConversions.bufferAsJavaList(Buffer("contents","bontents"))
		}

		new ListHolder().list.size should be (2)

		wrapper.wrap(new ListHolder()) match {
			case hash: ScalaHashModel => {
				hash.get("list") match {
					case listy:SimpleSequence => listy.size should be (2)
					case somethingElse => fail("unexpected match; expected listy:SimpleSequence but was a " + somethingElse + ":" + somethingElse.getClass.getSimpleName)
				}
			}
		}

	}

	@Test def accessingSameProperty {
		val wrapper = new ScalaBeansWrapper()
		val wrapped = wrapper.wrap(new MyObject)

		wrapped match {
			case hash: ScalaHashModel => {
				(hash.get("name") eq hash.get("name")) should be (true)
			}
			case _ => fail()
		}


	}

	@Test def permissions = withUser("cuscav") {
		val wrapper = new ScalaBeansWrapper()
		val securityService = mock[SecurityService]

		val m = JConcurrentMap[Permission, Boolean]()
		m.put(Permissions.Assignment.Read, true)
		m.containsKey(Permissions.Assignment.Read) should be (true)

		wrapper.securityService = securityService

		val obj = new MyObject
		wrapper.wrap(obj) match {
			case hash: ScalaHashModel => {
				hash.get("name").toString should be ("text")
				hash.get("motto").toString should be ("do be good, don't be bad")
				hash.get("grotto").toString should be ("Santa's")
				hash.get("greeting").toString should be ("Hello you!")

				hash.get("permsName") should be (null)
				hash.get("permsMotto") should be (null)
				hash.get("permsGrotto") should be (null)
				hash.get("permsGreeting") should be (null)

				securityService.can(currentUser, Permissions.GodMode) returns (true)
				securityService.can(currentUser, Permissions.Module.ManageAssignments, obj) returns (true)
				securityService.can(currentUser, Permissions.Module.Delete, obj) returns (true)

				hash.clearCaches()

				hash.get("permsName").toString should be ("text")
				hash.get("permsMotto").toString should be ("do be good, don't be bad")
				hash.get("permsGrotto").toString should be ("Santa's")
				hash.get("permsGreeting").toString should be ("Hello you!")
			}
		}
	}

	@Test def nameCollision {
		// TAB-766

		val wrapper = new ScalaBeansWrapper()
		wrapper.wrap(new MyObject) match {
			case hash: ScalaHashModel => {
				hash.get("something").toString should be("steve")
				hash.get("isSomething").asInstanceOf[SimpleMethodModel].exec(JList()) should be (TemplateBooleanModel.TRUE)
			}
			case _ => fail()
		}
	}

	@Test
	def runtimePermisionsRestriction = withUser("cuscav") {

		val wrapper = new ScalaBeansWrapper()
		val securityService = mock[SecurityService]

		val m = JConcurrentMap[Permission, Boolean]()
		m.put(Permissions.Assignment.Read, true)
		m.containsKey(Permissions.Assignment.Read) should be (true)

		wrapper.securityService = securityService
		val target = new MyObject
		var wrapped = wrapper.wrap(target).asInstanceOf[ScalaHashModel]
		// initially, there are no permissions set, so we can read the value
		wrapped.get("runtimeRestricted").toString() should be("Ho Ho Ho")
		wrapped.get("runtimeRestricted").toString() should be("Ho Ho Ho")

		// now change the object's state so that it applies permissions
		// wrappers assume that objects are immutable, so we'll have to create a new one.
		target.restrictAccess = true
		wrapped = wrapper.wrap(target).asInstanceOf[ScalaHashModel]
		wrapped.get("runtimeRestricted") should be(null)

		// finally, give the current user permissions, and make sure he can see the value again.
		securityService.can(currentUser, target.providePermission().head, target) returns (true)
		wrapped = wrapper.wrap(target).asInstanceOf[ScalaHashModel]
		wrapped.get("runtimeRestricted").toString() should be("Ho Ho Ho")

	}

	@Test
	def cachesResultsOfGettersByDefault(){
		val wrapper = new ScalaBeansWrapper
		val target = new MyObject
		val wrapped = wrapper.wrap(target).asInstanceOf[ScalaHashModel]
		wrapped.get("name").toString should be("text")
		target.name="something different"
		// method is not re-invoked
		wrapped.get("name").toString should be("text")
	}

	@Test
	def doesntCacheIfToldNotTo(){
		val wrapper = new ScalaBeansWrapper
		wrapper.useWrapperCache = false
		val target = new MyObject
		val wrapped = wrapper.wrap(target).asInstanceOf[ScalaHashModel]
		wrapped.get("name").toString should be("text")
		target.name="something different"
		// method is not re-invoked
		wrapped.get("name").toString should be("something different")

	}


}