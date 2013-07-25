package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.{AutowiringSmallGroupDaoComponent, SmallGroupDaoComponent, Daoisms}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._

trait SmallGroupServiceComponent {
	def smallGroupService: SmallGroupService
}

trait AutowiringSmallGroupServiceComponent extends SmallGroupServiceComponent {
	var smallGroupService = Wire[SmallGroupService]
}

trait SmallGroupService {
	def getSmallGroupSetById(id: String): Option[SmallGroupSet]
	def getSmallGroupById(id: String): Option[SmallGroup]
	def getSmallGroupEventById(id: String): Option[SmallGroupEvent]
	def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence]
	def saveOrUpdate(smallGroupSet: SmallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent)
	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent]
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup]

	def updateAttendance(smallGroupEvent: SmallGroupEvent, weekNumber: Int, usercodes: Seq[String]): SmallGroupEventOccurrence
	def getAttendees(event: SmallGroupEvent, weekNumber: Int): JList[String]
}

abstract class AbstractSmallGroupService extends SmallGroupService {
	self: SmallGroupDaoComponent =>

	val eventTutorsHelper = new UserGroupMembershipHelper[SmallGroupEvent]("tutors")
	val groupTutorsHelper = new UserGroupMembershipHelper[SmallGroup]("events.tutors")

	def getSmallGroupSetById(id: String) = smallGroupDao.getSmallGroupSetById(id)
	def getSmallGroupById(id: String) = smallGroupDao.getSmallGroupById(id)
	def getSmallGroupEventById(id: String) = smallGroupDao.getSmallGroupEventById(id)
	def getSmallGroupEventOccurrenceById(id: String) = smallGroupDao.getSmallGroupEventOccurrenceById(id)

	def saveOrUpdate(smallGroupSet: SmallGroupSet) = smallGroupDao.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup) = smallGroupDao.saveOrUpdate(smallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent) = smallGroupDao.saveOrUpdate(smallGroupEvent)

	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent] = eventTutorsHelper.findBy(user)
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup] = groupTutorsHelper.findBy(user)
	
	def getAttendees(event: SmallGroupEvent, weekNumber: Int): JList[String] = 
		smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber) match {
			case Some(occurrence) => occurrence.attendees.includeUsers
			case _ => JArrayList()
		}

	def updateAttendance(event: SmallGroupEvent, weekNumber: Int, usercodes: Seq[String]): SmallGroupEventOccurrence = {
		val occurrence = smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber) getOrElse {
			val newOccurrence = new SmallGroupEventOccurrence()
			newOccurrence.smallGroupEvent = event
			newOccurrence.week = weekNumber
			smallGroupDao.saveOrUpdate(newOccurrence)
			newOccurrence
		}

		occurrence.attendees.includeUsers.clear()
		occurrence.attendees.includeUsers.addAll(usercodes.asJava)
		occurrence
	}
}

@Service("smallGroupService")
class SmallGroupServiceImpl 
	extends AbstractSmallGroupService
		with AutowiringSmallGroupDaoComponent
		with Logging