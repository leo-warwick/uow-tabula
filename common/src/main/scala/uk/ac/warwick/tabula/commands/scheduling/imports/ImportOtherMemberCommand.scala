package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.tabula.commands.{Description, Unaudited}
import uk.ac.warwick.tabula.data.{Daoisms, HibernateHelpers}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.services.scheduling.MembershipInformation
import uk.ac.warwick.userlookup.User

class ImportOtherMemberCommand(member: MembershipInformation, ssoUser: User)
  extends ImportMemberCommand(member, ssoUser, None)
    with ApplicantProperties with AutowiringProfileServiceComponent
    with Logging with Daoisms with Unaudited {

  this.mobileNumber = member.sitsApplicantInfo.map(_.mobileNumber).orNull
  this.nationality = member.sitsApplicantInfo.map(_.nationality).orNull
  this.secondNationality = member.sitsApplicantInfo.map(_.secondNationality).orNull
  member.sitsApplicantInfo.foreach(info => profileService.getDisability(info.disability).foreach(this.disability = _))

  def applyInternal(): Member = transactional() {
    val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)
    if (memberExisting.collect { case m @ (_: StudentMember | _: StaffMember) => m }.exists(_.missingFromImportSince == null)) {
      // Don't override existing students or staff
      memberExisting.get
    } else {
      logger.debug("Importing other member " + universityId + " into " + memberExisting)

      val (isTransient, member) = memberExisting.map(HibernateHelpers.initialiseAndUnproxy) match {
        case Some(m @ (_: StudentMember | _: StaffMember)) =>
          // TAB-692 delete the existing member, then return a brand new one
          // TAB-2188
          logger.info(s"Deleting $m while importing $universityId")
          memberDao.delete(m)
          (true, if (this.userType == MemberUserType.Applicant) new ApplicantMember(universityId) else new OtherMember(universityId))

        case Some(m) => (false, m)
        case _ if this.userType == MemberUserType.Applicant => (true, new ApplicantMember(universityId))
        case _ => (true, new OtherMember(universityId))
      }

      val commandBean = new BeanWrapperImpl(this)
      val memberBean = new BeanWrapperImpl(member)

      // We intentionally use single pipes rather than double here - we want all statements to be evaluated
      val hasChanged = member match {
        case _: ApplicantMember => copyMemberProperties(commandBean, memberBean) | copyApplicantProperties(commandBean, memberBean)
        case _ => copyMemberProperties(commandBean, memberBean)
      }

      if (isTransient || member.stale || hasChanged) {
        logger.debug(s"Saving changes for $member because ${if (isTransient) "it's a new object" else if (member.stale) "it's re-appeared in SITS" else "it's changed"}")

        member.missingFromImportSince = null
        member.lastUpdatedDate = DateTime.now
        memberDao.saveOrUpdate(member)
      }

      member
    }
  }

  private val basicApplicantProperties = Set(
    "nationality", "secondNationality", "mobileNumber", "disability"
  )

  private def copyApplicantProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
    copyBasicProperties(basicApplicantProperties, commandBean, memberBean)

  override def describe(d: Description): Unit =
    d.properties(
      "universityId" -> universityId,
      "category" -> userType.description
    )

  def phoneNumberPermissions = Nil

}
