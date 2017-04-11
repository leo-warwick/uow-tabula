package uk.ac.warwick.tabula.commands.coursework
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.commands.coursework.assignments.AddAssignmentCommand
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.ModuleDao
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.data.model._

class AddAssignmentCommandContextTest extends AppContextTestBase {

	@Autowired var modules:ModuleDao =_

	@Transactional
	@Test def edit() {
		withUser("abc") {

		}
	}

	@Transactional
	@Test def add() {
		// hbm2ddl generates a swathe of conflicting foreign key constraints for entity_id, so ignore for this test
		session.createSQLQuery("SET DATABASE REFERENTIAL INTEGRITY FALSE").executeUpdate()

		withUser("abc") {

			val dept = new Department
			dept.code = "in"
			session.save(dept)

			val module = new Module
			module.adminDepartment = dept
			modules.saveOrUpdate(module)
			session.flush() // get out of my car, get into my database

			module.id should not be null

			val command = new AddAssignmentCommand(module)

			command.name = "Assignment name"
			command.comment = "Text at the top"
			val assignmentNew = command.apply()

			session.flush()
			session.clear()

			val assignment = session.get(classOf[Assignment], assignmentNew.id).asInstanceOf[Assignment]

			assignment.fields.size should be >= 2
			assignment.submissionFields.size should be (2)
			assignment.submissionFields.head should have('class(classOf[CommentField]))
			assignment.submissionFields.head.template should be ("comment")
			assignment.submissionFields.head.propertiesMap("value") should be("Text at the top")
			assignment.submissionFields.apply(1) should have('class(classOf[FileField]))
			assignment.submissionFields.apply(1).assignment should be(assignment)
			assignment.submissionFields.apply(1).position should be(1)
			assignment.submissionFields.apply(1).template should be("file")
		}
	}

  @Test def massAddUsers() {
    val form = new AddAssignmentCommand()
    form.massAddUsers =
      """ cusebr
          cusfal
          ecu
          0123456
          whatever yep good
      """
    form.massAddUsersEntries should be (Seq("cusebr","cusfal","ecu","0123456","whatever","yep","good"))
  }
}