package uk.ac.warwick.tabula.data

import javax.persistence.DiscriminatorValue

import org.hibernate.ObjectNotFoundException
import org.joda.time.{DateTime, DateTimeUtils}
import org.junit.{After, Before}
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.SubmissionReceivedNotification
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{ScheduledMeetingRecordInviteeNotification, ScheduledMeetingRecordNotification}
import uk.ac.warwick.tabula.data.model.permissions.RoleOverride
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, ModuleManagerRoleDefinition}
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.services.{SecurityService, UserLookupService}
import uk.ac.warwick.tabula.{Fixtures, Mockito, PackageScanner, PersistenceTestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

@Transactional
class NotificationDaoTest extends PersistenceTestBase with Mockito {

	val notificationDao = new NotificationDaoImpl

	val agentMember: Member = Fixtures.member(userType=MemberUserType.Staff)
	val agent: User = agentMember.asSsoUser

	val victim: User = Fixtures.user("heronVictim", "heronVictim")
	val heron = new Heron(victim)

	@Before
	def setup() {
		notificationDao.sessionFactory = sessionFactory
		SSOUserType.userLookup = smartMock[UserLookupService]
		SSOUserType.userLookup.getUserByUserId("heronVictim") returns victim
		// hbm2ddl generates a swathe of conflicting foreign key constraints for entity_id, so ignore for this test
		session.createSQLQuery("SET DATABASE REFERENTIAL INTEGRITY FALSE").executeUpdate()
	}

	@After
	def teardown() {
		SSOUserType.userLookup = null
		DateTimeUtils.setCurrentMillisSystem()
	}

	def newHeronNotification(agent: User, heron: Heron): HeronWarningNotification = {
		Notification.init(new HeronWarningNotification, agent, heron)
	}

	@Test def saveAndFetch() {

			val notification = newHeronNotification(agent, heron)

			session.save(heron)

			notificationDao.getById("heronWarningNotification") should be (None)
			notificationDao.save(notification)
			notificationDao.getById(notification.id) should be (Option(notification))

			session.flush()
			session.clear()

			val retrievedNotification = notificationDao.getById(notification.id).get.asInstanceOf[HeronWarningNotification]
			retrievedNotification.title should be ("You all need to know. Herons would love to kill you in your sleep")
			retrievedNotification.url should be ("/beware/herons")
			retrievedNotification.item.entity should be(heron)
			retrievedNotification.recipient should be (victim)
			retrievedNotification.content.template should be ("/WEB-INF/freemarker/notifications/i_really_hate_herons.ftl")

			session.clear()
			session.delete(heron)
			session.flush()

			// If an attached entityreference points to a now non-existent thing, we shouldn't explode
			// when loading the Notification but only when accessing the lazy entityreference.

			val notificationWithNoItem = try {
				 notificationDao.getById(notification.id).get.asInstanceOf[HeronWarningNotification]
			} catch {
				case e: ObjectNotFoundException =>
					fail("Shouldn't throw ObjectNotFoundException until entity reference is accessed")
			}

			// asserts that this type of exception is thrown
			intercept[ObjectNotFoundException] {
				notificationWithNoItem.item
			}
	}

	@Test
	def submissionReceived() {
		val submission = Fixtures.submission()
		val assignment = Fixtures.assignment("Fun")
		assignment.addSubmission(submission)

		val module = Fixtures.module("in101")
		val department = Fixtures.department("in")
		module.adminDepartment = department

		val permissionsService = mock[PermissionsService]
		module.permissionsService = permissionsService
		session.save(module)

		department.permissionsService = permissionsService
		session.save(department)

		assignment.module = module
		session.save(assignment)

		/** Had to construct this stream and then  extract assignment,module,dept from there to set expectations. If we set expectation
			* on assignemnet,dept,module objects directly they are somehow still null (expectation don't work). SubmissionReceivedNotification works on stream
			* elements now and it is possible for expectation to work properly they needd to be of same type - scala.collection.immutable.Stream.ConsWrapper
			*
			**/
		val assignmentWithParents = Fixtures.withParents(assignment)


		permissionsService.ensureUserGroupFor(assignmentWithParents.head, ModuleManagerRoleDefinition) returns UserGroup.ofUniversityIds
		permissionsService.ensureUserGroupFor(assignmentWithParents(1), DepartmentalAdministratorRoleDefinition) returns UserGroup.ofUniversityIds
		permissionsService.getAllGrantedRolesFor(assignmentWithParents.head) returns Nil
		permissionsService.getAllGrantedRolesFor(assignmentWithParents(1)) returns Nil
		permissionsService.getAllGrantedRolesFor(assignmentWithParents(2)) returns Nil
		permissionsService.getGrantedPermission(assignmentWithParents.head, Permissions.Submission.Delete, RoleOverride.Allow) returns None
		permissionsService.getGrantedPermission(assignmentWithParents(1), Permissions.Submission.Delete, RoleOverride.Allow) returns None
		permissionsService.getGrantedPermission(assignmentWithParents(2), Permissions.Submission.Delete, RoleOverride.Allow) returns None

		val notification = Notification.init(new SubmissionReceivedNotification, agent, submission, assignment)

		notification.securityService = mock[SecurityService]
		notification.permissionsService = permissionsService

		notificationDao.save(notification)
		notification.target.id should not be null
	}


	@Test
	def scheduledMeetings() {
		val staff = Fixtures.staff("1234567")
		val student = Fixtures.student("9876543")
		val relType = StudentRelationshipType("tutor", "tutor", "tutor", "tutor")

		val meeting = new ScheduledMeetingRecord
		meeting.creator = staff

		val relationship = StudentRelationship(staff, relType, student, DateTime.now)
		meeting.relationship = relationship

		session.save(staff)
		session.save(student)
		session.save(relType)
		session.save(relationship)
		session.save(meeting)

		val r: StudentRelationship = relationship

		val notification = Notification.init(new ScheduledMeetingRecordInviteeNotification, agent, Seq(meeting), r)
		notificationDao.save(notification)

		session.flush()
		session.clear()

		val retrieved = notificationDao.getById(notification.id).get.asInstanceOf[ScheduledMeetingRecordNotification]
		retrieved.meeting should be (meeting)
	}

	@Test def recent() {
		val agent = Fixtures.user()
		val group = Fixtures.smallGroup("Blissfully unaware group")
		session.save(group)
		val now = DateTime.now
		DateTimeUtils.setCurrentMillisFixed(now.getMillis)

		session.save(heron)

		for (i <- 1 to 1000) {
			val notification = newHeronNotification(agent, heron)
			notification.created = now.minusMinutes(i)
			notificationDao.save(notification)
		}

		session.flush()

		val everything = notificationDao.recent(now.minusMonths(10)).all.toSeq
		everything.size should be (1000)

		val oneHundred = notificationDao.recent(now.minusMonths(10)).take(100).toSeq
		oneHundred.size should be (100)

		def noNewer(mins: Int)(n: Notification[_,_]) = n.created.isBefore(now.minusMinutes(mins))

		val recent = notificationDao.recent(now.minusMonths(10)).takeWhile(noNewer(25)).toSeq
		recent.size should be (975)

	}

	/**
	 * Ensure there's nothing obviously wrong with the Notification subclass mappings. This will detect e.g.
	 * if an @Entity or @DiscriminatorValue are missing.
	 */
	@Test def nooneDied() {
		val notificationClasses = PackageScanner.subclassesOf[Notification[_,_]]("uk.ac.warwick.tabula.data.model")
		withClue("Package scanner should find a sensible number of classes") {
			notificationClasses.size should be > 5
		}

		val user = new User
		user.setUserId("testid")

		for (clazz <- notificationClasses) {
			try {
				val notification = clazz.getConstructor().newInstance().asInstanceOf[Notification[ToEntityReference,_]]
				notification.agent = user

				session.save(notification)

				if (clazz.getAnnotation(classOf[DiscriminatorValue]) == null) {
					fail(s"Notification $clazz has no @DiscriminatorValue annotation")
				}

				// FIXME we do want to flush because it would test things we care about, but many of the subclasses
				// expect specific properties to be set in order to save successfully. Need to do magic reflection to
				// work out what this is???
				//session.flush()
			} catch {
				case e: Exception => fail(s"Exception saving $clazz", e)
			}
		}
	}

	@Test def unemailedRecipients() {
		val agent = Fixtures.user()
		val group = Fixtures.smallGroup("Blissfully unaware group")
		session.save(group)
		val now = DateTime.now
		DateTimeUtils.setCurrentMillisFixed(now.getMillis)

		session.save(heron)

		val notifications = (1 to 50).map(i => {
			val notification = newHeronNotification(agent, heron)
			notification.created = now.minusMinutes(i)
			notificationDao.save(notification)
			notification
		})

		session.flush()

		notificationDao.unemailedRecipients.all.size should be (50)

		// attempt to send 10 mails - only 5 actually go
		for (
			(n, i) <- notifications.take(10).zipWithIndex;
			info <- n.recipientNotificationInfos.asScala
		) {
			if(i % 2 == 0)
				info.emailSent = true // half have been sent
			info.attemptedAt = DateTime.now
			notificationDao.save(n)
		}
		session.flush()

		// the half that didn't send won't show up in the unsent queue for another RETRY_DELAY_MINUTES - no point in retrying so soon
		notificationDao.unemailedRecipients.all.size should be (40)

		// go forward in time RETRY_DELAY_MINUTES +1  minutes - the 5 unsent mails show up again
		DateTimeUtils.setCurrentMillisFixed(now.plusMinutes(NotificationDao.RETRY_DELAY_MINUTES + 1).getMillis)
		notificationDao.unemailedRecipients.all.size should be (45)
	}
}
