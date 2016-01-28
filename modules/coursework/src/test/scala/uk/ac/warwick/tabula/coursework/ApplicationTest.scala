package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.data.model._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import scala.collection.JavaConversions._
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.beans.factory.annotation.Value
import uk.ac.warwick.tabula._
import org.junit.Ignore
import scala.language.reflectiveCalls
import scala.language.implicitConversions
import org.hibernate.transform.DistinctRootEntityResultTransformer

// scalastyle:off magic.number
class ApplicationTest extends AppContextTestBase with FieldAccessByReflection{

    @Autowired var annotationMapper:RequestMappingHandlerMapping =_

    @Value("${filesystem.index.dir}") var indexDir:String =_

    @Test def handlerMappings = {
    	annotationMapper.getHandlerMethods.size should not be (0)
    	for ((info,method) <- annotationMapper.getHandlerMethods()) {

    	}
    }

    /**
     * Check that a property in default.properties can reference
     * a property found in tabula.properties, even though the latter
     * is loaded after the former.
     *
     * This is important for allowing "base.data.dir" to be set in
     * tabula.properties, and default.properties using that as
     * the root directory for many other directory locations.
     */
    @Test def defaultProperties = {
			indexDir should fullyMatch regex ("target/test-[A-Z0-9]+-tmp/index")
    }

    @Transactional @Test def hibernatePersistence = {
	  val assignment = new Assignment
	  assignment.name = "Cake Studies 1"
	  assignment.academicYear = new AcademicYear(2009)
	  session.save(assignment)
	  assignment.id should not be (null)

	  session.flush()
	  session.clear()

	  val fetchedAssignment = session.get(classOf[Assignment], assignment.id).asInstanceOf[Assignment]
	  fetchedAssignment.name should be("Cake Studies 1")
	  fetchedAssignment.academicYear should be(assignment.academicYear)
	}

    /*
     * A post-load event in Department makes sure that a null settings
     * property is replaced with a new empty group on load.
     */
    @Transactional @Test def departmentLoadEvent {

      val dept = new Department
      dept.code = "gr"

			dept.setV("settings",null)

      dept.getV("settings") == null should be(true)
      session.save(dept)

      val id = dept.id

      session.flush()
      session.clear()

      session.load(classOf[Department], id) match {
        case loadedDepartment:Department => (loadedDepartment.getV("settings") == null) should be(false)
				case _ => fail("Department not found")
      }
    }

    @Transactional @Test def getModules = {
      val modules = session.createCriteria(classOf[Module]).setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE).list
      modules.size should be (4)
      modules(0).asInstanceOf[Module].adminDepartment.name should be ("Computer Science")
    }

}
