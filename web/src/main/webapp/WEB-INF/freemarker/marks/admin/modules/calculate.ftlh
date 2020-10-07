<#-- @ftlvariable name="from_origin" type="java.lang.String" -->
<#-- @ftlvariable name="calculation" type="uk.ac.warwick.tabula.commands.marks.CalculateModuleMarksCommand.StudentModuleMarksItem" -->
<#-- @ftlvariable name="assessmentComponents" type="java.util.List<uk.ac.warwick.tabula.data.model.AssessmentComponent>" -->

<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />
<#import "/WEB-INF/freemarker/generated_grades_macros.ftlh" as gg />

<div class="deptheader">
  <h1>Calculate module marks</h1>
  <h4 class="with-related">for ${sitsModuleCode} ${module.name} (${academicYear.toString}, ${occurrence})</h4>
</div>

<p>
  Use this form to calculate and record <strong>unconfirmed actual</strong> module marks for students. Check the box next to any student you wish to record module marks for.
  <#if has_mma_calculation(studentModuleMarkRecords)>
    Because there are Force Majeure assessment components, you will need to perform a missing marks adjustment.
  </#if>
</p>

<#if oldestImport??>
  <p>
    Marks are shown using the data available in SITS at <@fmt.date date=oldestImport capitalise=false at=true relative=true />.
    If data changes in SITS after this time, you'll need to refresh the SITS data to see the most recent information.
  </p>

  <p>
    <a href="<@routes.marks.module_marks_forceImport sitsModuleCode academicYear occurrence />" class="btn btn-default">
      Refresh SITS data
    </a>
  </p>
</#if>

<#macro calculationCells calculation student member expand_failure_message=false>
  <#if calculation.successful>
    <#if calculation.multiple>
      <td colspan="3">
        <button type="button" class="btn btn-default btn-xs" data-toggle="modal" data-target="#mma-modal-${student.sprCode?replace('/', '_')}"><i class="fal fa-ballot-check" aria-hidden="true"></i> Missing mark adjustment</button>

        <@modal.modal id="mma-modal-${student.sprCode?replace('/', '_')}">
          <@modal.wrapper cssClass="modal-lg">
            <@modal.header>
              <h3 class="modal-title">Missing mark adjustment for ${member.fullName}, ${student.sprCode}</h3>
            </@modal.header>
            <@modal.body>
              ${calculation.reason}

              <p>Please select from one of the ${calculation.suggestions?size} possible calculations:</p>

              <#local colours = ['success', 'info', 'warning'] />
              <#list calculation.suggestions as suggestion>
                <#if !suggestion.calculation.multiple>
                  <div class="panel panel-${colours[suggestion_index % 3]} no-wide-tables">
                    <div class="panel-heading">
                      <h3 class="panel-title">${suggestion.title}</h3>
                    </div>
                    <div class="panel-body">
                      ${suggestion.description}
                    </div>
                    <table class="table">
                      <thead>
                        <tr>
                          <th class="col-xs-4">Mark</th>
                          <th class="col-xs-4">Grade</th>
                          <th class="col-xs-4">Result</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <@calculationCells suggestion.calculation student member true />
                        </tr>
                      </tbody>
                    </table>
                    <#if suggestion.calculation.successful>
                      <div class="panel-footer clearfix" id="mma-modal-${student.sprCode?replace('/', '_')}-suggestion-${suggestion_index}">
                        <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.calculate" value="true" />
                        <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.mark" value="${suggestion.calculation.mark!}" />
                        <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.grade" value="${suggestion.calculation.grade!}" />
                        <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.grade-txt" value="${suggestion.calculation.grade!}" />
                        <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.result" value="${(suggestion.calculation.result.dbValue)!}" />
                        <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.comments" value="${suggestion.calculation.comments!}" />
                        <button type="button" class="btn btn-primary pull-right" data-toggle="copy-values" data-target="#mma-modal-${student.sprCode?replace('/', '_')}-suggestion-${suggestion_index}" data-dismiss="modal">Use this calculation <i class="fas fa-arrow-right" aria-hidden="true"></i></button>
                      </div>
                    </#if>
                  </div>
                </#if>
              </#list>
            </@modal.body>
            <@modal.footer>
              <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
            </@modal.footer>
          </@modal.wrapper>
        </@modal.modal>
      </td>
    <#else>
      <td data-sortby="${calculation.mark!-1}">
        <#if (student.mark!-1) != (calculation.mark!-1)>
          ${student.mark!'-'}
          <i class="fal fa-arrow-right"></i>
        </#if>
        ${calculation.mark!'-'}
      </td>
      <td data-sortby="g${calculation.grade!}">
        <#if student.grade! != calculation.grade!>
          <@components.format_grade student.grade!'-' />
          <i class="fal fa-arrow-right"></i>
        </#if>
        <@components.format_grade calculation.grade!'-' />
      </td>
      <td data-sortby="${(calculation.result.dbValue)!}">
        <#if (student.result.description)! != (calculation.result.description)!>
          ${(student.result.description)!'-'}
          <i class="fal fa-arrow-right"></i>
        </#if>
        ${(calculation.result.description)!'-'}
      </td>
    </#if>
  <#else>
    <#assign failureMark><#compress><#if calculation.mark??>
      <#if (student.mark!-1) != (calculation.mark!-1)>
          ${student.mark!'-'}
          <i class="fal fa-arrow-right"></i>
        </#if>
        ${calculation.mark!'-'}
    </#if></#compress></#assign>
    <td colspan="3">
      <#if expand_failure_message>
        <i class="fa-fw fad fa-exclamation-triangle" aria-hidden="true"></i>
        ${calculation.message}
        ${failureMark}
      <#else>
        <span tabindex="0" class="tabula-tooltip" data-title="${calculation.message}">
          <i class="fa-fw fad fa-exclamation-triangle" aria-hidden="true"></i>
          <span class="sr-only">${calculation.message}</span>
        </span>
        ${failureMark}
      </#if>
    </td>
  </#if>
