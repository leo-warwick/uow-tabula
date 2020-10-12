package uk.ac.warwick.tabula.web.controllers.profiles

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringRelationshipServiceComponent}
import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class ProfilesController extends BaseController with ProfileBreadcrumbs with CurrentMemberComponent
  with AutowiringProfileServiceComponent with AutowiringRelationshipServiceComponent {

  hideDeletedItems
  activeProfilesOnly
  studentProfilesOnly


  /**
    * Enables the Hibernate filter for this session to exclude
    * non-student entities.
    */
  private var _studentProfilesOnly = false

  def studentProfilesOnly: Unit = {
    _studentProfilesOnly = true
  }

  def notStudentProfilesOnly: Unit = {
    _studentProfilesOnly = false
  }

  private var _activeProfilesOnly = false

  def activeProfilesOnly: Unit = {
    _activeProfilesOnly = true
  }

  def notActiveProfilesOnly: Unit = {
    _activeProfilesOnly = false
  }

  final override def onPreRequest(): Unit = {
    // if studentsOnly has been called, activate the studentsOnly filter
    if (_studentProfilesOnly) {
      session.enableFilter(Member.StudentsOnlyFilter)
    }

    if (_activeProfilesOnly) {
      session.enableFilter(Member.ActiveOnlyFilter)
    }
  }

  final def optionalCurrentMember: Option[Member] = user.profile

  final def currentMember: Member = optionalCurrentMember getOrElse new RuntimeMember(user)

}

trait CurrentMemberComponent {
  def optionalCurrentMember: Option[Member]

  def currentMember: Member
}
