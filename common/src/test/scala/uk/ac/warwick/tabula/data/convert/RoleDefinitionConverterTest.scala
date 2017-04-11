package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.Mockito
import org.hibernate.SessionFactory
import org.hibernate.Session
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition

class RoleDefinitionConverterTest extends TestBase with Mockito {

	val converter = new RoleDefinitionConverter

	val sessionFactory: SessionFactory = mock[SessionFactory]
	val session: Session = mock[Session]

	sessionFactory.getCurrentSession() returns (session)

	converter.sessionFactory = sessionFactory

	@Test def validBuiltInInput {
		converter.convertRight("DepartmentalAdministratorRoleDefinition") should be (DepartmentalAdministratorRoleDefinition)
	}

	@Test def validCustomInput {
		val definition = new CustomRoleDefinition
		definition.id = "steve"

		session.get(classOf[CustomRoleDefinition].getName(), "steve") returns (definition)

		converter.convertRight("steve") should be (definition)
	}

	@Test def invalidInput {
		session.get(classOf[CustomRoleDefinition].getName(), "20X6") returns (null)

		converter.convertRight("20X6") should be (null)
		converter.convertRight(null) should be (null)
	}

	@Test def formatting {
		val definition = new CustomRoleDefinition
		definition.id = "steve"

		converter.convertLeft(DepartmentalAdministratorRoleDefinition) should be ("DepartmentalAdministratorRoleDefinition")
		converter.convertLeft(definition) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}