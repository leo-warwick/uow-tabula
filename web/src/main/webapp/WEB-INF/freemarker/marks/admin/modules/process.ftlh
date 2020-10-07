<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />
<#import "/WEB-INF/freemarker/generated_grades_macros.ftlh" as gg />


<div class="deptheader">
  <h1>Process module marks</h1>
  <h4 class="with-related">for ${sitsModuleCode} ${module.name} (${academicYear.toString}, ${occurrence})</h4>
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

<#assign submitUrl><@routes.marks.module_marks_process sitsModuleCode academicYear occurrence /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

<div class="fix-area process-module-marks">
  <@f.form method="post" action=submitUrl?markup_string modelAttribute="command" cssClass="marks-form">
    <div class="has-error"><@f.errors path="" cssClass="help-block" /></div>
    <table class="table table-striped table-sortable table-checkable">
      <thead>
        <tr>
          <th class="for-check-all" data-sorter="false"><input type="checkbox" class="collection-check-all" title="Select all/none" /></th>
          <th class="col-sm-1 sortable" data-sortInitialOrder="asc" data-sorter="text">SPR Code</th>
          <th class="col-sm-1 sortable" data-sorter="text">First name</th>
          <th class="col-sm-1 sortable" data-sorter="text">Last name</th>
          <th class="col-sm-1 sortable" data-sorter="text">State</th>
          <th class="col-sm-2 sortable" data-sorter="formfield">Mark</th>
          <th class="col-sm-1 sortable" data-sorter="formfield">Grade</th>
          <th class="col-sm-1 sortable" data-sorter="formfield">Result</th>
          <th class="col-sm-4 sortable" data-sorter="formfield">Comments</th>
        </tr>
      </thead>
      <tbody>
        <#list studentModuleMarkRecords as student>
          <#assign member = membersBySprCode[student.sprCode] />
          <tr>
            <td>
              <@f.checkbox cssClass="collection-checkbox" path="students[${student.sprCode}].process" />
            </td>
            <td data-sortby="${student.sprCode}">
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
              </#if>
            </td>
            <td>
              <@bs3form.labelled_form_group path="students[${student.sprCode}].mark">
                <div class="input-group">
                  <@f.input id="students${student.sprCode?replace('/', '_')}.mark" path="students[${student.sprCode}].mark" cssClass="form-control mark-box" autocomplete="off" type="number" min="0" max="100" />
                  <div class="input-group-addon">%</div>
                </div>
              </@bs3form.labelled_form_group>
            </td>
            <td>
              <@spring.bind path="students[${student.sprCode}]">
                <#assign studentModuleMarkItem = status.actualValue />
              </@spring.bind>
              <@bs3form.labelled_form_group path="students[${student.sprCode}].grade">
                <#assign generateUrl><@routes.marks.module_generateGrades sitsModuleCode academicYear occurrence student.sprCode /></#assign>
                <@f.input id="students${student.sprCode?replace('/', '_')}.grade-txt" path="students[${student.sprCode}].grade" cssClass="form-control ${studentModuleMarkItem.validGrades?has_content?string('auto-grade grades-already-available', 'auto-grade')} grade-box" data\-mark="students[${student.sprCode}].mark" data\-generate\-url=generateUrl?markup_string />
                <@f.select id="students${student.sprCode?replace('/', '_')}.grade" path="students[${student.sprCode}].grade" cssClass="form-control" cssStyle="display: none;" disabled=true>
                  <#if studentModuleMarkItem.validGrades?has_content>
                    <@gg.gradeOptions studentModuleMarkItem.validGrades._1() studentModuleMarkItem.validGrades._2() studentModuleMarkItem.grade!'' />
                  </#if>
                </@f.select>
              </@bs3form.labelled_form_group>
            </td>
            <td>
              <@bs3form.labelled_form_group path="students[${student.sprCode}].result">
                <@f.select id="students${student.sprCode?replace('/', '_')}.result" path="students[${student.sprCode}].result" cssClass="form-control">
                  <@f.option value="" />
                  <#list moduleResults as result>
                    <@f.option value=result.entryName label=result.description />
                  </#list>
                </@f.select>
              </@bs3form.labelled_form_group>
            </td>
            <td>
              <@bs3form.labelled_form_group path="students[${student.sprCode}].comments">
                <@f.input id="students${student.sprCode?replace('/', '_')}.comments" path="students[${student.sprCode}].comments" cssClass="form-control" />
              </@bs3form.labelled_form_group>
            </td>
          </tr>
        </#list>
      </tbody>
    </table>

    <div class="submit-buttons fix-footer">
      <input type="submit" class="btn btn-primary" value="Save" />
      <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome module.adminDepartment academicYear />">Cancel</a>
    </div>
  </@f.form>
</div>
