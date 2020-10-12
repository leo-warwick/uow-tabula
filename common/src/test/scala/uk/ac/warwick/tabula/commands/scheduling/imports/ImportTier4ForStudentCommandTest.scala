package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentCourseYearDetails, StudentMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.{CasUsageImporter, Tier4VisaImporter}

class ImportTier4ForStudentCommandTest extends TestBase with Mockito with Logging {

  trait Environment {

    val studentMember: StudentMember = Fixtures.student()
    val year: AcademicYear = studentMember.mostSignificantCourseDetails.get.latestStudentCourseYearDetails.academicYear

  }

  @Transactional
  @Test def testVisaUpdatesAreApplied(): Unit = {
    new Environment {

      val casUsageImporter: CasUsageImporter = smartMock[CasUsageImporter]
      casUsageImporter.isCasUsed(studentMember.universityId) returns false

      val tier4VisaImporter: Tier4VisaImporter = smartMock[Tier4VisaImporter]
      tier4VisaImporter.hasTier4Visa(studentMember.universityId) returns false

      val command = ImportTier4ForStudentCommand(studentMember, year)
      command.casUsageImporter = casUsageImporter
      command.tier4VisaImporter = tier4VisaImporter
      command.studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
      command.applyInternal()

      studentMember.casUsed should be(Some(false))
      studentMember.hasTier4Visa should be(Some(false))
    }
  }


  @Transactional
  @Test def testVisaUpdatesApplyGoingForward(): Unit = {
    new Environment {
      val scd: StudentCourseDetails = studentMember.mostSignificantCourse;
      val currentScyd: StudentCourseYearDetails = scd.latestStudentCourseYearDetails
      currentScyd.yearOfStudy = 2
      val lastScyd: StudentCourseYearDetails = Fixtures.studentCourseYearDetails(AcademicYear.forDate(DateTime.now.minusYears(1)), null, 1, scd)
      scd.attachStudentCourseYearDetails(lastScyd);
      val nextScyd: StudentCourseYearDetails = Fixtures.studentCourseYearDetails(AcademicYear.forDate(DateTime.now.plusYears(1)), null, 3, scd)
      scd.attachStudentCourseYearDetails(nextScyd);

      val casUsageImporter: CasUsageImporter = smartMock[CasUsageImporter]
      casUsageImporter.isCasUsed(studentMember.universityId) returns true

      val tier4VisaImporter: Tier4VisaImporter = smartMock[Tier4VisaImporter]
      tier4VisaImporter.hasTier4Visa(studentMember.universityId) returns true

      val command = ImportTier4ForStudentCommand(studentMember, year)
      command.casUsageImporter = casUsageImporter
      command.tier4VisaImporter = tier4VisaImporter
      command.studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
      command.applyInternal()

      studentMember.casUsed should be(Some(true))
      studentMember.hasTier4Visa should be(Some(true))
      lastScyd.tier4Visa.booleanValue() should be(false)
      lastScyd.casUsed.booleanValue() should be(false)
      currentScyd.tier4Visa.booleanValue() should be(true)
      currentScyd.casUsed.booleanValue() should be(true)
      nextScyd.tier4Visa.booleanValue() should be(true)
      nextScyd.casUsed.booleanValue() should be(true)
    }
  }
}
