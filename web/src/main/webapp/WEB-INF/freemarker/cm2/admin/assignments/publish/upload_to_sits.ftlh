<#import "*/cm2_macros.ftl" as cm2 />

<@cm2.assignmentHeader "Upload feedback to SITS" assignment "for" />
<#if command.gradeValidation.invalidLinkedToMultipleComponents?has_content >
  <div class="alert alert-danger">
    <p>
      <#assign total = command.gradeValidation.invalidLinkedToMultipleComponents?size />
      <@fmt.p total "student" />
      <#if total==1>
        has feedback that cannot be uploaded because the student is a member of more than one of the assessment components linked to this assignment.
      <#else>
        have feedback that cannot be uploaded because the students are members of more than one of the assessment components linked to this assignment.
      </#if>
    </p>
    <p>Feedback can only be uploaded to a single assessment component for each student. Check that the correct <a href="<@routes.cm2.assignmentstudents assignment "edit" />">assessment components</a> are linked for this assignment.</p>
  </div>

  <table class="table table-bordered table-condensed table-striped table-hover">
    <thead>
      <tr>
        <th>University ID</th>
        <th>Mark</th>
        <th>Grade</th>
      </tr>
    </thead>
    <tbody>
      <#list command.gradeValidation.invalidLinkedToMultipleComponents as feedback>
        <tr>
          <td>${feedback.studentIdentifier}</td>
          <td>${feedback.latestMark!}</td>
          <td>${feedback.latestGrade!}</td>
        </tr>
      </#list>
    </tbody>
  </table>
</#if>

<#if command.gradeValidation.notOnScheme?has_content >
  <div class="alert alert-danger">
    <p>
        <#assign total = command.gradeValidation.notOnScheme?keys?size />
        <@fmt.p total "student" />
        <#if total==1>
          has feedback that cannot be uploaded because the student is manually-added and therefore not present on a linked assessment component.
        <#else>
          have feedback that cannot be uploaded because the students are manually-added and therefore not present on a linked assessment component.
        </#if>
    </p>
  </div>

  <table class="table table-bordered table-condensed table-striped table-hover">
    <thead>
    <tr>
      <th>University ID</th>
      <th>Mark</th>
      <th>Grade</th>
      <th>Valid grades</th>
    </tr>
    </thead>
    <tbody>
    <#list command.gradeValidation.notOnScheme?keys as feedback>
      <tr>
        <td>${feedback.studentIdentifier}</td>
        <td>${feedback.latestMark!}</td>
        <td>${feedback.latestGrade!}</td>
        <td>${mapGet(command.gradeValidation.notOnScheme, feedback)}</td>
      </tr>
    </#list>
    </tbody>
  </table>
</#if>

<#if isGradeValidation>

  <#if command.gradeValidation.invalid?keys?has_content>
    <div class="alert alert-danger">
      <p>
        <#assign total = command.gradeValidation.invalid?keys?size />
        <@fmt.p total "student" />
        <#if total==1>
          has feedback with a grade that is invalid. It will not be uploaded.
        <#else>
          have feedback with grades that are invalid. They will not be uploaded.
        </#if>
      </p>
      <p>
        Any students with no valid grades may be registered to an assessment component without a mark code,
        or are not registered to an assessment component, and so cannot be uploaded.
      </p>
    </div>

    <table class="table table-bordered table-condensed table-striped table-hover">
      <thead>
      <tr>
        <th>University ID</th>
        <th>Mark</th>
        <th>Grade</th>
        <th>Valid grades</th>
      </tr>
      </thead>
      <tbody>
      <#list command.gradeValidation.invalid?keys as feedback>
        <tr>
          <td>${feedback.studentIdentifier}</td>
          <td>${feedback.latestMark!}</td>
          <td>${feedback.latestGrade!}</td>
          <td>${mapGet(command.gradeValidation.invalid, feedback)}</td>
        </tr>
      </#list>
      </tbody>
    </table>
  </#if>

  <#if command.gradeValidation.zero?keys?has_content>
    <div class="alert alert-danger">
      <p>
        <#assign total = command.gradeValidation.zero?keys?size />
        <@fmt.p total "student" />
        <#if total==1>
          has feedback with a mark of zero and no grade. Zero marks are not populated with a default grade and it will not be uploaded.
        <#else>
          have feedback with marks of zero and no grades. Zero marks are not populated with a default grade and they will not be uploaded.
        </#if>
      </p>
    </div>

    <table class="table table-bordered table-condensed table-striped table-hover">
      <thead>
      <tr>
        <th>University ID</th>
        <th>Mark</th>
        <th>Grade</th>
      </tr>
      </thead>
      <tbody>
      <#list command.gradeValidation.zero?keys as feedback>
        <tr>
          <td>${feedback.studentIdentifier}</td>
          <td>${feedback.latestMark!}</td>
          <td>${feedback.latestGrade!}</td>
        </tr>
      </#list>
      </tbody>
    </table>
  </#if>

  <#if command.gradeValidation.populated?keys?has_content>
    <div class="alert alert-info">
      <#assign total = command.gradeValidation.populated?keys?size />
      <@fmt.p total "student" />
      <#if total==1>
        has feedback with a grade that is empty. It will be populated with a default grade.
      <#else>
        have feedback with grades that are empty. They will be populated with a default grade.
      </#if>
    </div>

    <table class="table table-bordered table-condensed table-striped table-hover">
      <thead>
      <tr>
        <th>University ID</th>
        <th>Mark</th>
        <th>Populated grade</th>
      </tr>
      </thead>
      <tbody>
      <#list command.gradeValidation.populated?keys as feedback>
        <tr>
          <td>${feedback.studentIdentifier}</td>
          <td>${feedback.latestMark!}</td>
          <td>${mapGet(command.gradeValidation.populated, feedback)}</td>
        </tr>
      </#list>
      </tbody>
    </table>
  </#if>

