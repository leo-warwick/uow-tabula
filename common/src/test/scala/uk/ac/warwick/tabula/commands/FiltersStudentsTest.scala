package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.{Fixtures, JavaImports, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{ProfileService, SecurityService}
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.JavaImports.JArrayList

class FiltersStudentsTest extends TestBase with Mockito {

  trait Fixture {
    val thisProfileService: ProfileService = mock[ProfileService]
    val thisSecurityService: SecurityService = mock[SecurityService]

    val dept: Department = Fixtures.department("arc", "School of Architecture")
    val subDept: Department = Fixtures.department("arc-ug", "Architecture Undergraduates", isImportDepartment = false)
    subDept.parent = dept
    dept.children.add(subDept)

    val mod1: Module = Fixtures.module("ac101", "Introduction to Architecture")
    val mod2: Module = Fixtures.module("ac102", "Architecture Basics")
    val mod3: Module = Fixtures.module("ac901", "Postgraduate Thesis")

    dept.modules.add(mod3)
    subDept.modules.add(mod2)
    subDept.modules.add(mod1)

    val route1: Route = Fixtures.route("a501", "Architecture BA")
    val route2: Route = Fixtures.route("a502", "Architecture BA with Intercalated year")
    val route3: Route = Fixtures.route("a9p1", "Architecture MA")

    dept.routes.add(route3)
    subDept.routes.add(route2)
    subDept.routes.add(route1)

    val sprF: SitsStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
    val sprP: SitsStatus = Fixtures.sitsStatus("P", "Permanently Withdrawn", "Permanently Withdrawn")

    val moaFT: ModeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")
    val moaPT: ModeOfAttendance = Fixtures.modeOfAttendance("P", "PT", "Part time")
  }

  @Test
  def initState(): Unit = {
    new Fixture {
      thisProfileService.allSprStatuses(dept) returns Seq(sprF, sprP)
      thisProfileService.allModesOfAttendance(dept) returns Seq(moaFT, moaPT)

      val filter = new FiltersStudents {
        val department: Department = dept
        val courseTypes: JavaImports.JList[CourseType] = JArrayList()
        val specificCourseTypes: JavaImports.JList[SpecificCourseType] = JArrayList()
        val modesOfAttendance: JavaImports.JList[ModeOfAttendance] = JArrayList()
        val defaultOrder: Seq[Order] = Seq()
        val sprStatuses: JavaImports.JList[SitsStatus] = JArrayList()
        val sortOrder: JavaImports.JList[Order] = JArrayList()
        val profileService: ProfileService = thisProfileService
        val securityService: SecurityService = thisSecurityService
        val yearsOfStudy: JavaImports.JList[JavaImports.JInteger] = JArrayList()
        val levelCodes: JavaImports.JList[String] = JArrayList()
        val studyLevelCodes: JavaImports.JList[String] = JArrayList()
        val modules: JavaImports.JList[Module] = JArrayList()
        val routes: JavaImports.JList[Route] = JArrayList()
        val courses: JavaImports.JList[Course] = JArrayList()
        var hallsOfResidence: JavaImports.JList[String] = JArrayList()
        var postcodes: JavaImports.JList[String] = JArrayList()
        val includeTier4Filters: Boolean = false
      }

      filter.allCourseTypes should be(CourseType.all)
      filter.allModesOfAttendance should be(Seq(moaFT, moaPT))
      filter.allModules should be(Seq(mod1, mod2, mod3)) // Order should be right through implicit ordering
      filter.allRoutes should be(Seq(route1, route2, route3)) // Order should be right through implicit ordering
      filter.allSprStatuses should be(Seq(sprF, sprP))
      filter.allYearsOfStudy should be(1 to FilterStudentsOrRelationships.MaxYearsOfStudy)
    }
  }

  @Test
  def serializeFilter(): Unit = {
    new Fixture {
      thisProfileService.allSprStatuses(dept) returns Seq(sprF, sprP)
      thisProfileService.allModesOfAttendance(dept) returns Seq(moaFT, moaPT)

      val filter = new FiltersStudents {
        val department: Department = dept
        val courseTypes: JavaImports.JList[CourseType] = JArrayList(CourseType.UG)
        val specificCourseTypes: JavaImports.JList[SpecificCourseType] = JArrayList(SpecificCourseType.UGD)
        val modesOfAttendance: JavaImports.JList[ModeOfAttendance] = JArrayList(moaFT)
        val defaultOrder: Seq[Order] = Seq()
        val sprStatuses: JavaImports.JList[SitsStatus] = JArrayList(sprF)
        val sortOrder: JavaImports.JList[Order] = JArrayList()
        val profileService: ProfileService = thisProfileService
        val securityService: SecurityService = thisSecurityService
        val yearsOfStudy: JavaImports.JList[JavaImports.JInteger] = JArrayList(1)
        val modules: JavaImports.JList[Module] = JArrayList(mod1)
        val routes: JavaImports.JList[Route] = JArrayList(route1, route2)
        val courses: JavaImports.JList[Course] = JArrayList()
        val levelCodes: JavaImports.JList[String] = JArrayList()
        val studyLevelCodes: JavaImports.JList[String] = JArrayList()
        var hallsOfResidence: JavaImports.JList[String] = JArrayList()
        var postcodes: JavaImports.JList[String] = JArrayList()
        val includeTier4Filters: Boolean = false
      }

      val serialized: String = filter.serializeFilter
      serialized.contains("courseTypes=" + CourseType.UG.value) should be(true)
      serialized.contains("courseTypes=" + CourseType.PGT.value) should be(false)
      serialized.contains("specificCourseTypes=" + SpecificCourseType.UGD.value) should be(true)
      serialized.contains("specificCourseTypes=" + SpecificCourseType.UG.value) should be(false)
      serialized.contains("modesOfAttendance=" + moaFT.code) should be(true)
      serialized.contains("modesOfAttendance=" + moaPT.code) should be(false)
      serialized.contains("sprStatuses=" + sprF.code) should be(true)
      serialized.contains("sprStatuses=" + sprP.code) should be(false)
      serialized.contains("yearsOfStudy=" + 1) should be(true)
      serialized.contains("yearsOfStudy=" + 2) should be(false)
      serialized.contains("modules=" + mod1.code) should be(true)
      serialized.contains("modules=" + mod2.code) should be(false)
      serialized.contains("routes=" + route1.code) should be(true)
      serialized.contains("routes=" + route2.code) should be(true)
      serialized.contains("routes=" + route3.code) should be(false)
    }
  }

  @Test
  def serializeFilterEmpty(): Unit = {
    new Fixture {
      thisProfileService.allSprStatuses(dept) returns Seq(sprF, sprP)
      thisProfileService.allModesOfAttendance(dept) returns Seq(moaFT, moaPT)

      val filter = new FiltersStudents {
        val department: Department = dept
        val courseTypes: JavaImports.JList[CourseType] = JArrayList()
        val specificCourseTypes: JavaImports.JList[SpecificCourseType] = JArrayList()
        val modesOfAttendance: JavaImports.JList[ModeOfAttendance] = JArrayList()
        val defaultOrder: Seq[Order] = Seq()
        val sprStatuses: JavaImports.JList[SitsStatus] = JArrayList()
        val sortOrder: JavaImports.JList[Order] = JArrayList()
        val profileService: ProfileService = thisProfileService
        val securityService: SecurityService = thisSecurityService
        val yearsOfStudy: JavaImports.JList[JavaImports.JInteger] = JArrayList()
        val modules: JavaImports.JList[Module] = JArrayList()
        val routes: JavaImports.JList[Route] = JArrayList()
        val courses: JavaImports.JList[Course] = JArrayList()
        val levelCodes: JavaImports.JList[String] = JArrayList()
        val studyLevelCodes: JavaImports.JList[String] = JArrayList()
        var hallsOfResidence: JavaImports.JList[String] = JArrayList()
        var postcodes: JavaImports.JList[String] = JArrayList()
        val includeTier4Filters: Boolean = false
      }

      val serialized: String = filter.serializeFilter
      serialized should not be null
      serialized should be("")
    }
  }

}
