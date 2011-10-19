package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.data.DepartmentDao
import org.specs.mock.JMocker._
import org.specs.mock.JMocker.{expect => expecting}
import org.hamcrest.BaseMatcher
import org.hamcrest.Matchers._
import org.junit.Before
import org.scalatest.junit.ShouldMatchersForJUnit
import org.junit.Test
import uk.ac.warwick.courses.TestBase

class ModuleServiceTest extends TestBase {
  
    var moduleService:ModuleService = null;
  
    @Before def before {
		moduleService = new ModuleService
    }
    
    @Test def moduleServiceImport {
         
         val dao = mock[DepartmentDao]
         expecting {
		      val chemistry = new Department
		      chemistry.code = "ch"
		      chemistry.name = "Chemistry"
		      
//		      val physics = new Department
//		      physics.code = "ph"
//		      physics.name = "Phygsics"
		      //physics.code must be matching("ph")
		      
		      //physics must have ('code("ph"))
		        
			  one(dao).save(departmentLike("ch","Chemistry"))
			  one(dao).save(departmentLike("ph","Physics"))
			  never(dao).save(departmentLike("in","IT Services"))
			  
			  allowing(dao).getDepartmentByCode(anyString)
			  will(returnValue(None))
		 }
         
		 moduleService.departmentFetcher = mockDepartmentFetcher
		 moduleService.departmentDao = dao
		 moduleService.importDepartments
	  
    }
    
    def departmentLike(code:String,name:String):Department = withArg(
			      hasProperty("code",equalTo(code)),
			      hasProperty("name",equalTo(name))
			  )
    
	def mockDepartmentFetcher = {
	  val fetcher = mock[DepartmentFetcher]
	  expecting {
	    one(fetcher).getDepartments
	    will(returnValue(List(
	        new DepartmentInfo("Physics","ph","Science"),
	        new DepartmentInfo("IT Services","in","Service/Admin"),
	        new DepartmentInfo("Chemistry","ch","Science")
	    )))
	  }
	  fetcher
	}
}