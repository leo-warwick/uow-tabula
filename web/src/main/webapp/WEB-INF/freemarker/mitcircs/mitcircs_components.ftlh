<#ftl strip_text=true />

<#-- FIXME why is this necessary? -->
<#if JspTaglibs??>
  <#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
  <#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
</#if>

<#function stage_sortby stages>
  <#local result = '' />

  <#list stages?reverse as progress>
    <#local stageResult = '1' /> <#-- not started -->
    <#if progress.completed>
      <#local stageResult = '4' />
    <#elseif progress.skipped>
      <#local stageResult = '2' />
    <#elseif progress.started>
      <#local stageResult = '3' />
    </#if>

    <#local result = "${result}${stageResult}" />
  </#list>

  <#return result />
</#function>

<#macro enumListWithOther enumValues otherValue condensed=true><#compress>
  <#if condensed>
    <#list enumValues as value>${value.description}<#if value.entryName == "Other"> (${otherValue?trim})</#if><#if value_has_next>, </#if></#list>
  <#elseif enumValues?has_content>
    <ul class="list-unstyled">
      <#list enumValues as value>
        <li>${value.description}<#if value.entryName == "Other"> (${otherValue?trim})</#if></li>
      </#list>
    </ul>
  </#if>
</#compress></#macro>

<#macro detail label condensed=false bigLabel=false>
  <div class="row form-horizontal mitcircs-details__detail <#if condensed>mitcircs-details__detail--condensed</#if>">
    <div class="col-sm-<#if bigLabel>4<#else>3</#if> control-label">
      ${label}
    </div>
    <div class="col-sm-<#if bigLabel>8<#else>9</#if>">
      <div class="form-control-static">
        <#nested>
      </div>
    </div>
  </div>
</#macro>

<#macro section label>
  <div class="mitcircs-details__section row form-horizontal">
    <div class="control-label">${label}</div>
    <div class="content form-control-static">
      <#nested>
    </div>
  </div>
</#macro>

<#macro asyncSection id label url>
  <div id="${id}" class="mitcircs-details__section async row form-horizontal" data-href="${url}">
    <div class="control-label">${label}</div>
    <div class="content form-control-static">
      <i class="id7-koan-spinner id7-koan-spinner--xs id7-koan-spinner--inline" aria-hidden="true"></i> Loading&hellip;
    </div>
  </div>
</#macro>

<#-- Progress bar for a single submission  -->
<#macro stage_progress_bar stages>
  <div class="stage-progress-bar">
    <#list stages as progress>
      <#local stage = progress.stage />

      <#local state = 'default' />
      <#local icon = 'fa-circle-o' />
      <#if progress.completed>
        <#if progress.health.toString == 'Good'>
          <#local state = 'success' />
          <#local icon = 'fa-check-circle-o' />
        <#else>
          <#local state = 'danger' />
          <#local icon = 'fa-times-circle-o' />
        </#if>
      <#elseif progress.skipped>
        <#local state = 'primary' />
        <#local icon = 'fa-arrow-circle-o-right' />
      <#elseif progress.started>
        <#local state = 'warning' />
        <#local icon = 'fa-dot-circle-o' />
      </#if>

      <#local title><@spring.message code=progress.stage.actionCode /></#local>
      <#if progress_index gt 0>
        <div tabindex="0" class="bar bar-${state} use-tooltip" data-toggle="tooltip" data-placement="top" title="${fmt.strip_html(title?markup_string)}"></div>
      </#if>
      <#local title><@spring.message code=progress.messageCode /></#local>
      <span tabindex="0" class="fa-stack use-tooltip" data-toggle="tooltip" data-placement="top" title="${fmt.strip_html(title?markup_string)}">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x ${icon} text-${state}"></i>
			</span>
    </#list>
  </div>
</#macro>

