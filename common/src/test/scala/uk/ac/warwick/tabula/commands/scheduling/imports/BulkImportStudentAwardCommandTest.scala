package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.data.AutowiringTransactionalComponent
import uk.ac.warwick.tabula.data.model.{Award, Classification, StudentAward}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling.{AutowiringStudentAwardImporterComponent, StudentAwardImporter, StudentAwardRow}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}


class BulkImportStudentAwardCommandTest extends TestBase with Mockito with Logging {

  trait Environment {
    val yr = 2017
    val year: AcademicYear = AcademicYear(yr)
    val years = Seq(year - 3, year - 2, year - 1, year)

    // create awards and classifications
    val existingAwardCode1 = "BSC"
    val existingAwardCode2 = "BA"
    val existingClassificationCode1 = "02"
    val existingClassificationCode2 = "03"

    val existingAward1: Award = Fixtures.award(existingAwardCode1, "Bachelor of Science (with Honours)")
    val existingAward2: Award = Fixtures.award(existingAwardCode2, "Bachelor of Arts (with Honours)")
    val existingClassification1: Option[Classification] = Some(Fixtures.classification(existingClassificationCode1, "Second Class, Upper Division"))
    val existingClassification2: Option[Classification] = Some(Fixtures.classification(existingClassificationCode2, "Second Class, Lower Division"))

    val studentAwardImporter = smartMock[StudentAwardImporter]
    val studentAwardService = smartMock[StudentAwardService]
    val awardService = smartMock[AwardService]
    val classificationService = smartMock[ClassificationService]

    val command = new BulkImportStudentAwardsCommandInternal
      with AutowiringStudentAwardImporterComponent
      with AutowiringStudentAwardServiceComponent
      with AutowiringAwardServiceComponent
      with AutowiringClassificationServiceComponent
      with AutowiringTransactionalComponent
      with BulkImportStudentAwardsForAcademicYearRequest {
      override val academicYear: AcademicYear = year
    }


    command.studentAwardImporter = studentAwardImporter
    command.studentAwardService = studentAwardService
    command.awardService = awardService
    command.classificationService = classificationService

    awardService.allAwards returns Seq(existingAward1, existingAward2)
    classificationService.allClassifications returns Seq(existingClassification1.get, existingClassification2.get)


  }


  @Test def testNewRowsBulkImportStudentAwardCommand(): Unit = {

    new Environment {
      val sAwardRow1 = StudentAwardRow("0123473/2", year, existingAwardCode1, None, Some(existingClassificationCode1))
      val sAwardRow2 = StudentAwardRow("0123474/2", year, existingAwardCode2, None, Some(existingClassificationCode2))
      studentAwardImporter.getStudentAwardRowsForAcademicYears(years) returns Seq(sAwardRow1, sAwardRow2)
      studentAwardService.getByAcademicYears(years) returns Seq()

      val result = command.applyInternal()
      result.created should be(2)
      result.deleted should be(0)
      result.updated should be(0)

    }

  }

  @Test def testNewAndUpdateExistingRowBulkImportStudentAwardCommand(): Unit = {
    new Environment {
      val sprCode1 = "0123473/2"
      val sprCode2 = "0123474/2"
      val sitsStudentAwardRow1 = StudentAwardRow(sprCode1, year, existingAwardCode1, None, Some(existingClassificationCode1))
      val sitsStudentAwardRow2 = StudentAwardRow(sprCode2, year, existingAwardCode2, None, Some(existingClassificationCode2))


      //existing award at tabula end set to  the different award
      val existingStudentAward2 = studentAward(sprCode2, existingAward2, existingClassification1, year)
      studentAwardImporter.getStudentAwardRowsForAcademicYears(years) returns Seq(sitsStudentAwardRow1, sitsStudentAwardRow2)
      studentAwardService.getByAcademicYears(years) returns Seq(existingStudentAward2)

      val result = command.applyInternal()
      result.created should be(1)
      result.deleted should be(0)
      result.updated should be(1)

    }
  }

  @Test def testWithNoChangesBulkImportStudentAwardCommand(): Unit = {
    new Environment {

      val sprCode1 = "0123473/2"
      val sprCode2 = "0123474/2"
      val sitsStudentAwardRow1 = StudentAwardRow(sprCode1, year, existingAwardCode1, None, Some(existingClassificationCode1))
      val sitsStudentAwardRow2 = StudentAwardRow(sprCode2, year, existingAwardCode2, None, Some(existingClassificationCode2))


      //existing award at tabula end same as SITS
      val existingStudentAward1 = studentAward(sprCode1, existingAward1, existingClassification1, year)
      val existingStudentAward2 = studentAward(sprCode2, existingAward2, existingClassification2, year)
      studentAwardImporter.getStudentAwardRowsForAcademicYears(years) returns Seq(sitsStudentAwardRow1, sitsStudentAwardRow2)
      studentAwardService.getByAcademicYears(years) returns Seq(existingStudentAward1, existingStudentAward2)

      val result = command.applyInternal()
      result.created should be(0)
      result.deleted should be(0)
      result.updated should be(0)

    }
  }


  @Test def testDeleteRowBulkImportStudentAwardCommand(): Unit = {
    new Environment {

      val sprCode1 = "0123473/2"
      val sprCode2 = "0123474/2"
      val sitsStudentAwardRow1 = StudentAwardRow(sprCode1, year, existingAwardCode1, None, Some(existingClassificationCode1))

      //existing award at tabula end same as SITS
      val existingStudentAward1 = studentAward(sprCode1, existingAward1, existingClassification1, year)
      val existingStudentAward2 = studentAward(sprCode2, existingAward2, existingClassification2, year)
      studentAwardImporter.getStudentAwardRowsForAcademicYears(years) returns Seq(sitsStudentAwardRow1)
      studentAwardService.getByAcademicYears(years) returns Seq(existingStudentAward1, existingStudentAward2)

      val result = command.applyInternal()
      result.created should be(0)
      result.deleted should be(1)
      result.updated should be(0)

    }
  }

  @Test def testDeleteWithUpdateRowBulkImportStudentAwardCommand(): Unit = {
    new Environment {
      val somePastDate = new DateTime(yr, DateTimeConstants.NOVEMBER, 15, 9, 18 ).toLocalDate

      val sprCode1 = "0123473/2"
      val sprCode2 = "0123474/2"
      val sitsStudentAwardRow1 = StudentAwardRow(sprCode1, year, existingAwardCode1, Some(somePastDate), Some(existingClassificationCode1))

      //existing award at tabula different than SITS (award date changed in SITS)
      val existingStudentAward1 = studentAward(sprCode1, existingAward1, existingClassification1, year)
      val existingStudentAward2 = studentAward(sprCode2, existingAward2, existingClassification2, year)
      studentAwardImporter.getStudentAwardRowsForAcademicYears(years) returns Seq(sitsStudentAwardRow1)
      studentAwardService.getByAcademicYears(years) returns Seq(existingStudentAward1, existingStudentAward2)

      val result = command.applyInternal()
      result.created should be(0)
      result.deleted should be(1)
      result.updated should be(1)

    }
  }
  def studentAward(sprCode: String, award: Award, classification: Option[Classification], academicYear: AcademicYear, awardDate: Option[LocalDate] = None): StudentAward = {
    val sa = new StudentAward()
    sa.sprCode = sprCode
    sa.award = award
    sa.classification = classification
    sa.academicYear = academicYear
    sa.awardDate = awardDate
    sa
  }

}
