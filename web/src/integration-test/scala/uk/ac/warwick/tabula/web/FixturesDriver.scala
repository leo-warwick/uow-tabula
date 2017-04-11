package uk.ac.warwick.tabula.web

import dispatch.classic._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.{FunctionalTestAcademicYear, FunctionalTestProperties, LoginDetails}

import scala.language.postfixOps
import scala.util.parsing.json.JSON

trait FixturesDriver extends SimpleHttpFetching {

	def updateExtensionSettings(departmentCode: String, allow: Boolean = true, managerUserId: String = ""): Unit = {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/update/extensionSettings"
		val req = url(uri).POST << Map("departmentCode" -> departmentCode, "allow" -> allow.toString, "userId" -> managerUserId)
		http.when(_==200)(req >| )
	}

	def createModule(departmentCode: String, code: String, name: String) {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/module"
		val req = url(uri).POST << Map("departmentCode" -> departmentCode, "code" -> code, "name" -> name)
		http.when(_==200)(req >| )
	}

	def createSmallGroupSet(
		moduleCode:String,
		groupSetName:String,
		groupCount:Int= 1,
		formatName:String="tutorial",
		allocationMethodName:String="Manual",
		openForSignups:Boolean = true,
		maxGroupSize:Int = 0,
		releasedToStudents:Boolean = true,
		allowSelfGroupSwitching:Boolean  = true,
		academicYear: String
	):String  = {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/groupset"
		val req = url(uri).POST << Map(
			"moduleCode" -> moduleCode,
			"groupSetName" -> groupSetName,
		  "formatName" -> formatName,
		  "allocationMethodName" -> allocationMethodName,
		  "groupCount" -> groupCount.toString,
		  "openForSignups" -> openForSignups.toString,
		  "releasedToStudents" -> releasedToStudents.toString,
		  "maxGroupSize" -> maxGroupSize.toString,
		  "allowSelfGroupSwitching" -> allowSelfGroupSwitching.toString,
			"academicYear" -> academicYear
		)
		val resp = http.when(_==200)(req as_str)

		val id = JSON.parseFull(resp).get.asInstanceOf[Map[String,Any]]("id").toString
		id
	}

	def createSmallGroupEvent(setId: String,title: String, weekRange:String="1") {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/groupEvent"
		val req = url(uri).POST << Map("setId" -> setId, "title" -> title, "weekRange"->weekRange)
		http.when(_==200)(req >| )
	}


  def addStudentToGroupSet(studentUserId: String, setId:String){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/groupsetMembership"
		val req = url(uri).POST << Map(
			"groupSetId" -> setId,
			"userId" -> studentUserId)
		http.when(_==200)(req >|)
	}

	def addStudentToGroup(studentUserId: String, setId:String, groupName:String){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/groupMembership"
		val req = url(uri).POST << Map(
			"groupSetId" -> setId,
			"userId" -> studentUserId,
		  "groupName"->groupName)
		http.when(_==200)(req >|)
	}

	def createStudentMember(
		userId:String,
		genderCode:String = "M",
		routeCode:String="",
		yearOfStudy:Int=1,
		courseCode:String="",
		deptCode:String="",
		academicYear:String = "2014"
	){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/studentMember"
		val req = url(uri).POST << Map(
			"userId" -> userId,
			"genderCode"->genderCode,
		  "yearOfStudy"->yearOfStudy.toString,
		  "routeCode"->routeCode,
		  "courseCode"->courseCode,
		  "deptCode"->deptCode,
		  "academicYear"->academicYear
		)
		http.when(_==200)(req >|)
	}

	def createStaffMember(userId:String, genderCode:String = "M", deptCode:String){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/staffMember"
		val req = url(uri).POST << Map(
			"userId" -> userId,
			"genderCode"->genderCode,
			"deptCode"->deptCode
		)
		http.when(_==200)(req >|)
	}

	def updateAssignment(deptCode:String, assignmentName:String, openDate:Option[DateTime] = None, closeDate:Option[DateTime] = None){
		val datesToUpdate:Seq[(String,String)] = Seq("openDate" -> openDate, "closeDate" -> closeDate).flatMap(t => t._2 match {
			case None => None
			case Some(d) => Some(t._1, d.toString("dd-MMM-yyyy HH:mm:ss"))
		})
		val params:Seq[(String,String)] = Seq("deptCode"->deptCode,"assignmentName"->assignmentName) ++ datesToUpdate
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/update/assignment"
		val req = url(uri).POST << Map(
			params :_*
		)
		http.when(_==200)(req >|)
	}

