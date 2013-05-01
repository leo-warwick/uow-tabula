package uk.ac.warwick.tabula.web.views

import scala.collection.mutable.Buffer
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit
import freemarker.template.SimpleSequence
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.SecurityService
import freemarker.template.TemplateModel
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.permissions.Permissions

class MyObject extends PermissionsTarget {
  var name = "text"
  def getMotto() = "do be good, don't be bad"
  def grotto = "Santa's"
	  
  def getGreeting(name:String) = "Hello %s!" format (name)
  def getGreeting():String = getGreeting("you")
  
  def departments = "ah" :: List("ch", "cs")
  
  @Restricted(Array("GodMode")) var permsName = "text"
  @Restricted(Array("Module.Read")) def getPermsMotto() = "do be good, don't be bad"
  @Restricted(Array("Module.Read")) def permsGrotto = "Santa's"
	  
  @Restricted(Array("Module.Read")) def getPermsGreeting(name:String) = "Hello %s!" format (name)
  @Restricted(Array("Module.Read", "Module.Delete", "GodMode")) def getPermsGreeting():String = getPermsGreeting("you")
  
  override def id = ""
	override def permissionsParents = Nil
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
			case hash: wrapper.ScalaHashModel => {
				hash.get("Scotland") match {
					case hash: wrapper.ScalaHashModel => {
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
			case hash: wrapper.ScalaHashModel => {
				hash.get("greeting").toString should be ("Hello you!")
			}
			case somethingElse => fail("unexpected match; expected hash:ScalaHashModel but was a " + somethingElse + ":" + somethingElse.getClass.getSimpleName)
		}
	}
	
	@Test def scalaGetter {
	  val wrapper = new ScalaBeansWrapper()
	  wrapper.wrap(new MyObject) match {
	    case hash: wrapper.ScalaHashModel => {
	      hash.get("name").toString should be("text")
	      hash.get("motto").toString should be("do be good, don't be bad")
	      hash.get("grotto").toString should be("Santa's")
	      hash.get("departments").getClass should be (classOf[SimpleSequence])
	    }
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
	 	  case hash: wrapper.ScalaHashModel => {
	 	 	  hash.get("list") match {
	 	 	 	  case listy:SimpleSequence => listy.size should be (2)
	 	 	 	  case somethingElse => fail("unexpected match; expected listy:SimpleSequence but was a " + somethingElse + ":" + somethingElse.getClass.getSimpleName)
	 	 	  }
	 	  }
	  }
	   
	}
	
	@Test def permissions = withUser("cuscav") {
		val wrapper = new ScalaBeansWrapper()
		val securityService = mock[SecurityService]
		
		wrapper.securityService = securityService
		
		val obj = new MyObject
	  wrapper.wrap(obj) match {
	    case hash: wrapper.ScalaHashModel => {
	      hash.get("name").toString should be ("text")
	      hash.get("motto").toString should be ("do be good, don't be bad")
	      hash.get("grotto").toString should be ("Santa's")
	      hash.get("greeting").toString should be ("Hello you!")
	       
	      hash.get("permsName") should be (null)
	      hash.get("permsMotto") should be (null)
	      hash.get("permsGrotto") should be (null)
	      hash.get("permsGreeting") should be (null)
	       
	      securityService.can(currentUser, Permissions.GodMode) returns (true)
	      securityService.can(currentUser, Permissions.Module.Read, obj) returns (true)
	      securityService.can(currentUser, Permissions.Module.Delete, obj) returns (true)
	      
	      hash.get("permsName").toString should be ("text")
	      hash.get("permsMotto").toString should be ("do be good, don't be bad")
	      hash.get("permsGrotto").toString should be ("Santa's")
	      hash.get("permsGreeting").toString should be ("Hello you!")
	    }
	  }
	}
}