package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.Transactions._
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.SprCode
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.scheduling.services.ProfileImporter
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.DepartmentDao

class ImportProfilesCommand extends Command[Unit] with Logging with Daoisms {
	
	PermissionCheck(Permissions.ImportSystemData)

	var departmentDao = Wire.auto[DepartmentDao]
	var profileImporter = Wire.auto[ProfileImporter]
	var profileService = Wire.auto[ProfileService]
	var userLookup = Wire.auto[UserLookupService]
	var features = Wire.auto[Features]

	def applyInternal() {
		if (features.profiles) {
			benchmark("ImportMembers") {
				doMemberDetails
			}
		}
	}

	/** Import basic info about all members in ADS, batched 250 at a time (small batch size is mostly for web sign-on's benefit) */
	def doMemberDetails {
		benchmark("Import all member details") {
			for {
				department <- departmentDao.allDepartments;
				userIdsAndCategories <- logSize(profileImporter.userIdsAndCategories(department)).grouped(250)
			} {
				logger.info("Fetching user details for " + userIdsAndCategories.size + " usercodes from websignon")
				val users: Map[String, User] = userLookup.getUsersByUserIds(userIdsAndCategories.map(x => x.member.usercode)).toMap
				
				logger.info("Fetching member details for " + userIdsAndCategories.size + " members from ADS")

				transactional() {
					profileImporter.getMemberDetails(userIdsAndCategories, users) map { _.apply }
					
					session.flush
					session.clear
				}
			}
		}
	}
	
	def refresh(member: Member) {	  
		transactional() {		
			val usercode = member.userId
			val user = userLookup.getUserByUserId(usercode)
			
			val userIdAndCategory = profileImporter.userIdAndCategory(member)
			
			println(userIdAndCategory)
		
			val members = profileImporter.getMemberDetails(List(userIdAndCategory), Map(usercode -> user)) map { _.apply }
			
			println(members)
			
			session.flush
			for (member <- members) session.evict(member)
		}
	}
	
	def equal(s1: Seq[String], s2: Seq[String]) =
		s1.length == s2.length && s1.sorted == s2.sorted

	def describe(d: Description) {

	}

}