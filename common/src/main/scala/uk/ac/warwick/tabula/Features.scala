package uk.ac.warwick.tabula


import com.fasterxml.jackson.annotation.JsonAutoDetect
import javax.annotation.PostConstruct
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.util.queue.{Queue, QueueListener}

import scala.beans.BeanProperty

trait FeaturesComponent {
  def features: Features
}

trait AutowiringFeaturesComponent extends FeaturesComponent {
  var features: Features = Wire[Features]
}

/**
  * Defines flags to turn features on and off.
  *
  * Defaults set in this class.
  * App can change startup features in its `tabula.properties`,
  * then modify them at runtime via JMX.
  *
  * ==Adding a new feature==
  *
  * Define a new boolean variable here and FeaturesMessages, and then to set it to a different value in
  * `tabula.properties` add a line such as
  *
  * {{{
  * features.yourFeatureName=false
  * }}}
  */
abstract class Features {
  private val defaults = new FeaturesMessage

  // FIXME currently requires default to be set twice: in annotation for Spring, and in FeaturesMessage non-Spring tests

  @Value("${features.timetableFeedCurrentAcademicYear:true}") var timetableFeedCurrentAcademicYear: Boolean = defaults.timetableFeedCurrentAcademicYear
  @Value("${features.timetableIncludeLectureFeedWBS:false}") var timetableIncludeLectureFeedWBS: Boolean = defaults.timetableIncludeLectureFeedWBS
  @Value("${features.timetableIncludeMitCircsPanelEvents:false}") var timetableIncludeMitCircsPanelEvents: Boolean = defaults.timetableIncludeMitCircsPanelEvents

  @Value("${features.emailStudents:false}") var emailStudents: Boolean = defaults.emailStudents
  @Value("${features.collectRatings:true}") var collectRatings: Boolean = defaults.collectRatings
  @Value("${features.submissions:true}") var submissions: Boolean = defaults.submissions
  @Value("${features.privacyStatement:true}") var privacyStatement: Boolean = defaults.privacyStatement
  @Value("${features.collectMarks:true}") var collectMarks: Boolean = defaults.collectMarks
  @Value("${features.turnitin:true}") var turnitin: Boolean = defaults.turnitin
  @Value("${features.turnitinSubmissions:true}") var turnitinSubmissions: Boolean = defaults.turnitinSubmissions
  @Value("${features.turnitinTca:false}") var turnitinTca: Boolean = defaults.turnitinTca
  @Value("${features.assignmentMembership:true}") var assignmentMembership: Boolean = defaults.assignmentMembership
  @Value("${features.extensions:true}") var extensions: Boolean = defaults.extensions
  @Value("${features.feedbackTemplates:true}") var feedbackTemplates: Boolean = defaults.feedbackTemplates
  @Value("${features.markingWorkflows:true}") var markingWorkflows: Boolean = defaults.markingWorkflows
  @Value("${features.markerFeedback:true}") var markerFeedback: Boolean = defaults.markerFeedback
  @Value("${features.profiles:true}") var profiles: Boolean = defaults.profiles
  @Value("${features.assignmentProgressTable:true}") var assignmentProgressTable: Boolean = defaults.assignmentProgressTable
  @Value("${features.assignmentProgressTableByDefault:true}") var assignmentProgressTableByDefault: Boolean = defaults.assignmentProgressTableByDefault
  @Value("${features.summativeFilter:true}") var summativeFilter: Boolean = defaults.summativeFilter
  @Value("${features.dissertationFilter:true}") var dissertationFilter: Boolean = defaults.dissertationFilter
  @Value("${features.meetingRecordApproval:true}") var meetingRecordApproval: Boolean = defaults.meetingRecordApproval
  @Value("${features.smallGroupTeaching:true}") var smallGroupTeaching: Boolean = defaults.smallGroupTeaching
  @Value("${features.smallGroupTeaching.studentSignUp:true}") var smallGroupTeachingStudentSignUp: Boolean = defaults.smallGroupTeachingStudentSignUp
  @Value("${features.smallGroupTeaching.randomAllocation:false}") var smallGroupTeachingRandomAllocation: Boolean = defaults.smallGroupTeachingRandomAllocation
  @Value("${features.smallGroupTeaching.selfGroupSwitching:true}") var smallGroupTeachingSelfGroupSwitching: Boolean = defaults.smallGroupTeachingSelfGroupSwitching
  @Value("${features.smallGroupTeaching.tutorView:true}") var smallGroupTeachingTutorView: Boolean = defaults.smallGroupTeachingTutorView
  @Value("${features.smallGroupTeaching.spreadsheetImport:true}") var smallGroupTeachingSpreadsheetImport: Boolean = defaults.smallGroupTeachingSpreadsheetImport

