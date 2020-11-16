package uk.ac.warwick.tabula.data.model.notifications.marks

import uk.ac.warwick.tabula.data.model.{ModuleRegistration, ModuleSelectionStatus, Notification, RecordedModuleRegistration}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}

class ConfirmedModuleMarkChangedNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

  @Test def single(): Unit = withUser("cuscav") {
    val student = Fixtures.student(universityId = "8347243", courseDepartment = Fixtures.department("cs"))
    val scd = student.mostSignificantCourse
    val academicYear = scd.latestStudentCourseYearDetails.academicYear

    val module = Fixtures.module("in101")

    val moduleRegistration = new ModuleRegistration(scd.sprCode, module, BigDecimal(30).underlying, "IN101-30", academicYear, "A", "WMR")
    moduleRegistration._allStudentCourseDetails.add(scd)
    moduleRegistration.assessmentGroup = "A1"
    moduleRegistration.selectionStatus = ModuleSelectionStatus.OptionalCore

    val recordedModuleRegistration = new RecordedModuleRegistration(moduleRegistration)

    val department = Fixtures.department("in")

    val notification = Notification.init(new ConfirmedModuleMarkChangedNotification, currentUser.apparentUser, recordedModuleRegistration, department)
    notification.profileService = smartMock[ProfileService]
    notification.profileService.getStudentCourseDetailsBySprCode(scd.sprCode) returns Seq(scd)
    notification.topLevelUrl = "https://tabula.ac.uk"
    notification.title should be ("IN101-30: Confirmed module marks have been changed")

    val content = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be(
      s"""Confirmed module marks have been modified for 1 student. Follow the link below to view the student marks:
        |
        |* 8347243: https://tabula.ac.uk/exams/grids/cs/${academicYear.startYear}/8347243_1/assessmentdetails
        |""".stripMargin)
  }

  @Test def batched(): Unit = withUser("cuscav") {
    val student1 = Fixtures.student(universityId = "8347243", courseDepartment = Fixtures.department("cs"))
    val scd1 = student1.mostSignificantCourse

    val student2 = Fixtures.student(universityId = "8347246", courseDepartment = Fixtures.department("cs"))
    val scd2 = student2.mostSignificantCourse

    val academicYear = scd1.latestStudentCourseYearDetails.academicYear

    val module1 = Fixtures.module("in101")

    val moduleRegistration1 = new ModuleRegistration(scd1.sprCode, module1, BigDecimal(30).underlying, "IN101-30", academicYear, "A", "WMR")
    moduleRegistration1._allStudentCourseDetails.add(scd1)
    moduleRegistration1.assessmentGroup = "A1"
    moduleRegistration1.selectionStatus = ModuleSelectionStatus.OptionalCore

    val module2 = Fixtures.module("in102")

    val moduleRegistration2 = new ModuleRegistration(scd1.sprCode, module2, BigDecimal(30).underlying, "IN102-30", academicYear, "A", "WMR")
    moduleRegistration2._allStudentCourseDetails.add(scd1)
    moduleRegistration2.assessmentGroup = "A1"
    moduleRegistration2.selectionStatus = ModuleSelectionStatus.OptionalCore

    val moduleRegistration3 = new ModuleRegistration(scd2.sprCode, module2, BigDecimal(30).underlying, "IN102-30", academicYear, "A", "WMR")
    moduleRegistration3._allStudentCourseDetails.add(scd2)
    moduleRegistration3.assessmentGroup = "A1"
    moduleRegistration3.selectionStatus = ModuleSelectionStatus.OptionalCore

    val recordedModuleRegistration1 = new RecordedModuleRegistration(moduleRegistration1)
    val recordedModuleRegistration2 = new RecordedModuleRegistration(moduleRegistration2)
    val recordedModuleRegistration3 = new RecordedModuleRegistration(moduleRegistration3)

    val department = Fixtures.department("in")

    val notification1 = Notification.init(new ConfirmedModuleMarkChangedNotification, currentUser.apparentUser, recordedModuleRegistration1, department)
    notification1.profileService = smartMock[ProfileService]
    notification1.profileService.getStudentCourseDetailsBySprCode(scd1.sprCode) returns Seq(scd1)
    notification1.topLevelUrl = "https://tabula.ac.uk"

    val notification2 = Notification.init(new ConfirmedModuleMarkChangedNotification, currentUser.apparentUser, recordedModuleRegistration2, department)
    notification2.profileService = smartMock[ProfileService]
    notification2.profileService.getStudentCourseDetailsBySprCode(scd1.sprCode) returns Seq(scd1)
    notification2.topLevelUrl = "https://tabula.ac.uk"

    val notification3 = Notification.init(new ConfirmedModuleMarkChangedNotification, currentUser.apparentUser, recordedModuleRegistration3, department)
    notification3.profileService = smartMock[ProfileService]
    notification3.profileService.getStudentCourseDetailsBySprCode(scd2.sprCode) returns Seq(scd2)
    notification3.topLevelUrl = "https://tabula.ac.uk"

    val batch = Seq(notification1, notification2, notification3)
    ConfirmedModuleMarkChangedBatchedNotificationHandler.titleForBatch(batch, currentUser.apparentUser) should be ("Confirmed module marks have been changed")

    val content = ConfirmedModuleMarkChangedBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be(
      s"""Confirmed module marks have been modified for 3 students. Follow the links below to view the student marks:
        |
        |* IN101-30 - 8347243: https://tabula.ac.uk/exams/grids/cs/${academicYear.startYear}/8347243_1/assessmentdetails
        |* IN102-30 - 8347243: https://tabula.ac.uk/exams/grids/cs/${academicYear.startYear}/8347243_1/assessmentdetails
        |* IN102-30 - 8347246: https://tabula.ac.uk/exams/grids/cs/${academicYear.startYear}/8347246_1/assessmentdetails
        |""".stripMargin)
  }


  @Test def courseChange(): Unit = withUser("cuscav") {
    val dept = Fixtures.department("cs")
    val student1 = Fixtures.student(universityId = "8347243", courseDepartment = dept)
    val scd1 = student1.mostSignificantCourse
    scd1.mostSignificant = false

    // the student has moved onto a new course - the notification about the mark change relating to the previous course should still work
    Fixtures.studentCourseDetails(student1, dept, null, student1.universityId + "/2", academicYear = Some(AcademicYear.now().next))

    val academicYear = scd1.latestStudentCourseYearDetails.academicYear

    val module1 = Fixtures.module("in101")
    val moduleRegistration1 = new ModuleRegistration(scd1.sprCode, module1, BigDecimal(30).underlying, "IN101-30", academicYear, "A", "WMR")
    moduleRegistration1._allStudentCourseDetails.add(scd1)
    moduleRegistration1.assessmentGroup = "A1"
    moduleRegistration1.selectionStatus = ModuleSelectionStatus.OptionalCore
    val recordedModuleRegistration = new RecordedModuleRegistration(moduleRegistration1)

    val department = Fixtures.department("in")

    val notification = Notification.init(new ConfirmedModuleMarkChangedNotification, currentUser.apparentUser, recordedModuleRegistration, department)
    notification.profileService = smartMock[ProfileService]
    notification.profileService.getStudentCourseDetailsBySprCode(scd1.sprCode) returns student1.freshOrStaleStudentCourseDetails.toSeq
    notification.studentList.length should be (1)
  }
}