</#macro>

<#macro studentMarkRecordRow studentMarkRecord>
  <#local student = studentMarkRecord._1() />
  <#local componentMarks = studentMarkRecord._2() />
  <#local calculation = studentMarkRecord._4() />
  <#local member = membersBySprCode[student.sprCode] />

  <tr>
    <td>
      <@f.checkbox id="students${student.sprCode?replace('/', '_')}.calculate" cssClass="collection-checkbox" path="students[${student.sprCode}].calculate" />
    </td>
    <td data-sortby="${student.sprCode}">
      <@pl.profile_link member.universityId />
      ${student.sprCode}
      <@components.moduleMarkStatusIcon student />
      <@components.moduleMarkHistory student sitsModuleCode member.fullName />
    </td>
    <td>${member.firstName}</td>
    <td>${member.lastName}</td>

    <#list assessmentComponents as ac>
      <#local marksAndWeighting = (mapGet(componentMarks, ac))! />
      <#if marksAndWeighting?has_content>
        <#local marks = marksAndWeighting._1() />
        <#local weighting = (marksAndWeighting._2())! />
        <td data-sortby="${marks.mark!-1}">
          ${marks.mark!'-'}

          <#if marks.reassessment && weighting?has_content && ac.scaledWeighting?has_content && weighting?string["0.#"] != ac.scaledWeighting?string["0.#"]>
            <span tabindex="0" class="badge tabula-tooltip" data-title="${weighting?string["0.#"]}% re-assessment weighting">${weighting?string["0.#"]}%</span>
          <#elseif marks.mark?has_content && weighting?has_content && ac.scaledWeighting?has_content && weighting?string["0.#"] != ac.scaledWeighting?string["0.#"]>
            <span tabindex="0" class="badge tabula-tooltip" data-title="Weighting was adjusted to ${weighting?string["0.#"]}% by variable weighting rules">${weighting?string["0.#"]}%</span>
          </#if>

          <@components.componentMarkStatusIcon marks />
          <@components.componentMarkHistory marks ac member />
        </td>
        <td data-sortby="g${marks.grade!'-'}"><@components.format_grade marks.grade!'-' /></td>
      <#else>
        <td colspan="2"><span class="very-subtle">N/A</span></td>
      </#if>
    </#list>

    <@calculationCells calculation student member />

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
        <#local studentModuleMarkItem = status.actualValue />
      </@spring.bind>
      <@bs3form.labelled_form_group path="students[${student.sprCode}].grade">
        <#if isGradeValidation>
          <#local generateUrl><@routes.marks.module_generateGrades sitsModuleCode academicYear occurrence student.sprCode /></#local>
          <@f.input id="students${student.sprCode?replace('/', '_')}.grade-txt" path="students[${student.sprCode}].grade" cssClass="form-control ${studentModuleMarkItem.validGrades?has_content?string('auto-grade grades-already-available', 'auto-grade')} grade-box" data\-mark="students[${student.sprCode}].mark" data\-generate\-url=generateUrl?markup_string />
          <@f.select id="students${student.sprCode?replace('/', '_')}.grade" path="students[${student.sprCode}].grade" cssClass="form-control" cssStyle="display: none;" disabled=true>
              <#if studentModuleMarkItem.validGrades?has_content>
                  <@gg.gradeOptions studentModuleMarkItem.validGrades._1() studentModuleMarkItem.validGrades._2() studentModuleMarkItem.grade!'' />
              </#if>
          </@f.select>
        <#else>
          <@f.input id="students${student.sprCode?replace('/', '_')}.grade" path="students[${student.sprCode}].grade" cssClass="form-control grade-box" />
        </#if>
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
</#macro>