  @Value("${features.smallGroupAllocationFiltering:true}") var smallGroupAllocationFiltering: Boolean = defaults.smallGroupAllocationFiltering
  @Value("${features.personalTutorAssignment:true}") var personalTutorAssignment: Boolean = defaults.personalTutorAssignment
  @Value("${features.personalTutorAssignmentFiltering:true}") var personalTutorAssignmentFiltering: Boolean = defaults.personalTutorAssignmentFiltering
  @Value("${features.arbitraryRelationships:true}") var arbitraryRelationships: Boolean = defaults.arbitraryRelationships
  @Value("${features.personalTimetables:true}") var personalTimetables: Boolean = defaults.personalTimetables
  @Value("${features.personalTimetables.exams:true}") var personalExamTimetables: Boolean = defaults.personalTimetables
  @Value("${features.profiles.memberNotes:true}") var profilesMemberNotes: Boolean = defaults.profilesMemberNotes
  @Value("${features.smallGroupTeaching.recordAttendance:true}") var smallGroupTeachingRecordAttendance: Boolean = defaults.smallGroupTeachingRecordAttendance

  @Value("${features.smallGroupTeaching.lectures:true}") var smallGroupTeachingLectures: Boolean = defaults.smallGroupTeachingLectures
  @Value("${features.profiles.coursework:true}") var courseworkInStudentProfile: Boolean = defaults.courseworkInStudentProfile
  @Value("${features.profiles.visa:true}") var visaInStudentProfile: Boolean = defaults.visaInStudentProfile

