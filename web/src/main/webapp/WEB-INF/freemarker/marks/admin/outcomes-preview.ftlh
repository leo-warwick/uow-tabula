<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />
<#import "/WEB-INF/freemarker/exams/grids/generate/form_fields.ftl" as gf />

<#assign courses><#list selectCourseCommand.courses as course >${course.code}<#if course_has_next>, </#if></#list></#assign>
<div class="deptheader">
  <h1>Record outcomes</h1>
  <h4 class="with-related">for ${courses} &ndash; <#if selectCourseCommand.levelCode??>Level ${selectCourseCommand.levelCode}<#else>Block ${selectCourseCommand.yearOfStudy}</#if></h4>
</div>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
<#if changes?size == 0>
  <p>There were no changes to decisions.</p>
  <p><a href="<@routes.marks.adminhome department academicYear />">Back to marks</a></p>
<#else>
  <p>The following decisions will be written to SITS pending approval by Student Records.</p>
  <p>Once these decisions are agreed, you will not be able to change them without the involvement of Student Records.</p>

  <#if notifications?size != 0>
    <div class="alert alert-info"><i class="fa fa-envelope"></i> Some decisions are being updated. Student records will be notified of these changes.</div>
  </#if>

  <div class="fix-area">
    <@f.form method="post" modelAttribute="examBoardOutcomesCommand" cssClass="marks-form">
      <@gf.select_course_fields />
      <@f.hidden name="confirm" value="true" />

      <table class="table table-striped table-sortable">
        <thead>
        <tr>
          <th class="col-sm-1 sortable" data-sortInitialOrder="asc" data-sorter="text">SPR Code</th>
          <th class="col-sm-1 sortable" data-sorter="text">First name</th>
          <th class="col-sm-1 sortable" data-sorter="text">Last name</th>
          <th class="col-sm-1 sortable" data-sorter="text">Period</th>
          <th class="col-sm-3">Decision</th>
          <th class="col-sm-5">Notes</th>
        </tr>
        </thead>
        <tbody>
          <#list changes?keys as sprCode>
            <#assign student = mapGet(changes, sprCode) />
            <@spring.bind path="students[${sprCode}]"><#assign c = status.actualValue /></@spring.bind>
            <tr>
              <td data-sortby="${sprCode}">
                <@f.hidden path="students[${sprCode}].record" />
                <@pl.profile_link student.universityId /> ${sprCode}
                <#if student.existing??>
                  <#assign tooltip>Student Records will be notified about these changes.</#assign>
                  <span class="tabula-tooltip" data-title="${tooltip}">
                    <i class="fa-fw fad fa-envelope" aria-hidden="true"></i>
                    <span class="sr-only">${tooltip}</span>
                  </span>
                </#if>
              </td>
              <td>${student.firstName}</td>
              <td>${student.lastName}</td>
              <td>
                <#if student.resitting>
                  <span class="label label-info">Summer vacation exam period</span>
                <#else>
                  <span class="label label-info">Summer exam period</span>
                </#if>
              </td>
              <td>
                <@f.hidden path="students[${sprCode}].decision" />
                  <#if student.finalist>
                    ${(student.existing.decision.finalistDescription)!'-'}
                    <#if (student.existing.decision.finalistDescription)! != c.decision.finalistDescription!>
                      <i class="fal fa-arrow-right"></i>
                      <#if c.decision?has_content>${c.decision.finalistDescription}</#if>
                    </#if>
                  <#else>
                    ${(student.existing.decision.description)!'-'}
                    <#if (student.existing.decision.description)! != c.decision.description!>
                      <i class="fal fa-arrow-right"></i>
                      <#if c.decision?has_content>${c.decision.description}</#if>
                    </#if>
                  </#if>
              </td>
              <td>
                <@f.input path="students[${sprCode}].notes" cssClass="form-control" readonly=true tabindex="-1" />
              </td>
            </tr>
          </#list>
        </tbody>
      </table>

      <div class="submit-buttons fix-footer">
        <input type="submit" class="btn btn-primary" value="Record" />
        <a class="btn btn-default dirty-check-ignore" href="">Cancel</a>
      </div>
    </@f.form>
  </div>
</#if>