<#macro attachments submission>
  <#if submission.attachments?has_content>
    <table class="mitcircs-attachments table table-consensed">
      <thead>
        <tr>
          <th>File</th>
          <th>Date uploaded</th>
        </tr>
      </thead>
      <tbody>
        <#list submission.attachments as attachment>
          <#local mimeTypeDetectionResult = mimeTypeDetector(attachment) />
          <tr id="attachment-${attachment.id}" class="attachment mitcircs-attachments__attachment">
            <td>
              <@fmt.file_type_icon mimeTypeDetectionResult.mediaType />
              <a href="<@routes.mitcircs.renderAttachment submission attachment />" <#if mimeTypeDetectionResult.serveInline>data-inline="true"</#if>><#compress>${attachment.name}</#compress></a>
            </td>
            <td><@fmt.date date=attachment.dateUploaded shortMonth=true excludeCurrentYear=true /></td>
          </tr>
        </#list>
      </tbody>
    </table>
  </#if>
</#macro>

<#macro submissionTable submissions actions=true panel=false forPanel=false submissionStages={}>
  <#if submissions?has_content>
    <table class="mitcircs-panel-form__submissions table table-condensed table-sortable">
      <thead>
      <tr>
        <th class="sortable header">Reference</th>
        <th class="sortable header">Student</th>
        <th class="sortable header">Affected dates</th>
        <th class="sortable header">Last updated</th>
        <#if panel><th class="sortable header">Current panel</th></#if>
        <#if submissionStages?has_content><th class="col-sm-2 sortable header">Progress</th></#if>
        <th></th>
      </tr>
      </thead>
      <tbody>
      <#list submissions as submission>
        <tr>
          <td>
            <#if !submission.draft>
              <#if forPanel>
                <a href="<@routes.mitcircs.reviewSubmissionPanel submission />">MIT-${submission.key}</a>
              <#else>
                <a href="<@routes.mitcircs.reviewSubmission submission />">MIT-${submission.key}</a>
              </#if>
            <#else>
              MIT-${submission.key}
            </#if>
            <#if actions><@f.hidden path="submissions" value="${submission.key}" /></#if>
          </td>
          <td>
            <@pl.profile_link submission.student.universityId />
            ${submission.student.universityId}
            ${submission.student.firstName}
            ${submission.student.lastName}
          </td>
          <td>
            <#if submission.startDate??>
              <@fmt.date date=submission.startDate includeTime=false relative=false shortMonth=true excludeCurrentYear=true />
              &mdash;
              <#if submission.endDate??>
                <@fmt.date date=submission.endDate includeTime=false relative=false shortMonth=true excludeCurrentYear=true />
              <#else>
                <span class="very-subtle">(ongoing)</span>
              </#if>
            <#else>
              <span class="very-subtle">TBC</span>
            </#if>
          </td>
          <td>
            <@fmt.date date=submission.lastModified shortMonth=true excludeCurrentYear=true />
            <#if submission.unreadByOfficer>
              <span tabindex="0" class="tabula-tooltip" data-title="There are unread change(s)"><i class="far fa-envelope text-info"></i></span>
            </#if>
          </td>
          <#if panel>
            <td>
              <#if submission.panel??>
                ${submission.panel.name}
              <#else>
                <span class="very-subtle">None</span>
              </#if>
            </td>
          </#if>
          <#if submissionStages?has_content && mapGet(submissionStages, submission)??>
            <#assign stages = mapGet(submissionStages, submission)/>
            <td data-sortby="${stage_sortby(stages?values)}"><@stage_progress_bar stages?values /></td>
          </#if>
          <#if actions>
            <td>
              <button class="remove btn btn-sm">Remove</button>
            </td>
          </#if>
        </tr>
      </#list>
      </tbody>
    </table>
  <#else>
    <div class="form-control-static">No submissions</div>
  </#if>
</#macro>

<#macro panelDetails panel show_name=false>
    <#if show_name>
      <@detail label="Name" condensed=true>
        <a href="<@routes.mitcircs.viewPanel panel />">${panel.name}</a>
      </@detail>
    </#if>
    <@detail label="Date" condensed=true>
      <#if panel.date??><@fmt.date date=panel.date includeTime=false relative=false /><#if panel.startTime??>: <@fmt.time panel.startTime /><#if panel.endTime??> &mdash; <@fmt.time panel.endTime /></#if></#if><#else><span class="very-subtle">TBC</span></#if>
    </@detail>
    <#if panel.location??><@detail label="Location" condensed=true><@fmt.location panel.location /></@detail></#if>
    <@detail label="Panel chair" condensed=true>
        <#if panel.chair??>${panel.chair.fullName}<#else><span class="very-subtle">TBC</span></#if>
    </@detail>
    <@detail label="Panel secretary" condensed=true>
        <#if panel.secretary??>${panel.secretary.fullName}<#else><span class="very-subtle">TBC</span></#if>
    </@detail>
    <@detail label="Panel members" condensed=true>
        <#if panel.members?has_content>
            <#list panel.members as member>${member.fullName}<#if member_has_next>, </#if></#list>
        <#else>
          <span class="very-subtle">TBC</span>
        </#if>
    </@detail>