  @Value("${features.profiles.scheduledMeetings:true}") var scheduledMeetings: Boolean = defaults.scheduledMeetings
  @Value("${features.disability.rendering.profiles:true}") var disabilityRenderingInProfiles: Boolean = defaults.disabilityRenderingInProfiles
  @Value("${features.disability.rendering.extensions:true}") var disabilityRenderingInExtensions: Boolean = defaults.disabilityRenderingInExtensions
  @Value("${features.disability.rendering.submissions:true}") var disabilityOnSubmission: Boolean = defaults.disabilityOnSubmission
  @Value("${features.newSeenSecondMarkingWorkflows:true}") var newSeenSecondMarkingWorkflows: Boolean = defaults.newSeenSecondMarkingWorkflows
  @Value("${features.activityStreams:true}") var activityStreams: Boolean = defaults.activityStreams
  @Value("${features.profiles.showModuleResults:true}") var showModuleResults: Boolean = defaults.showModuleResults
  @Value("${features.profiles.showAccreditedPriorLearning:true}") var showAccreditedPriorLearning: Boolean = defaults.showAccreditedPriorLearning
  @Value("${features.attendanceMonitoring:true}") var attendanceMonitoring: Boolean = defaults.attendanceMonitoring
  @Value("${features.attendanceMonitoring.meetingPointType:true}") var attendanceMonitoringMeetingPointType: Boolean = defaults.attendanceMonitoringMeetingPointType
  @Value("${features.attendanceMonitoring.report:true}") var attendanceMonitoringReport: Boolean = defaults.attendanceMonitoringReport
  @Value("${features.attendanceMonitoring.report.realTime:true}") var attendanceMonitoringRealTimeReport: Boolean = defaults.attendanceMonitoringRealTimeReport
  @Value("${features.attendanceMonitoring.note:true}") var attendanceMonitoringNote: Boolean = defaults.attendanceMonitoringNote
  @Value("${features.attendanceMonitoring.smallGroupPointType:true}") var attendanceMonitoringSmallGroupPointType: Boolean = defaults.attendanceMonitoringSmallGroupPointType
  @Value("${features.attendanceMonitoring.assignmentSubmissionPointType:true}") var attendanceMonitoringAssignmentSubmissionPointType: Boolean = defaults.attendanceMonitoringAssignmentSubmissionPointType
  @Value("${features.smallGroupTeaching.crossModuleSmallGroups:true}") var smallGroupCrossModules: Boolean = defaults.smallGroupCrossModules
  @Value("${features.masqueradersCanWrite:false}") var masqueradersCanWrite: Boolean = defaults.masqueradersCanWrite
  @Value("${features.masqueradeElevatedPermissions:false}") var masqueradeElevatedPermissions: Boolean = defaults.masqueradeElevatedPermissions
  @Value("${features.masqueradeByQueryParam:false}") var masqueradeByQueryParam: Boolean = defaults.masqueradeByQueryParam
  @Value("${features.profiles.autoGroupDeregistration:false}") var autoGroupDeregistration: Boolean = defaults.autoGroupDeregistration
  @Value("${features.reports:true}") var reports: Boolean = defaults.reports
  @Value("${features.queueFeedbackForSits:true}") var queueFeedbackForSits: Boolean = defaults.queueFeedbackForSits
  @Value("${features.searchOnApiComponent:true}") var searchOnApiComponent: Boolean = defaults.searchOnApiComponent
  @Value("${features.celcatTimetablesChemistry:true}") var celcatTimetablesChemistry: Boolean = defaults.celcatTimetablesChemistry
  @Value("${features.celcatTimetablesWBS:true}") var celcatTimetablesWBS: Boolean = defaults.celcatTimetablesWBS
  @Value("${features.smallGroupTeaching.autoMarkMissedMonitoringPoints:true}") var autoMarkMissedMonitoringPoints: Boolean = defaults.autoMarkMissedMonitoringPoints
  @Value("${features.notificationListeners.mywarwick:true}") var myWarwickNotificationListener: Boolean = defaults.myWarwickNotificationListener

