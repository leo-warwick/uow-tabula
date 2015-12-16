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

    @Value("${filesystem.index.audit.dir}") var auditIndexDir:String =_

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
    	auditIndexDir should fullyMatch regex ("target/test-[A-Z0-9]+-tmp/index/audit")
    }

    @Transactional @Test def hibernatePersistence = {
	  val assignment = new Assignment
	  assignment.name = "Cake Studies 1"
	  assignment.academicYear = new AcademicYear(2009)
	  session.save(assignment)
	  assignment.id should not be (null)

	  session.flush()
	  session.clear()

	  val fetchedAssignment = session.getById[Assignment](assignment.id).get
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

      session.getById[Department](id) match {
        case Some(loadedDepartment: Department) => (loadedDepartment.getV("settings") == null) should be(false)
				case _ => fail("Department not found")
      }
    }

    @Transactional @Test def getModules = {
      val modules = session.newCriteria(classOf[Module]).distinct.seq
      modules.size should be (4)
      modules.head.asInstanceOf[Module].adminDepartment.name should be ("Computer Science")
    }

}
