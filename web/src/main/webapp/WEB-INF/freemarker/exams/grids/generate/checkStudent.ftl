<#import "*/modal_macros.ftlh" as modal />
<#escape x as x?html>
<#if errors?? && errors>
  <@modal.header>
    <h3 class="modal-title">
      <#if command.member??>
        Checking if ${command.member.fullName} should appear on this grid
      <#else>
        Check for missing students
      </#if>
    </h3>
  </@modal.header>
  <@modal.body>
    <@bs3form.errors path="command" />
  </@modal.body>
<#else>
  <#macro pass>
    <div class="alert alert-success"><i class="fa fa-check"></i>&nbsp;&nbsp;<#nested /></div></#macro>
  <#macro fail><#assign errror=true />
    <div class="alert alert-danger"><i class="fa fa-times"></i>&nbsp;&nbsp;<#nested /></div></#macro>
  <#assign errror=false />
  <@modal.header><h3 class="modal-title">Checking if ${command.student.fullName} should appear on this grid</h3></@modal.header>
  <@modal.body>
    <p>
      This student was last imported from SITS at <@fmt.date date=check.lastImportDate capitalise=false at=true relative=true />.
      If data has changed in SITS after this time, you'll need to generate the grid again to see the most recent information.
    </p>
    <#if !check.hasEnrolmentForYear>
      <@fail>This student doesn't appear to be enrolled for ${academicYear.toString}. The student may not have a course enrolment record for this year in SITS or they may have been marked as withdrawn before completing their course. </@fail>
    </#if>
    <#if !check.courseMatches>
      <@fail>This student isn't on a course that's been selected. The student is taking <#list command.student.freshStudentCourseDetails as scd>${scd.course.code}<#if scd_has_next> and </#if></#list>.</@fail>
    </#if>
    <#if !command.includeTempWithdrawn && check.temporarilyWithdrawn>
      <@fail>The student is marked as temporarily withdrawn for ${academicYear.toString}. You can re-generate this grid and choose to include temporarily withdrawn students.</@fail>
    </#if>
    <#if command.routes?has_content && !check.routeMatches>
      <@fail>This student isn't on a route that's been selected. The student is taking <#list command.student.freshStudentCourseDetails as scd>${scd.currentRoute.code}<#if scd_has_next> and </#if></#list>.</@fail>
    </#if>
    <#if command.resitOnly && !check.resitStudent>
      <@fail>This student doesn't have any resit marks. No Student Re-Assessment (SRA) records with marks exist in SITS. You can re-generate this grid and choose to show all students.</@fail>
    </#if>
    <#if !check.yearOrLevelMatches>
      <#if command.isLevelGrid()>
        <@fail>This student is not studying at level:${command.levelCode} for ${academicYear.toString}.</@fail>
      <#else>
        <@fail>This student is not on block ${command.yearOfStudy} for ${academicYear.toString}.</@fail>
      </#if>
    </#if>
    <#if !check.confirmedModuleRegs>
      <@fail>This student's module registrations have not been confirmed. The student may appear on this grid but their module marks will not be imported from SITS.</@fail>
    </#if>
    <#if !errror>
      <@pass>This student meets all the selection criteria for this grid and should appear.</@pass>
    </#if>
  </@modal.body>
</#if>
</#escape>