	def createExtension(userId: String, assignmentId: String, approved: Boolean) {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/extension"
		val req = url(uri).POST << Map(
			"userId" -> userId,
			"assignmentId" -> assignmentId,
			"approved" -> approved.toString)
		http.when(_==200)(req >|)
	}

	def createRoute(routeCode:String, departmentCode:String, routeName:String, degreeType:String="UG" ){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/route"
		val req = url(uri).POST << Map(
			"routeCode" -> routeCode,
			"departmentCode"->departmentCode,
		  "routeName"->routeName,
		  "degreeType"->degreeType)
		http.when(_==200)(req >|)
	}


	def createCourse(courseCode:String, courseName:String ){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/course"
		val req = url(uri).POST << Map(
			"courseCode" -> courseCode,
			"courseName"->courseName)
		http.when(_==200)(req >|)
	}

	def registerStudentsOnModule(students: Seq[LoginDetails], moduleCode: String, academicYear: Option[String] = None) {
		val uniIds = students.map(_.warwickId).mkString(",")
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/moduleRegistration"
		val req = url(uri).POST << Map(
			"universityIds" -> uniIds,
			"moduleCode" -> moduleCode,
			"academicYear" -> academicYear.getOrElse(FunctionalTestAcademicYear.currentSITS.startYear.toString))

		http.when(_==200)(req >|)
	}

	def createStudentRelationship(student:LoginDetails, agent:LoginDetails, relationshipType:String = "tutor"){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/relationship"
		val req = url(uri).POST << Map(
			"studentUniId" -> student.warwickId,
			"agent"->agent.warwickId,
		  "relationshipType"->relationshipType)

		http.when(_==200)(req >|)
	}

	def createMonitoringPointSet(routeCode:String, pointCount:Int, academicYear:String, yearOption:Option[Int]){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/monitoringPointSet"
		var args = Map(
			"routeCode" -> routeCode,
			"pointCount"-> pointCount.toString,
			"academicYear" -> academicYear
		)
		if (yearOption.isDefined)
			args = args ++ Map("year" -> yearOption.get.toString)

		val req = url(uri).POST << args
		http.when(_==200)(req >|)
	}

	def createAttendanceMonitoringScheme(deptCode:String, pointCount:Int, academicYear:String, warwickId:String ){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/attendanceMonitoringScheme"
		val args = Map(
			"deptCode" -> deptCode,
			"pointCount"-> pointCount.toString,
			"academicYear" -> academicYear,
			"warwickId" -> warwickId
		)

		val req = url(uri).POST << args
		http.when(_==200)(req >|)
	}

	def createAssessmentComponent(departmentCode: String, moduleCode: String, name: String, assessmentGroup: String = "A", sequence: String = "A01") {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/assessmentComponent"
		val req = url(uri).POST << Map(
			"moduleCode" -> moduleCode,
			"assessmentGroup" -> assessmentGroup,
			"sequence" -> sequence,
			"departmentCode" -> departmentCode,
			"name" -> name
		)
		http.when(_==200)(req >|)
	}

	def createUpstreamAssessmentGroup(moduleCode: String, universityIds: Seq[String], assessmentGroup: String = "A", occurrence: String = "A") {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/upstreamAssessmentGroup"

		val universityIdArgs: Map[String, String] = universityIds.zipWithIndex.map { case (universityId, index) =>
			s"universityIds[$index]" -> universityId
		}.toMap

		val args = Map(
			"moduleCode" -> moduleCode,
			"assessmentGroup" -> assessmentGroup,
			"occurrence" -> occurrence
		) ++ universityIdArgs

		val req = url(uri).POST << args
		http.when(_==200)(req >|)
	}

	def createPremarkedAssignment(moduleCode: String): Unit = {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/premarkedAssignment"
		val args = Map("moduleCode" -> moduleCode)
		val req = url(uri).POST << args
		http.when(_==200)(req >|)
	}

	def createMemberNote(memberId: String, creatorId: String, note: String, title: String = ""): Unit = {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/memberNote"
		val req = url(uri).POST << Map(
			"memberId" -> memberId,
			"creatorId" -> creatorId,
			"note" -> note,
			"title" -> title
		)
		http.when(_==200)(req >|)
	}

}