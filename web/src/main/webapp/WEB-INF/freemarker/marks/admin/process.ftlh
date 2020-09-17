<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />
<#import "/WEB-INF/freemarker/generated_grades_macros.ftlh" as gg />

<#assign courses><#list selectCourseCommand.courses as course >${course.code}<#if course_has_next>, </#if></#list></#assign>
<div class="deptheader">
  <h1>Process marks</h1>
  <h4 class="with-related">for ${courses} &ndash; <#if selectCourseCommand.levelCode??>Level ${selectCourseCommand.levelCode}<#else>Block ${selectCourseCommand.yearOfStudy}</#if></h4>
</div>

<p>
  Use this form to process and finalise module marks as <strong>agreed</strong> marks, for ratification by the Senate Sub-Group (SSG).
  Students can see their agreed mark, grade and result (but not comments) in their Tabula profile on the release day for their year and level of study.
  The agreed marks are also uploaded to SITS on the <a href="https://warwick.ac.uk/coronavirus/intranet/continuity/teaching/marksandexamboards/guidance/results/#coordinatedrelease" target="_blank">release day</a>,
  showing in Tabula as waiting to be written to SITS until then.
</p>

<p>
  By processing module marks, any component marks recorded will also be agreed. Check the box next to any student you
  wish to agree marks for.
</p>

<#assign submitUrl><@routes.marks.adminhome department academicYear /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

<div class="fix-area process-module-marks">
<#--    <@f.form method="post" action=submitUrl?markup_string modelAttribute="command" cssClass="marks-form">-->
      <div class="has-error"><@f.errors path="" cssClass="help-block" /></div>
      <table class="table table-striped table-sortable table-checkable">
        <thead>
        <tr>
          <th class="for-check-all" data-sorter="false"><input type="checkbox" class="collection-check-all" title="Select all/none" /></th>
          <th class="col-sm-1 sortable" data-sortInitialOrder="asc" data-sorter="text">SPR Code</th>
          <th class="col-sm-1 sortable" data-sorter="text">First name</th>
          <th class="col-sm-1 sortable" data-sorter="text">Last name</th>
          <th>Module results</th>
        </tr>
        </thead>
        <tbody>
          <#list students?keys as student>
            <#assign studentYearDetails = mapGet(students, student) />
            <#assign sprCode = studentYearDetails.studentCourseYearDetails.studentCourseDetails.sprCode />
            <tr>
              <td>
                <#-- <@f.checkbox cssClass="collection-checkbox" path="students[${sprCode}].process" /> --> <input type="checkbox" class="collection-checkbox" />
              </td>
              <td data-sortby="${sprCode}">
                  <@pl.profile_link student.universityId />
                  ${sprCode}
              </td>
              <td>${student.firstName}</td>
              <td>${student.lastName}</td>
              <td>
                <table class="table table-condensed">
                  <thead><tr><#list studentYearDetails.moduleRegistrations as mr>
                    <th>${mr.sitsModuleCode}</th>
                  </#list></tr></thead>
                  <tbody><tr>
                  <#list studentYearDetails.moduleRegistrations as mr>
                    <td><#if mr.firstDefinedMark??>${mr.firstDefinedMark}% &mdash; </#if>${mr.firstDefinedGrade!"-"}  &mdash; ${mr.moduleResult.description}</td>
                  </#list>
                </tr></tbody></table>
              </td>
            </tr>
          </#list>
        </tbody>
      </table>
      <div class="submit-buttons fix-footer">
<#--        <input type="submit" class="btn btn-primary" value="Save" />-->
        <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome department academicYear />">Cancel</a>
      </div>
<#--    </@f.form>-->
</div>

