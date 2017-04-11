package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Department.{InYearFilterRule, CompositeFilterRule, PostgraduateFilterRule, UndergraduateFilterRule}
import scala.util.{Failure, Try}

class DepartmentFilterRuleUserTypeTest extends TestBase {

	@Test
	def canSetFilterRuleFromCode(){
		val userType = new DepartmentFilterRuleUserType
		userType.convertToObject("UG") should be(UndergraduateFilterRule)
		userType.convertToObject("PG") should  be(PostgraduateFilterRule)
	}

	@Test
	def canStackFilters(){
		val userType = new DepartmentFilterRuleUserType
		userType.convertToObject("UG,Y2") should be(CompositeFilterRule(Seq(UndergraduateFilterRule,InYearFilterRule(2))))
	}

	@Test
	def invalidFilterCodeThrows(){
		val userType = new DepartmentFilterRuleUserType
		Try(userType.convertToObject("Fribble")) should be(anInstanceOf[Failure[Any]])
	}

	@Test
	def convertAndReconvertPreservesOrder(){
		val userType = new DepartmentFilterRuleUserType
		userType.convertToValue(userType.convertToObject("UG,Y2")) should be ("UG,Y2")
	}
}
