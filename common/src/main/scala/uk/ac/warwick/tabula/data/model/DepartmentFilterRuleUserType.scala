package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.{StandardBasicTypes}
import uk.ac.warwick.tabula.data.model.Department.{CompositeFilterRule, AllMembersFilterRule, FilterRule}
import java.sql.Types

class DepartmentFilterRuleUserType extends AbstractBasicUserType[FilterRule, String] {

	val basicType = StandardBasicTypes.STRING

	override def sqlTypes = Array(Types.VARCHAR)

	val nullObject: FilterRule = AllMembersFilterRule
	val nullValue: String = ""

	// what to put in the DB when saving null
	def convertToObject(filterRuleName: String): FilterRule = {
		val rules: Option[FilterRule] = Option(filterRuleName).map(ruleNames =>
			ruleNames.split(",").toList match {
				case Nil => AllMembersFilterRule // don't think this can ever happen; split never returns Nil.
				case singleRule :: Nil => FilterRule.withName(singleRule)
				case rules: Seq[String] => CompositeFilterRule(rules map FilterRule.withName)
			}
		)
		rules.getOrElse(AllMembersFilterRule)

	}

	def convertToValue(obj: FilterRule): String = obj.name
}