<#function has_mma_calculation studentModuleMarkRecords>
  <#list studentModuleMarkRecords as studentMarkRecord>
    <#local calculation = studentMarkRecord._4() />
    <#if calculation.successful && calculation.multiple>
      <#return true />
    </#if>
  </#list>
  <#return false />
</#function>

<#function get_mma_records studentModuleMarkRecords>
  <#local result = [] />
  <#list studentModuleMarkRecords as studentMarkRecord>
    <#local calculation = studentMarkRecord._4() />
    <#if calculation.successful && calculation.multiple>
      <#local result = result + [studentMarkRecord] />
    </#if>
  </#list>
  <#return result />
</#function>

<#function get_base_mma_calculation studentModuleMarkRecords>
  <#local baseCalculation = {} />
  <#list studentModuleMarkRecords as studentMarkRecord>
    <#local calculation = studentMarkRecord._4() />
    <#if calculation.successful && calculation.multiple>
      <#if !baseCalculation?has_content>
        <#local baseCalculation = calculation />
      <#else>
        <#local baseCalculationSize = baseCalculation.suggestions?filter(s -> s.calculation.successful)?size />
        <#local candidateCalculationSize = calculation.suggestions?filter(s -> s.calculation.successful)?size />
        <#if candidateCalculationSize gt baseCalculationSize>
          <#local baseCalculation = calculation />
        </#if>
      </#if>
    </#if>
  </#list>
  <#return baseCalculation />
</#function>

