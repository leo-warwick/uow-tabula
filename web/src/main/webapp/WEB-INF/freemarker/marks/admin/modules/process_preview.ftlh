<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />

<div class="deptheader">
  <h1>Process module marks</h1>
  <h4 class="with-related">for ${sitsModuleCode} ${module.name} (${academicYear.toString}, ${occurrence})</h4>
</div>

<#assign submitUrl><@routes.marks.module_marks_process sitsModuleCode academicYear occurrence /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
<#if changes?size == 0>
  <p>There were no changes to marks, grades or results.</p>

  <p><a href="<@routes.marks.adminhome module.adminDepartment academicYear />">Back to module marks</a></p>
<#else>
  <#if notificationDepartments?has_content>
    <div class="alert alert-info">
      <#list notificationDepartments?keys as dept>
        <#assign stuIds = mapGet(notificationDepartments, dept) />
        <p>Departmental administrators in <strong>${dept.name}</strong> will receive a notification that the mark has changed for <@fmt.p number=stuIds?size singular="student" shownumber=false/> <#list stuIds as stuId>${stuId}<#if stuId_has_next>, </#if></#list> when you click confirm.</p>
      </#list>
    </div>
  </#if>

  <p>The following marks will become agreed, which you will not be able to change without the involvement of the Exams Office:</p>

  <div class="fix-area">
    <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
      <@f.hidden name="confirm" value="true" />

      <table class="table table-striped table-sortable">
        <thead>
          <tr>
            <th class="col-sm-1 sortable" data-sortInitialOrder="asc" data-sorter="text">SPR Code</th>
            <th class="col-sm-1 sortable" data-sorter="text">First name</th>
            <th class="col-sm-1 sortable" data-sorter="text">Last name</th>
            <th class="col-sm-2" data-sorter="false">State</th>
            <th class="col-sm-1 sortable" data-sorter="text">Mark</th>
            <th class="col-sm-1 sortable" data-sorter="text">Grade</th>
            <th class="col-sm-1 sortable" data-sorter="text">Result</th>
            <th class="col-sm-4" data-sorter="false">Comment</th>
          </tr>
        </thead>
        <tbody>
          <#list changes as student>
            <#assign member = membersBySprCode[student.sprCode] />
            <@spring.bind path="students[${student.sprCode}]">
              <#assign c = status.actualValue />
            </@spring.bind>

            <tr>
              <td data-sortby="${student.sprCode}">
                <@f.hidden path="students[${student.sprCode}].process" />
                <@pl.profile_link member.universityId />
                ${student.sprCode}
                <@components.moduleMarkStatusIcon student />
                <@components.moduleMarkHistory student sitsModuleCode member.fullName />
              </td>
              <td>${member.firstName}</td>
              <td>${member.lastName}</td>
              <td>
                <#if student.markState?has_content>
                  <span class="label label-${student.markState.cssClass}">${student.markState.description}</span>
                <#else>
                  -
                </#if>

                <#if (student.markState.entryName)! != 'Agreed'>
                  <i class="fal fa-arrow-right"></i>
                  <span class="label label-success">Agreed</span>
                </#if>
              </td>
              <td data-sortby="${c.mark!-1}">
                <@f.hidden path="students[${student.sprCode}].mark" />
                ${student.mark!'-'}
                <#if "${student.mark!}" != c.mark!>
                  <i class="fal fa-arrow-right"></i>
                  ${c.mark!}
                </#if>
              </td>
              <td data-sortby="g${c.grade!}">
                <@f.hidden path="students[${student.sprCode}].grade" />
                <@components.format_grade student.grade!'-' />
                <#if student.grade! != c.grade!>
                  <i class="fal fa-arrow-right"></i>
                  <@components.format_grade c.grade! />
                </#if>
              </td>
              <td data-sortby="${c.result!}">
                <@f.hidden path="students[${student.sprCode}].result" />
                ${(student.result.description)!'-'}
                <#if (student.result.dbValue)! != c.result!>
                  <i class="fal fa-arrow-right"></i>
                  <#if c.result?has_content>
                    ${moduleResultsLookup[c.result].description}
                  </#if>
                </#if>
              </td>
              <td><@f.input path="students[${student.sprCode}].comments" cssClass="form-control" readonly=true tabindex="-1" /></td>
            </tr>
          </#list>
        </tbody>
      </table>

      <div class="submit-buttons fix-footer">
        <input type="submit" class="btn btn-primary" value="Process" />
        <a class="btn btn-default dirty-check-ignore" href="${returnTo}">Cancel</a>
      </div>
    </@f.form>
  </div>
</#if>