<#else>
  <#if command.gradeValidation.invalid?has_content || command.gradeValidation.populated?has_content>
    <div class="alert alert-danger">
      <p>
        <#assign total = command.gradeValidation.invalid?keys?size + command.gradeValidation.populated?keys?size />
        <@fmt.p total "student" />
        <#if total==1>
          has feedback with a grade that is empty or invalid. It will not be uploaded.
        <#else>
          have feedback with grades that are empty or invalid. They will not be uploaded.
        </#if>
      </p>
      <p>
        Any students with no valid grades may be registered to an assessment component without a mark code,
        or are not registered to an assessment component, and so cannot be uploaded.
      </p>
    </div>

    <table class="table table-bordered table-condensed table-striped table-hover">
      <thead>
      <tr>
        <th>University ID</th>
        <th>Mark</th>
        <th>Grade</th>
        <th>Valid grades</th>
      </tr>
      </thead>
      <tbody>
      <#list command.gradeValidation.invalid?keys as feedback>
        <tr>
          <td>${feedback.studentIdentifier}</td>
          <td>${feedback.latestMark!}</td>
          <td>${feedback.latestGrade!}</td>
          <td>${mapGet(command.gradeValidation.invalid, feedback)}</td>
        </tr>
      </#list>
      <#list command.gradeValidation.populated?keys as feedback>
        <tr>
          <td>${feedback.studentIdentifier}</td>
          <td>${feedback.latestMark!}</td>
          <td>${feedback.latestGrade!}</td>
          <td></td>
        </tr>
      </#list>
      </tbody>
    </table>
  </#if>
</#if>

<#if command.gradeValidation.valid?has_content || isGradeValidation && command.gradeValidation.populated?has_content>

  <#assign total = command.gradeValidation.valid?size />
  <#if isGradeValidation && command.gradeValidation.populated?has_content>
    <@fmt.p total "other student" />
  <#else>
    <@fmt.p total "student" />
  </#if>
  <#if total==1>
    has feedback that will be uploaded.
  <#else>
    have feedback that will be uploaded.
  </#if>

  <table class="table table-bordered table-condensed table-striped table-hover">
    <thead>
    <tr>
      <th>University ID</th>
      <th>Mark</th>
      <th>Grade</th>
    </tr>
    </thead>
    <tbody>
    <#list command.gradeValidation.valid as feedback>
      <tr>
        <td>${feedback.studentIdentifier}</td>
        <td>${feedback.latestMark!}</td>
        <td>${feedback.latestGrade!}</td>
      </tr>
    </#list>
    </tbody>
  </table>

  <#assign locationInSITS>
    <#if assignment.resitAssessment>
      <p>Resit marks and grades display in the SITS SRA screen as actual marks and grades.</p>
    <#else>
      <p>Marks and grades display in the SITS SAT screen as actual marks and grades.</p>
    </#if>
  </#assign>

  <div class="alert alert-info">
    <#if assignment.module.adminDepartment.canUploadMarksToSitsForYear(assignment.academicYear, assignment.module)>
      <div>
          ${locationInSITS}
      </div>
    <#else>
      <div class="alert alert-warning">
        <p>
          Mark upload is closed
          for ${assignment.module.adminDepartment.name} <#if assignment.module.degreeType??> (${assignment.module.degreeType.toString})</#if>
          for the academic year ${assignment.academicYear.toString}.
        </p>
        <p>
          If you still have marks to upload, please contact the Exams Office <a id="email-support-link" href="mailto:aoexams@warwick.ac.uk">aoexams@warwick.ac.uk</a>.
        </p>
        <p>
          Select the checkbox to queue marks and grades for upload to SITS. As soon as mark upload re-opens for this department, the marks and grades will
          automatically upload. ${locationInSITS}
        </p>
      </div>
    </#if>
  </div>

  <#assign submitUrl><@routes.cm2.uploadToSits assignment /></#assign>
  <@f.form method="post" action=submitUrl?markup_string class="double-submit-protection">
    <#list command.students as student>
      <input type="hidden" name="students" value="${student}" />
    </#list>
    <div class="submit-buttons">
      <button class="btn btn-primary" type="submit" name="confirm">Upload</button>
    </div>
  </@f.form>

<#else>
  <em>There is no feedback to upload.</em>
</#if>