<#assign submitUrl><@routes.marks.module_marks sitsModuleCode academicYear occurrence /></#assign>

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
          <table class="table table-striped table-sortable table-checkable">
            <colgroup class="student">
              <col class="check-all-checkbox" />
              <col class="student-info" />
              <col class="student-info" />
              <col class="student-info" />
            </colgroup>
            <#list assessmentComponents as ac>
              <colgroup class="assessment-component">
                <col class="assessment-component-mark" />
                <col class="assessment-component-grade" />
              </colgroup>
            </#list>
            <colgroup class="module-mark-calculation">
              <col class="calculated-mark" />
              <col class="calculated-grade" />
              <col class="calculated-result" />
            </colgroup>
            <colgroup class="form-fields">
              <col class="form-mark" />
              <col class="form-grade" />
              <col class="form-result" />
              <col class="form-comments" />
            </colgroup>

            <thead>
              <tr>
                <th class="col-sm-1 for-check-all" rowspan="2" data-sorter="false"><input type="checkbox" class="collection-check-all" title="Select all/none" /></th>
                <th class="col-sm-1 sortable" rowspan="2" data-sortInitialOrder="asc" data-sorter="text">SPR Code</th>
                <th class="col-sm-1 sortable" rowspan="2" data-sorter="text">First name</th>
                <th class="col-sm-1 sortable" rowspan="2" data-sorter="text">Last name</th>

                <#list assessmentComponents as ac>
                  <th colspan="2">${ac.name} (${ac.sequence}, ${(ac.scaledWeighting!0)?string["0.#"]}%)</th>
                </#list>

                <th colspan="3">
                  Calculation
                  <#if has_mma_calculation(studentModuleMarkRecords)>
                    <#assign mmaModuleMarkRecords = get_mma_records(studentModuleMarkRecords) />
                    <#assign baseCalculation = get_base_mma_calculation(mmaModuleMarkRecords) />

                    <button type="button" class="btn btn-default btn-xs" data-toggle="modal" data-target="#mma-modal-all"><i class="fal fa-ballot-check" aria-hidden="true"></i> Missing mark adjustment (all students)</button>

                    <@modal.modal id="mma-modal-all">
                      <@modal.wrapper cssClass="modal-lg">
                        <@modal.header>
                          <h3 class="modal-title">Missing mark adjustment for all students on ${sitsModuleCode} ${module.name} (${academicYear.toString}, ${occurrence})</h3>
                        </@modal.header>
                        <@modal.body>
                          ${baseCalculation.reason}

                          <p>You can change the calculation for one or more individual students after you've chosen a default calculation for all students.</p>

                          <p>Please select from one of the ${baseCalculation.suggestions?size} possible calculations:</p>

                          <#assign colours = ['success', 'info', 'warning'] />
                          <#list baseCalculation.suggestions as suggestion>
                            <#if suggestion.calculation.successful && !suggestion.calculation.multiple>
                              <div class="panel panel-${colours[suggestion_index % 3]} no-wide-tables">
                                <div class="panel-heading">
                                  <h3 class="panel-title">${suggestion.title}</h3>
                                </div>
                                <div class="panel-body">
                                  ${suggestion.description}
                                </div>
                                <div class="panel-footer clearfix" id="mma-modal-all-suggestion-${suggestion_index}">
                                  <#list mmaModuleMarkRecords as studentMarkRecord>
                                    <#assign student = studentMarkRecord._1() />
                                    <#assign calculation = studentMarkRecord._4().suggestions[suggestion_index].calculation />
                                    <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.calculate" value="true" />
                                    <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.mark" value="${calculation.mark!}" />
                                    <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.grade" value="${calculation.grade!}" />
                                    <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.grade-txt" value="${calculation.grade!}" />
                                    <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.result" value="${(calculation.result.dbValue)!}" />
                                    <@f.hidden class="mma-suggestion-field" data\-target="students${student.sprCode?replace('/', '_')}.comments" value="${calculation.comments!}" />
                                  </#list>
                                  <button type="button" class="btn btn-primary pull-right" data-toggle="copy-values" data-target="#mma-modal-all-suggestion-${suggestion_index}" data-dismiss="modal">Use this calculation for all students <i class="fas fa-arrow-right" aria-hidden="true"></i></button>
                                </div>
                              </div>
                            </#if>
                          </#list>
                        </@modal.body>
                        <@modal.footer>
                          <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                        </@modal.footer>
                      </@modal.wrapper>
                    </@modal.modal>
                  </#if>
                </th>

                <th class="col-sm-2" data-sorter="false" rowspan="2">Mark</th>
                <th class="col-sm-1" data-sorter="false" rowspan="2">Grade</th>
                <th class="col-sm-1" data-sorter="false" rowspan="2">Result</th>
                <th class="col-sm-2" data-sorter="false" rowspan="2">Comments</th>
              </tr>

              <tr>
                <#list assessmentComponents as ac>
                  <th class="sortable" data-sorter="digit">Mark</th>
                  <th class="sortable" data-sorter="text">Grade</th>
                </#list>

                <th class="sortable" data-sorter="digit">Mark</th>
                <th class="sortable" data-sorter="text">Grade</th>
                <th class="sortable" data-sorter="text">Result</th>
              </tr>
            </thead>
            <tbody>
              <#list studentModuleMarkRecords as studentMarkRecord>
                <@studentMarkRecordRow studentMarkRecord />
              </#list>
            </tbody>
          </table>

          <div class="submit-buttons fix-footer">
            <input type="submit" class="btn btn-primary" value="Save" />
            <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome module.adminDepartment academicYear />">Cancel</a>
          </div>
        </@f.form>
      </div>
    </div>
    <div role="tabpanel" class="tab-pane <#if (from_origin!"webform") == "upload">active</#if>" id="upload">
      <p>You can upload marks and grades in a spreadsheet, which must be an XLSX file (i.e. created in Microsoft Office 2007 or later). The spreadsheet
        should have the following column headings: <strong>SPR Code</strong>, <strong>Mark</strong>,
        <strong>Grade</strong>, <strong>Result</strong> and <strong>Comments</strong>.
        You can use this <a href="<@routes.marks.module_marks_template sitsModuleCode academicYear occurrence />">generated spreadsheet</a> as a template.
      </p>

      <p>Enter the corresponding results for each student in the following columns:</p>

      <ul>
        <li>Column B - Mark</li>
        <li>Column C - Grade</li>
        <li>Column D - Result</li>
        <li>Column E - Comments</li>
      </ul>

      <@f.form method="post" enctype="multipart/form-data" action=submitUrl?markup_string modelAttribute="command">
        <h3>Select file</h3>

        <@bs3form.labelled_form_group path="file" labelText="File">
          <input type="file" name="file.upload" />
        </@bs3form.labelled_form_group>

        <div class="buttons form-group">
          <button type="submit" class="btn btn-primary">Upload</button>
          <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome module.adminDepartment academicYear />">Cancel</a>
        </div>
      </@f.form>
    </div>
  </div>
</div>