  @Value("${features.scheduling.academicInformationImport:true}") var schedulingAcademicInformationImport: Boolean = defaults.schedulingAcademicInformationImport
  @Value("${features.scheduling.profilesImport:true}") var schedulingProfilesImport: Boolean = defaults.schedulingProfilesImport
  @Value("${features.scheduling.assignmentsImport:true}") var schedulingAssignmentsImport: Boolean = defaults.schedulingAssignmentsImport
  @Value("${features.scheduling.cleanupTemporaryFiles:true}") var schedulingCleanupTemporaryFiles: Boolean = defaults.schedulingCleanupTemporaryFiles
  @Value("${features.scheduling.auditIndex:true}") var schedulingAuditIndex: Boolean = defaults.schedulingAuditIndex
  @Value("${features.scheduling.profilesIndex:true}") var schedulingProfilesIndex: Boolean = defaults.schedulingProfilesIndex
  @Value("${features.scheduling.notificationsIndex:true}") var schedulingNotificationsIndex: Boolean = defaults.schedulingNotificationsIndex
  @Value("${features.scheduling.processScheduledNotifications:true}") var schedulingProcessScheduledNotifications: Boolean = defaults.schedulingProcessScheduledNotifications
  @Value("${features.scheduling.notificationEmails:true}") var schedulingNotificationEmails: Boolean = defaults.schedulingNotificationEmails
  @Value("${features.scheduling.jobService:true}") var schedulingJobService: Boolean = defaults.schedulingJobService
  @Value("${features.scheduling.fileSync:true}") var schedulingFileSync: Boolean = defaults.schedulingFileSync
  @Value("${features.scheduling.cleanupUnreferencedFiles:true}") var schedulingCleanupUnreferencedFiles: Boolean = defaults.schedulingCleanupUnreferencedFiles
  @Value("${features.scheduling.sanityCheckFilesystem:true}") var schedulingSanityCheckFilesystem: Boolean = defaults.schedulingSanityCheckFilesystem
  @Value("${features.scheduling.exportAttendanceToSits:true}") var schedulingExportAttendanceToSits: Boolean = defaults.schedulingExportAttendanceToSits
  @Value("${features.scheduling.synchroniseAttendanceToSits:true}") var schedulingSynchroniseAttendanceToSits: Boolean = defaults.schedulingSynchroniseAttendanceToSits
  @Value("${features.scheduling.attendance.updateSchemes:true}") var schedulingAttendanceUpdateSchemes: Boolean = defaults.schedulingAttendanceUpdateSchemes
  @Value("${features.scheduling.attendance.updateTotals:true}") var schedulingAttendanceUpdateTotals: Boolean = defaults.schedulingAttendanceUpdateTotals
  @Value("${features.scheduling.exportFeedbackToSits:true}") var schedulingExportFeedbackToSits: Boolean = defaults.schedulingExportFeedbackToSits
  @Value("${features.scheduling.exportResitsToSits:true}") var schedulingExportResitsToSits: Boolean = defaults.schedulingExportResitsToSits
  @Value("${features.scheduling.exportRecordedModuleMarksToSits:true}") var schedulingExportRecordedModuleMarksToSits: Boolean = defaults.schedulingExportRecordedModuleMarksToSits
  @Value("${features.scheduling.triggers:true}") var schedulingTriggers: Boolean = defaults.schedulingTriggers
  @Value("${features.scheduling.objectStorageMigration:false}") var schedulingObjectStorageMigration: Boolean = defaults.schedulingObjectStorageMigration
  @Value("${features.scheduling.moduleListsImport:true}") var schedulingModuleListsImport: Boolean = defaults.schedulingModuleListsImport
  @Value("${features.scheduling.processNotificationListeners:true}") var schedulingProcessNotificationListeners: Boolean = defaults.schedulingProcessNotificationListeners
  @Value("${features.scheduling.monitoringPointMigration:false}") var schedulingMonitoringPointMigration: Boolean = defaults.schedulingMonitoringPointMigration
  @Value("${features.scheduling.groups.updateDepartmentSets:true}") var schedulingGroupsUpdateDepartmentSets: Boolean = defaults.schedulingGroupsUpdateDepartmentSets
  @Value("${features.scheduling.groups.removeAgedApplicantsJob:true}") var schedulingRemoveAgedApplicantsJob: Boolean = defaults.schedulingRemoveAgedApplicantsJob
  @Value("${features.scheduling.bulkModuleRegistrationsImport:true}") var schedulingBulkModuleRegistrationsImport: Boolean = defaults.schedulingBulkModuleRegistrationsImport
  @Value("${features.scheduling.combinedModuleMembershipDataImport:true}") var schedulingCombinedModuleMembershipDataImport: Boolean = defaults.schedulingCombinedModuleMembershipDataImport
  @Value("${features.scheduling.departmentMandatoryPermissionsWarning:false}") var schedulingDepartmentMandatoryPermissionsWarning: Boolean = defaults.schedulingDepartmentMandatoryPermissionsWarning
  @Value("${features.scheduling.bulkProgressionDecisionsImport:true}") var schedulingBulkProgressionDecisionsImport: Boolean = defaults.schedulingBulkProgressionDecisionsImport
  @Value("${features.scheduling.bulkStudentAwardsImport:true}") var schedulingBulkStudentAwardsImport: Boolean = defaults.schedulingBulkStudentAwardsImport
  @Value("${features.exams:true}") var exams: Boolean = defaults.exams
  @Value("${features.exams.grids:true}") var examGrids: Boolean = defaults.examGrids
  @Value("${features.exams.graduationBenchmark:true}") var graduationBenchmark: Boolean = defaults.graduationBenchmark

