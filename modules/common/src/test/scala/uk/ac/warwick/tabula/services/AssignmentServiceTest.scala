package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.tabula.data.model.Assignment
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.UpstreamAssignment
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.MockUserLookup
import org.junit.Before

class AssignmentServiceTest extends AppContextTestBase {
	
	@Autowired var service:AssignmentServiceImpl =_
    @Autowired var modAndDeptService:ModuleAndDepartmentService =_
    var userLookup:MockUserLookup = _
    
    @Before def getUserLookup {
		// We can't just Autowire this because it has autowire-candidate="false"
		userLookup = beans.getBean("userLookupDelegate").asInstanceOf[MockUserLookup]
		userLookup.defaultFoundUser = true
	}
	
	@Transactional @Test def recentAssignment {
		val assignment = newDeepAssignment()
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		session.save(assignment)

		service.recentAssignment(department).get should be (assignment)
	}

	/**
	 * The Hibernate filter that adds deleted != 0
	 */
	@Transactional @Test def notDeletedFilter {
		val module = new Module
		session.save(module)
		val assignment = new Assignment
		assignment.name = "Essay"
		assignment.module = module
		assignment.academicYear = new AcademicYear(2009)
		assignment.markDeleted()
		assignment.addDefaultFields
		session.save(assignment)
		
		assignment.fields.get(1)
		
		service.isFilterEnabled("notDeleted") should be (false)
		service.getAssignmentById(assignment.id) should be (Some(assignment))
		session.enableFilter("notDeleted")
		service.getAssignmentById(assignment.id) should be (None)
		
		service.getAssignmentByNameYearModule(assignment.name, assignment.academicYear, assignment.module) should be ('empty)
	}
	
	@Transactional @Test def findDuplicateAssignmentNames {
		val module = new Module
		session.save(module)
		
		service.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should be ('empty)
		
		val assignment = new Assignment
		assignment.name = "Essay"
		assignment.module = module
		assignment.academicYear = new AcademicYear(2009)
		session.save(assignment)
		session.flush()
		
		service.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should not be ()
		service.getAssignmentByNameYearModule("Essay", new AcademicYear(2008), module) should be ('empty)
		service.getAssignmentByNameYearModule("Blessay", new AcademicYear(2009), module) should be ('empty)
	}
	
	@Transactional @Test def getAssignmentsByNameTest {    
	    val compSciDept = modAndDeptService.getDepartmentByCode("CS")
	    compSciDept should be ('defined)
	    
	    compSciDept.foreach(dept => {    
	        service.getAssignmentsByName("Test", dept) should have size(2)
            service.getAssignmentsByName("Computing", dept) should have size(1)	        
	        service.getAssignmentsByName("Assignment", dept) should have size(3) 
            service.getAssignmentsByName("xxxx", dept) should have size(0)	        
	    })
    }

	/*
	 * getUsersForFeedback gets all the users associated with an assignment who:
	 *     1. have feedback associated with that assignment which has not been released
	 *     2. have a submission associated with that assignment which is not suspected plagiarised.
	 */
	@Transactional @Test def getUsersForFeedbackTest {
		val assignment = service.getAssignmentById("1");
		assignment should be('defined)

		assignment.foreach { assmt =>
			// create a feedback for the assignment, not yet released
			val feedback = new Feedback
			feedback.universityId = "0070790"
			feedback.released = false
			assmt.addFeedback(feedback)
			session.save(feedback)
			
			// create a submission for the assignment, not plagiarised
			val submission = new Submission

			submission.setUniversityId("0070790")
			submission.suspectPlagiarised = false;
			assmt.addSubmission(submission)
			session.save(submission)
			
			// now check one user who needs to get feedback for this assignment is returned
			val userPairs = service.getUsersForFeedback(assmt)
			userPairs.size should be (1)

			// and check it's the right one
			for (userPair <- userPairs) {
				val studentId = userPair._1
				val user = userPair._2
				
				studentId should equal ("0070790")
				user.getWarwickId() should equal ("0070790")
			}
			
			// suppose the feedback was already released - would expect to get no users back
			feedback.released = true
			val userPairs2 = service.getUsersForFeedback(assmt)
			userPairs2.size should be (0)

			// feedback was not released - expect 1
			feedback.released = false
			val userPairs3 = service.getUsersForFeedback(assmt)
			userPairs3.size should be (1)
			
			// the only person was suspected of plagiarism - expect 0
			submission.suspectPlagiarised = true
			val userPairs4 = service.getUsersForFeedback(assmt)
			userPairs4.size should be (0)					
		}
		
	}
	
	@Transactional @Test def updateUpstreamAssignment {
		val upstream = new UpstreamAssignment
		upstream.departmentCode = "ch"
		upstream.moduleCode = "ch101"
		upstream.sequence = "A01"
		upstream.assessmentGroup = "A"
		upstream.name = "Egg plants"
		
		service.save(upstream)
		
		val upstream2 = new UpstreamAssignment
        upstream2.departmentCode = "ch"
        upstream2.moduleCode = "ch101"
        upstream2.sequence = "A01"
        upstream2.assessmentGroup = "A"
        upstream2.name = "Greg's plants"
		
        service.save(upstream2)
	}
	
	@Transactional @Test def findModulesWithFeedback {
		val ThisUser = 	"1234567"
		val OtherUser = "1234568"
		
		val myFeedback = new Feedback
		myFeedback.universityId = ThisUser
		myFeedback.released = true
		
		val otherFeedback = new Feedback
		otherFeedback.universityId = OtherUser
		otherFeedback.released = true
		
		val unreleasedFeedback = new Feedback
		unreleasedFeedback.universityId = ThisUser
			
		val deletedFeedback = new Feedback
		deletedFeedback.universityId = ThisUser
		deletedFeedback.released = true
		
		val assignment1 = new Assignment
		val assignment2 = new Assignment
		val assignment3 = new Assignment
		assignment3.markDeleted
		
		assignment1.addFeedback(myFeedback)
		assignment1.addFeedback(otherFeedback)
		assignment2.addFeedback(unreleasedFeedback)
		assignment3.addFeedback(deletedFeedback)
		
		session.save(assignment1)
		session.save(assignment2)
		session.save(assignment3)
		
		session.save(myFeedback)
		session.save(otherFeedback)
		session.save(unreleasedFeedback)
		session.save(deletedFeedback)
		
		session.enableFilter("notDeleted")
		
		val assignments = service.getAssignmentsWithFeedback(ThisUser)
		assignments.size should be (1)
		assignments(0) should be (assignment1)
	}
}