</#macro>

<#macro panelsTable panels>
  <table class="table table-condensed table-sortable">
    <thead>
      <tr>
        <th class="sortable">Name</th>
        <th class="sortable">Date</th>
        <th class="sortable">Location</th>
        <th class="sortable">Chair</th>
        <th class="sortable">Secretary</th>
        <th class="sortable">Members</th>
        <th class="sortable">Submissions</th>
      </tr>
    </thead>
    <tbody>
      <#list panels as panel>
        <tr>
          <td><a href="<@routes.mitcircs.viewPanel panel />">${panel.name}</a></td>
          <td data-sortby="${(panel.date.toString("yyyy-MM-dd HH:mm"))!'1970-01-01 00:00'}">
            <#if panel.date??>
              <@fmt.date date=panel.date includeTime=false relative=false shortMonth=true excludeCurrentYear=true /><#if panel.startTime??>: <@fmt.time panel.startTime /><#if panel.endTime??> &mdash; <@fmt.time panel.endTime /></#if></#if>
            <#else>
              <span class="very-subtle">TBC</span>
            </#if>
          </td>
          <td><#if panel.location??><@fmt.location panel.location /></#if></td>
          <td data-sortby="${(panel.chair.lastName)!'TBC'}, ${(panel.chair.firstName)!'TBC'}"><#if panel.chair??>${panel.chair.fullName}<#else><span class="very-subtle">TBC</span></#if></td>
          <td data-sortby="${(panel.secretary.lastName)!'TBC'}, ${(panel.secretary.firstName)!'TBC'}"><#if panel.secretary??>${panel.secretary.fullName}<#else><span class="very-subtle">TBC</span></#if></td>
          <td><#list panel.members as member>${member.fullName}<#if member_has_next>, </#if></#list></td>
          <td><#if panel.submissions??>${panel.submissions?size}<#else>0</#if></td>
        </tr>
      </#list>
    </tbody>
  </table>

  <script type="text/javascript" nonce="${nonce()}">
    (function ($) {
      $('.table-sortable').sortableTable({
        // Default is to sort by the date of the panel, ascending
        sortList: [[1, 0]],

        // If there's a data-sortby, use that as the sort value
        textExtraction: function (node) {
          var $el = $(node);
          if ($el.data('sortby')) {
            return $el.data('sortby');
          } else {
            return $el.text().trim();
          }
        }
      });
    })(jQuery);
  </script>
</#macro>

<#macro assessmentType assessment><#compress>
  <#if assessment.assessmentType.code == "E">
    Exam
  <#elseif assessment.assessmentType.code == "O">
    Other
  <#else>
    Assignment
  </#if>
</#compress></#macro>

<#macro assessmentModule assessment formatted=true>
  <#if formatted>
    <span class="mod-code">${((assessment.module.code)!assessment.moduleCode)?upper_case}</span>
    <span class="mod-name"><#if assessment.moduleCode == "OE">Engagement criteria<#elseif assessment.moduleCode == "O">Other<#else>${assessment.module.name}</#if> (${assessment.academicYear.toString})</span>
  <#else>
    ${((assessment.module.code)!assessment.moduleCode)?upper_case} <#if assessment.moduleCode == "OE">Engagement criteria<#elseif assessment.moduleCode == "O">Other<#else>${assessment.module.name}</#if> (${assessment.academicYear.toString})
  </#if>
</#macro>