  @Value("${features.anonymousMarkingCM2:false}") var anonymousMarkingCM2: Boolean = defaults.anonymousMarkingCM2
  @Value("${features.openEndedReminderDateCM2:false}") var openEndedReminderDateCM2: Boolean = defaults.openEndedReminderDateCM2
  @Value("${features.moderationSelector:true}") var moderationSelector: Boolean = defaults.moderationSelector
  @Value("${features.bulkModeration:false}") var bulkModeration: Boolean = defaults.bulkModeration
  @Value("${features.profiles.searchPast:true}") var profilesSearchPast: Boolean = defaults.profilesSearchPast
  @Value("${features.skillsforge:true}") var skillsforge: Boolean = defaults.skillsforge
  @Value("${features.mitCircs:true}") var mitCircs: Boolean = defaults.mitCircs
  @Value("${features.mitCircs.covid19:true}") var mitcircsCovid19: Boolean = defaults.mitcircsCovid19
  @Value("${features.renderStackTracesForAllUsers:false}") var renderStackTracesForAllUsers: Boolean = defaults.renderStackTracesForAllUsers
  @Value("${features.enforceCsp:true}") var enforceCsp: Boolean = defaults.enforceCsp
  @Value("${features.notificationBatching:true}") var notificationBatching: Boolean = defaults.notificationBatching
  @Value("${features.marksManagement:true}") var marksManagement: Boolean = defaults.marksManagement
  @Value("${features.includeSMSForCurrentYear:true}") var includeSMSForCurrentYear: Boolean = defaults.includeSMSForCurrentYear
  @Value("${features.progressionDecisionsInStudentProfile:true}") var progressionDecisionsInStudentProfile: Boolean = defaults.progressionDecisionsInStudentProfile
  @Value("${features.processAgreedMarks:true}") var processAgreedMarks: Boolean = defaults.processAgreedMarks
  @Value("${features.ignoreResultRelease:false}") var ignoreResultRelease: Boolean = defaults.processAgreedMarks
  @Value("${features.recordBoardOutcomes:false}") var recordBoardOutcomes: Boolean = defaults.recordBoardOutcomes

  private val bean = new BeanWrapperImpl(this)

  def update(message: FeaturesMessage): Features = {
    val values = new BeanWrapperImpl(message)

    for (pd <- values.getPropertyDescriptors if bean.getPropertyDescriptor(pd.getName).getWriteMethod != null)
      bean.setPropertyValue(pd.getName, values.getPropertyValue(pd.getName))
    this
  }

  @PostConstruct def validate(): Unit = {
    require(attendanceMonitoringReport || !attendanceMonitoringRealTimeReport, "attendanceMonitoringReport must be enabled for attendanceMonitoringRealTimeReport")
  }
}

class FeaturesImpl extends Features

@ItemType("Features")
@JsonAutoDetect
class FeaturesMessage {
  // Warning: If you make this more complicated, you may break the Jackson auto-JSON stuff for the FeaturesController

  def this(features: Features) {
    this()

    val bean = new BeanWrapperImpl(this)
    val values = new BeanWrapperImpl(features)

    for (pd <- bean.getPropertyDescriptors if bean.getPropertyDescriptor(pd.getName).getWriteMethod != null)
      bean.setPropertyValue(pd.getName, values.getPropertyValue(pd.getName))
  }

