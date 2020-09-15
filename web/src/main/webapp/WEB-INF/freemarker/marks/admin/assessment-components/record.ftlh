<#-- @ftlvariable name="from_origin" type="java.lang.String" -->
<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />
<#import "/WEB-INF/freemarker/generated_grades_macros.ftlh" as gg />

<div class="deptheader">
  <h1>Record marks</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<p>Use this form to record <strong>unconfirmed actual</strong> component marks for students. If the assessment didn't take place, leave all marks and grades blank and apply a <a href="<@routes.marks.assessmentcomponent_missingMarks assessmentComponent upstreamAssessmentGroup />">missing marks adjustment</a>.</p>

<#if oldestImport??>
  <p>
    Marks are shown using the data available in SITS at <@fmt.date date=oldestImport capitalise=false at=true relative=true />.
    If data changes in SITS after this time, you'll need to refresh the SITS data to see the most recent information.
  </p>

  <p>
    <a href="<@routes.marks.assessmentcomponent_marks_forceImport assessmentComponent upstreamAssessmentGroup />" class="btn btn-default">
      Refresh SITS data
    </a>
  </p>
</#if>

<#assign submitUrl><@routes.marks.assessmentcomponent_marks assessmentComponent upstreamAssessmentGroup /></#assign>
<#assign generateUrl><@routes.marks.assessmentcomponent_generateGrades assessmentComponent /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
<div>
  <!-- Nav tabs -->
  <ul class="nav nav-tabs" role="tablist">
    <li role="presentation" <#if (from_origin!"webform") == "webform">class="active"</#if>><a href="#webform" aria-controls="webform" role="tab" data-toggle="tab">Web Form</a></li>
    <li role="presentation" <#if (from_origin!"webform") == "upload">class="active"</#if>><a href="#upload" aria-controls="upload" role="tab" data-toggle="tab">Upload</a></li>
  </ul>
  <!-- Tab panes -->
  <div class="tab-content">
    <div role="tabpanel" class="tab-pane <#if (from_origin!"webform") == "webform">active</#if>" id="webform">
      <div class="fix-area">
        <@f.form method="post" action=submitUrl?markup_string modelAttribute="command" cssClass="marks-form">
          <div class="has-error"><@f.errors path="" cssClass="help-block" /></div>
          <table class="table table-striped table-sortable">
            <thead>
              <tr>
                <th class="col-sm-1 sortable">University ID</th>
                <th class="col-sm-1 sortable">First name</th>
                <th class="col-sm-1 sortable">Last name</th>
                <th class="col-sm-1 sortable">Mark</th>
                <th class="col-sm-1 sortable" data-sorter="text">Grade</th>
                <th class="col-sm-2">New mark</th>
                <th class="col-sm-2">New grade</th>
                <th class="col-sm-3">Comments</th>
              </tr>
            </thead>
            <tbody>
              <#list studentMarkRecords as student>
                <#assign member = membersByUniversityId[student.universityId] />

                <tr>
                  <td data-sortby="u${student.universityId}">
                    <#if !student.outOfSync><@f.hidden path="students[${student.universityId}_${student.resitSequence!}].resitSequence" /></#if>
                    <@pl.profile_link member.universityId />
                    ${student.universityId}
                    <@components.componentMarkStudentStatus student />
                  </td>
                  <td>${member.firstName}</td>
                  <td>${member.lastName}</td>
                  <td data-sortby="${student.mark!-1}">
                    ${student.mark!'-'}
                    <@components.componentMarkStatusIcon student />
                    <@components.componentMarkHistory student assessmentComponent member />
                  </td>
                  <td data-sortby="g${student.grade!'-'}">
                    <@components.format_grade student.grade!'-' />
                  </td>
                  <td>
                    <#if !student.outOfSync>
                      <@bs3form.labelled_form_group path="students[${student.universityId}_${student.resitSequence!}].mark">
                        <div class="input-group">
                            <@f.input path="students[${student.universityId}_${student.resitSequence!}].mark" cssClass="form-control mark-box" autocomplete="off" type="number" min="0" max="100" />
                          <div class="input-group-addon">%</div>
                        </div>
                      </@bs3form.labelled_form_group>
                    </#if>
                  </td>
                  <td>
                    <#if !student.outOfSync>
                      <@spring.bind path="students[${student.universityId}_${student.resitSequence!}]">
                        <#assign studentMarkItem = status.actualValue />
                      </@spring.bind>
                      <@bs3form.labelled_form_group path="students[${student.universityId}_${student.resitSequence!}].grade">
                        <#if isGradeValidation>
                          <@f.input path="students[${student.universityId}_${student.resitSequence!}].grade" cssClass="form-control ${studentMarkItem.validGrades?has_content?string('auto-grade grades-already-available', 'auto-grade')} grade-box" data\-mark="students[${student.universityId}_${student.resitSequence!}].mark" data\-generate\-url=generateUrl?markup_string data\-resit\-attempt=(student.upstreamAssessmentGroupMember.currentResitAttempt)! />
                          <@f.select path="students[${student.universityId}_${student.resitSequence!}].grade" cssClass="form-control" cssStyle="display: none;" disabled=true>
                            <#if studentMarkItem.validGrades?has_content>
                              <@gg.gradeOptions studentMarkItem.validGrades._1() studentMarkItem.validGrades._2() studentMarkItem.grade!'' />
                            </#if>
                          </@f.select>
                        <#else>
                          <@f.input path="students[${student.universityId}_${student.resitSequence!}].grade" cssClass="form-control grade-box" />
                        </#if>
                      </@bs3form.labelled_form_group>
                    </#if>
                  </td>
                  <td>
                    <#if !student.outOfSync>
                      <@bs3form.labelled_form_group path="students[${student.universityId}_${student.resitSequence!}].comments">
                        <@f.input path="students[${student.universityId}_${student.resitSequence!}].comments" cssClass="form-control" />
                      </@bs3form.labelled_form_group>
                    </#if>
                  </td>
                </tr>
              </#list>
            </tbody>
          </table>

          <div class="submit-buttons fix-footer">
            <input type="submit" class="btn btn-primary" value="Save" />
            <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Cancel</a>
          </div>
        </@f.form>
      </div>
    </div>
    <div role="tabpanel" class="tab-pane <#if (from_origin!"webform") == "upload">active</#if>" id="upload">
      <p>You can upload marks and grades in a spreadsheet, which must be an XLSX file (i.e. created in Microsoft Office 2007 or later). The spreadsheet
        should have the following column headings: <strong>University ID</strong>, <strong>Mark</strong>,
        <strong>Grade</strong> and <strong>Comments</strong>.
        You can use this <a href="<@routes.marks.assessmentcomponent_marks_template assessmentComponent upstreamAssessmentGroup />">generated spreadsheet</a> as a template.</p>

      <p>Enter the corresponding results for each university ID in the following columns:</p>

      <ul>
        <li>Column B - Mark</li>
        <li>Column C - Grade</li>
        <li>Column D - Comments</li>
      </ul>

      <p>Note that you can upload any of the following: marks only; grades only (for pass/fail modules); marks and grades only; marks and comments only; marks, grades and comments.</p>

      <#if assessmentComponent.module.adminDepartment??>
        <#assign department = assessmentComponent.module.adminDepartment />
        <h6>
          Departmental settings for ${department.name}
          <#if can.do("Department.ManageDisplaySettings", department)>
            <a href="<@routes.admin.displaysettings department />">Change</a>
          </#if>
        </h6>

        <ul>
          <#if department.assignmentGradeValidation>
            <li>Grade validation is currently <strong>ON</strong> - invalid grades cannot be saved. A grade must be provided if a mark is entered.</li>

            <#if assessmentComponent.marksCode != 'PF'>
              <#if department.assignmentGradeValidationUseDefaultForZero>
                <li>The default grade for a mark of 0 is <strong>F</strong>. Please check grades carefully.</li>
              <#else>
                <li>A grade will <strong>NOT</strong> be pre-filled for a mark of 0. A valid grade must be provided.</li>
              </#if>
            </#if>
          <#else>
            <li>Grade validation is currently <strong>OFF</strong> - please make sure only valid grades are recorded.</li>
          </#if>
        </ul>
      </#if>

      <@f.form method="post" enctype="multipart/form-data" action=submitUrl?markup_string modelAttribute="command">
        <h3>Select file</h3>

        <@bs3form.labelled_form_group path="file" labelText="File">
          <input type="file" name="file.upload" />
        </@bs3form.labelled_form_group>

        <div class="buttons form-group">
          <button type="submit" class="btn btn-primary">Upload</button>
          <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Cancel</a>
        </div>
      </@f.form>
    </div>
  </div>
</div>