<#macro studentSummary submission consise=false>
  <#-- Identity information about the student -->
  <#assign student = submission.student />
  <@detail label="Name" condensed=true bigLabel=consise>${student.fullName}</@detail>
  <@detail label="University ID" condensed=true bigLabel=consise>${student.universityId}</@detail>
  <#if student.email??><@detail label="Email" condensed=true bigLabel=consise>${student.email}</@detail></#if>

  <#if student.mostSignificantCourseDetails??>
    <#assign studentCourseDetails = student.mostSignificantCourseDetails />
    <@detail label="Course" condensed=true bigLabel=consise>${studentCourseDetails.course.name}</@detail>

    <#if studentCourseDetails.latestStudentCourseYearDetails??>
        <#assign studentCourseYearDetails = studentCourseDetails.latestStudentCourseYearDetails />

        <#if studentCourseYearDetails.yearOfStudy??>
            <@detail label="Year of study" condensed=true bigLabel=consise>${studentCourseYearDetails.yearOfStudy}</@detail>
        </#if>

        <#if studentCourseYearDetails.modeOfAttendance??>
            <@detail label="Mode of study" condensed=true bigLabel=consise>${studentCourseYearDetails.modeOfAttendance.fullNameAliased}</@detail>
        </#if>
    </#if>
  </#if>
</#macro>

<#macro submissionSummary submission consise=false>
  <@detail label="Issue type" condensed=true bigLabel=consise><@components.enumListWithOther enumValues=submission.issueTypes otherValue=submission.issueTypeDetails!"" condensed=false /></@detail>
  <@detail label="Submission date" condensed=true bigLabel=consise><#if submission.approvedOn??><@fmt.date date=submission.approvedOn includeTime=false shortMonth=consise /><#else><span class="very-subtle">TBC</span></#if></@detail>
  <@detail label="Start date" condensed=true bigLabel=consise><#if submission.startDate??><@fmt.date date=submission.startDate includeTime=false shortMonth=consise /><#else><span class="very-subtle">TBC</span></#if></@detail>
  <@detail label="End date" condensed=true bigLabel=consise>
    <#if submission.endDate??><@fmt.date date=submission.endDate shortMonth=consise includeTime=false /><#else><span class="very-subtle">Issue ongoing</span></#if>
  </@detail>
  <#if submission.relatedSubmission??>
    <#assign relatedLabel>Related<#if !consise> submission</#if></#assign>
    <@detail label="${relatedLabel}" condensed=true bigLabel=consise>
      <a href="<@routes.mitcircs.reviewSubmission submission.relatedSubmission />">
        MIT-${submission.relatedSubmission.key}
          <@enumListWithOther submission.relatedSubmission.issueTypes submission.relatedSubmission.issueTypeDetails!"" />
      </a>
    </@detail>
  </#if>
</#macro>

<#macro privacyStatement>
  <p>The University of Warwick will process your personal data provided on this online mitigating circumstances form, and any other disclosed data you provide, for the purposes of assessing your mitigating circumstances.</p>
  <p>The legal basis for processing this personal data is necessary for the performance of a contract.  Please note that the information you provide will be reviewed by the departmental mitigating circumstances pre-board in order to make a recommendation to the Board of Examiners about your case.  Your personal data will not be shared or disclosed to any third parties external to the University of Warwick except where required by law.</p>
  <p>Where any sharing of data occurs outside the EEA, we will ensure that there are appropriate safeguards in place to protect your personal data.  Your personal data will be kept securely by the University of Warwick and will be retained for six years after the end of the academic year in which you graduate from, or otherwise leave, the University.  After six years, the University will retain only the data necessary to identify you and confirm the dates you studied at the University, the degree and classification you were awarded and a transcript of your marks. All other personal data on your student record will be disposed of in a secure manner.</p>
  <p>The University of Warwick is the Data Controller of this information and is committed to protecting the rights of individuals in line with the Data Protection Legislation. Please visit the University’s <a href="https://warwick.ac.uk/dataprotection/">Data Protection webpages</a> for further information in relation to your rights and how the University processes your personal data. The University’s Data Protection Officer, A Bajaj, can be contacted through <a href="mailto:DPO@warwick.ac.uk">DPO@warwick.ac.uk</a> and any requests or complaints should be made in writing to the University’s Data Protection Officer.</p>
</#macro>