  // BeanProperty current required as Utils JsonMessageConverter uses Jackson
  // without the Scala module.
  @BeanProperty var timetableFeedCurrentAcademicYear = true
  @BeanProperty var timetableIncludeLectureFeedWBS = false
  @BeanProperty var timetableIncludeMitCircsPanelEvents = false

  @BeanProperty var emailStudents = false
  @BeanProperty var activityStreams = true
  @BeanProperty var masqueradersCanWrite = false
  @BeanProperty var masqueradeElevatedPermissions = false
  @BeanProperty var masqueradeByQueryParam = false
  @BeanProperty var searchOnApiComponent = true

  @BeanProperty var collectRatings = true
  @BeanProperty var submissions = true
  @BeanProperty var privacyStatement = true
  @BeanProperty var collectMarks = true
  @BeanProperty var turnitin = true
  @BeanProperty var turnitinSubmissions = true
  @BeanProperty var turnitinTca = false
  @BeanProperty var assignmentMembership = true
  @BeanProperty var extensions = true
  @BeanProperty var feedbackTemplates = true
  @BeanProperty var markingWorkflows = true
  @BeanProperty var markerFeedback = true
  @BeanProperty var assignmentProgressTable = true
  @BeanProperty var assignmentProgressTableByDefault = true
  @BeanProperty var summativeFilter = true
  @BeanProperty var dissertationFilter = true
  @BeanProperty var disabilityRenderingInExtensions = true
  @BeanProperty var disabilityOnSubmission = true
  @BeanProperty var newSeenSecondMarkingWorkflows = true
  @BeanProperty var queueFeedbackForSits = true

  @BeanProperty var profiles = true
  @BeanProperty var meetingRecordApproval = true
  @BeanProperty var personalTutorAssignment = true
  @BeanProperty var personalTutorAssignmentFiltering = true
  @BeanProperty var arbitraryRelationships = true
  @BeanProperty var personalTimetables = true
  @BeanProperty var personalExamTimetables = true
  @BeanProperty var profilesMemberNotes = true
  @BeanProperty var courseworkInStudentProfile = true
  @BeanProperty var visaInStudentProfile = true
  @BeanProperty var scheduledMeetings = true
  @BeanProperty var disabilityRenderingInProfiles = true
  @BeanProperty var showModuleResults = true
  @BeanProperty var showAccreditedPriorLearning = true
  @BeanProperty var autoGroupDeregistration = false
  @BeanProperty var celcatTimetablesChemistry = true
  @BeanProperty var celcatTimetablesWBS = true
  @BeanProperty var myWarwickNotificationListener = true

  @BeanProperty var smallGroupTeaching = true
  @BeanProperty var smallGroupTeachingStudentSignUp = true
  @BeanProperty var smallGroupTeachingRandomAllocation = false
  @BeanProperty var smallGroupTeachingSelfGroupSwitching = true
  @BeanProperty var smallGroupTeachingTutorView = true
  @BeanProperty var smallGroupAllocationFiltering = true
  @BeanProperty var smallGroupTeachingRecordAttendance = true
  @BeanProperty var smallGroupTeachingLectures = true
  @BeanProperty var smallGroupCrossModules = true
  @BeanProperty var smallGroupTeachingSpreadsheetImport = true

  @BeanProperty var attendanceMonitoring = true
  @BeanProperty var attendanceMonitoringMeetingPointType = true
  @BeanProperty var attendanceMonitoringReport = true
  @BeanProperty var attendanceMonitoringRealTimeReport = true
  @BeanProperty var attendanceMonitoringNote = true
  @BeanProperty var attendanceMonitoringSmallGroupPointType = true
  @BeanProperty var attendanceMonitoringAssignmentSubmissionPointType = true

  @BeanProperty var autoMarkMissedMonitoringPoints = true

  @BeanProperty var schedulingAcademicInformationImport = true
  @BeanProperty var schedulingProfilesImport = true
  @BeanProperty var schedulingAssignmentsImport = true
  @BeanProperty var schedulingCleanupTemporaryFiles = true
  @BeanProperty var schedulingAuditIndex = true
  @BeanProperty var schedulingProfilesIndex = true
  @BeanProperty var schedulingNotificationsIndex = true
  @BeanProperty var schedulingProcessScheduledNotifications = true
  @BeanProperty var schedulingNotificationEmails = true
  @BeanProperty var schedulingJobService = true
  @BeanProperty var schedulingFileSync = true
  @BeanProperty var schedulingCleanupUnreferencedFiles = true
  @BeanProperty var schedulingSanityCheckFilesystem = true
  @BeanProperty var schedulingExportAttendanceToSits = true
  @BeanProperty var schedulingSynchroniseAttendanceToSits = true
  @BeanProperty var schedulingExportFeedbackToSits = true
  @BeanProperty var schedulingExportResitsToSits = true
  @BeanProperty var schedulingExportRecordedModuleMarksToSits = true
  @BeanProperty var schedulingAttendanceUpdateSchemes = true
  @BeanProperty var schedulingAttendanceUpdateTotals = true
  @BeanProperty var schedulingTriggers = true
  @BeanProperty var schedulingObjectStorageMigration = false
  @BeanProperty var schedulingModuleListsImport = true
  @BeanProperty var schedulingProcessNotificationListeners = true
  @BeanProperty var schedulingMonitoringPointMigration = false
  @BeanProperty var schedulingGroupsUpdateDepartmentSets = true
  @BeanProperty var schedulingRemoveAgedApplicantsJob = true
  @BeanProperty var schedulingBulkModuleRegistrationsImport = true
  @BeanProperty var schedulingCombinedModuleMembershipDataImport = true
  @BeanProperty var schedulingDepartmentMandatoryPermissionsWarning = false
  @BeanProperty var schedulingBulkProgressionDecisionsImport = true
  @BeanProperty var schedulingBulkStudentAwardsImport = true

  @BeanProperty var exams = true
  @BeanProperty var examGrids = true
  @BeanProperty var graduationBenchmark = true

  @BeanProperty var reports = true

  @BeanProperty var anonymousMarkingCM2 = false
  @BeanProperty var openEndedReminderDateCM2 = false
  @BeanProperty var moderationSelector = true
  @BeanProperty var bulkModeration = false
  @BeanProperty var profilesSearchPast = true
  @BeanProperty var skillsforge = true
  @BeanProperty var mitCircs = true
  @BeanProperty var mitcircsCovid19 = true
  @BeanProperty var renderStackTracesForAllUsers = false
  @BeanProperty var enforceCsp = true
  @BeanProperty var notificationBatching = true
  @BeanProperty var marksManagement = true
  @BeanProperty var includeSMSForCurrentYear = true
  @BeanProperty var progressionDecisionsInStudentProfile = true
  @BeanProperty var processAgreedMarks = true
  @BeanProperty var ignoreResultRelease = false
  @BeanProperty var recordBoardOutcomes = false

}

class FeatureFlagListener extends QueueListener with InitializingBean with Logging {

  var queue: Queue = Wire.named[Queue]("settingsSyncTopic")
  var features: Features = Wire.auto[Features]
  var context: String = Wire.property("${module.context}")

  override def isListeningToQueue = true

  override def onReceive(item: Any): Unit = {
    logger.info("Synchronising item " + item + " for " + context)
    item match {
      case copy: FeaturesMessage => features.update(copy)
      case _ => // Should never happen
    }
  }

  override def afterPropertiesSet(): Unit = {
    logger.info("Registering listener for " + classOf[FeaturesMessage].getAnnotation(classOf[ItemType]).value + " on " + context)
    queue.addListener(classOf[FeaturesMessage].getAnnotation(classOf[ItemType]).value, this)
  }

}

object Features {
  def empty = new FeaturesImpl
}
