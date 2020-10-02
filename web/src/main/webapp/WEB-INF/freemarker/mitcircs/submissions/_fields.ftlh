<#import "*/modal_macros.ftlh" as modal />
<#import "mitcirc_form_macros.ftl" as mitcirc />
<#import "*/mitcircs_components.ftlh" as components />

<@mitcirc.identity_section student />

<#if features.mitcircsCovid19>
  <@mitcirc.question_section
    question = "Are your mitigating circumstances coronavirus related?"
    showNumber = false
  >
    <div class="radio">
      <@bs3form.radio_inline>
        <@f.radiobutton path="covid19Submission" value="true" /> Yes
      </@bs3form.radio_inline>
      <@bs3form.radio_inline>
        <@f.radiobutton path="covid19Submission" value="false" /> No
      </@bs3form.radio_inline>
      <@bs3form.errors path="covid19Submission" />
    </div>
  </@mitcirc.question_section>
</#if>

<#--TODO get URL for appendix B -->
<#assign typeHint>
  Tick all that apply, but remember that you'll need to tell us something about each item you tick, and to upload some supporting evidence for each item. Detailed evidence requirements can be found <a href='#'>here</a>.
</#assign>

<@mitcirc.question_section
  question = "Your coronavirus circumstances"
  hint = typeHint
  cssClass = "mitcircs-form__fields__section--covid19--yes"
  covid19Hide = true
>
  <@mitcirc.checkboxesWithOther covid19IssueTypes "issueTypes" "issueTypeDetails" />
</@mitcirc.question_section>


<@mitcirc.question_section
  question = "What kind of mitigating circumstances are you presenting?"
  covidQuestion = "What kind of coronavirus circumstances are you presenting?"
  hint = typeHint
  cssClass="mitcircs-form__fields__section--covid19--no"
  covid19Hide = true
>
  <@mitcirc.checkboxesWithOther issueTypes "issueTypes" "issueTypeDetails" />
</@mitcirc.question_section>

<@mitcirc.question_section
  question = "What period do your mitigating circumstances cover?"
  covidQuestion = "What period do your coronavirus circumstances cover?"
  hint = "If you're claiming for a period in the past, include a start and end date. If you're claiming for
    something that's ongoing, you may not know the end date at this point."
  covidHint = "If you’re claiming for a period in the past, or a defined self-isolation period, include a
    start and end date. If you’re claiming for something that’s ongoing, you may not know the end date at this point."
  cssClass = "form-horizontal"
  covid19Hide = true
>
  <@bs3form.form_group "startDate">
    <@bs3form.label path="startDate" cssClass="col-xs-4 col-sm-2">Start date</@bs3form.label>

    <div class="col-xs-8 col-sm-4">
      <@spring.bind path="startDate">
        <div class="input-group">
          <@f.input path="startDate" autocomplete="off" cssClass="form-control date-picker" />
          <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
        </div>
      </@spring.bind>

      <@bs3form.errors path="startDate" />
    </div>
  </@bs3form.form_group>

  <@bs3form.form_group "endDate">
    <@bs3form.label path="endDate" cssClass="col-xs-4 col-sm-2">End date</@bs3form.label>

    <div class="col-xs-8 col-sm-4">
      <@bs3form.radio>
        <@f.radiobutton path="noEndDate" value="false" cssClass="radio-with-field" />

        <@spring.bind path="endDate">
          <div class="input-group">
            <@f.input path="endDate" autocomplete="off" cssClass="form-control date-picker" />
            <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
          </div>
        </@spring.bind>
      </@bs3form.radio>

      <@bs3form.radio>
        <@f.radiobutton path="noEndDate" value="true" /> Ongoing
      </@bs3form.radio>

      <@bs3form.errors path="endDate" />
    </div>
  </@bs3form.form_group>
</@mitcirc.question_section>

<#if previousSubmissions?has_content>
  <@mitcirc.question_section
    question = "Does this relate to a previous mitigating circumstances claim of yours?"
    cssClass="mitcircs-form__fields__section--covid19--no"
    covid19Hide = true
  >
    <@bs3form.form_group path="relatedSubmission">
      <@f.select path="relatedSubmission" cssClass="form-control">
        <option value="">No, this does not relate to a previous submission</option>
        <#list previousSubmissions as submission>
          <#assign label>MIT-${submission.key} <#if submission.issueTypes?has_content><#list submission.issueTypes as type>${type.description}<#if type.entryName == "Other" && submission.issueTypeDetails?has_content> - ${submission.issueTypeDetails!""}</#if><#if type_has_next>, </#if></#list></#if></#assign>
          <@f.option value="${submission.key}" label="${label?markup_string}" />
        </#list>
      </@f.select>
      <@bs3form.errors path="relatedSubmission" />
    </@bs3form.form_group>
  </@mitcirc.question_section>
</#if>

<@mitcirc.question_section
  question = "Which assessments have been affected?"
  hint = "Please tell us which coursework submissions or exams have been affected by your mitigating circumstances.
    To make this easier, we've listed the assignments and exams that we think fall within the period you've selected."
  covidHint = "Please tell us which coursework submissions or exams have been affected by your coronavirus circumstances.
    To make this easier, we've listed the assignments and exams that we think fall within the period you've selected."
  covid19Hide = true
>
  <ul class="nav nav-tabs" role="tablist">
    <li role="presentation" class="active"><a href="#assessment-table-assignments" aria-controls="assessment-table-assignments" role="tab" data-toggle="tab">Assignments</a></li>
    <li role="presentation"><a href="#assessment-table-exams" aria-controls="assessment-table-exams" role="tab" data-toggle="tab">Exams</a></li>
    <li role="presentation"><a href="#assessment-table-other" aria-controls="assessment-table-other" role="tab" data-toggle="tab">Other</a></li>
  </ul>

  <table class="table table-striped table-condensed table-hover table-checkable mitcircs-form__fields__section__assessments-table tab-content" data-endpoint="<@routes.mitcircs.affectedAssessments student />">
    <colgroup>
      <col class="col-sm-1">
      <col class="col-sm-3">
      <col class="col-sm-5">
      <col class="col-sm-3">
    </colgroup>
    <thead>
      <tr>
        <th scope="col" class="mitcircs-form__fields__section__assessments-table__checkbox"><input type="checkbox" class="check-all" title="Select all/none"></th></th>
        <th scope="col" class="mitcircs-form__fields__section__assessments-table__module">Module</th>
        <th scope="col" class="mitcircs-form__fields__section__assessments-table__name">Title</th>
        <th scope="col" class="mitcircs-form__fields__section__assessments-table__deadline">Deadline / exam date</th>
      </tr>
    </thead>
    <tfoot>
      <tr>
        <td><button type="button" class="btn btn-default btn-sm">Add</button></td>
        <td>
          <label class="control-label sr-only" for="new-assessment-module">Module</label>
          <select id="new-assessment-module" class="form-control input-sm">
            <option></option>
            <#list registeredYears as year>
              <optgroup label="${year.toString}">
                <#list (mapGet(registeredModules, year))![] as module>
                  <option value="${module.code}" data-name="${module.name}"><@fmt.module_name module=module withFormatting=false /></option>
                </#list>
                <#if includeEngagementCriteria>
                  <option value="OE" data-name="Engagement criteria">Engagement criteria</option>
                </#if>
                <option value="O" data-name="Other">Other</option>
              </optgroup>
            </#list>
          </select>
        </td>
        <td>
          <label class="control-label sr-only" for="new-assessment-name">Title</label>
          <input id="new-assessment-name" type="text" class="form-control input-sm">
        </td>
        <td>
          <label class="control-label sr-only" for="new-assessment-deadline">Deadline or examination date</label>
          <div class="input-group">
            <input id="new-assessment-deadline" type="text" autocomplete="off" class="form-control input-sm date-picker">
            <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
          </div>
        </td>
      </tr>
    </tfoot>

    <#macro affectedAssessment item index>
      <@spring.nestedPath path="affectedAssessments[${index}]">
        <tr>
          <td class="mitcircs-form__fields__section__assessments-table__checkbox">
            <@f.hidden path="moduleCode" />
            <@f.hidden path="sequence" />
            <@f.hidden path="academicYear" />
            <@f.hidden path="assessmentType" />
            <@f.hidden path="acuteOutcomeApplies" />
            <@f.hidden path="extensionDeadline" />
            <@f.checkbox path="selected" />
          </td>
          <td class="mitcircs-form__fields__section__assessments-table__module">
            <@components.assessmentModule item />
            <@f.errors path="moduleCode" cssClass="error form-errors text-danger" />
            <@f.errors path="sequence" cssClass="error form-errors text-danger" />
            <@f.errors path="academicYear" cssClass="error form-errors text-danger" />
            <@f.errors path="assessmentType" cssClass="error form-errors text-danger" />
          </td>
          <td class="mitcircs-form__fields__section__assessments-table__name">
            <@f.hidden path="name" />
            ${item.name}
            <@f.errors path="name" cssClass="error form-errors text-danger" />
          </td>
          <td class="mitcircs-form__fields__section__assessments-table__deadline">
            <div class="input-group">
              <@f.input path="deadline" autocomplete="off" cssClass="form-control input-sm date-picker" />
              <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
            </div>
            <@f.errors path="deadline" cssClass="error form-errors text-danger" />
          </td>
        </tr>
      </@spring.nestedPath>
    </#macro>

    <tbody id="assessment-table-assignments" role="tabpanel" class="tab-pane active">
      <#if command.affectedAssessments?has_content>
        <#list command.affectedAssessments as item>
          <#if item.assessmentType?? && item.assessmentType.code == 'A'>
            <@affectedAssessment item item_index />
          </#if>
        </#list>
      </#if>
    </tbody>
    <tbody id="assessment-table-exams" role="tabpanel" class="tab-pane">
      <#if command.affectedAssessments?has_content>
        <#list command.affectedAssessments as item>
          <#if item.assessmentType?? && item.assessmentType.code == 'E'>
            <@affectedAssessment item item_index />
          </#if>
        </#list>
      </#if>
    </tbody>
    <tbody id="assessment-table-other" role="tabpanel" class="tab-pane">
      <#if command.affectedAssessments?has_content>
        <#list command.affectedAssessments as item>
          <#if item.assessmentType?? && item.assessmentType.code == 'O'>
            <@affectedAssessment item item_index />
          </#if>
        </#list>
      </#if>
    </tbody>
  </table>

  <@f.errors path="affectedAssessments" cssClass="error form-errors text-danger" />
</@mitcirc.question_section>

<@mitcirc.question_section
  question = "Have you discussed your mitigating circumstances with anyone?"
  covidQuestion = "Have you discussed your circumstances with anyone?"
  covid19Hide = true
>
  <div class="radio">
    <@bs3form.radio_inline>
      <@f.radiobutton path="contacted" value="true" /> Yes
    </@bs3form.radio_inline>
    <@bs3form.radio_inline>
      <@f.radiobutton path="contacted" value="false" /> No
    </@bs3form.radio_inline>
    <@bs3form.errors path="contacted" />
  </div>
  <div class="mitcircs-form__fields__contact-subfield mitcircs-form__fields__contact-subfield--yes" style="display: none;">
    <#if personalTutors?has_content>
      <#assign tutorNames><#list personalTutors as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list></#assign>
      <#assign personalTutors = { "PersonalTutor": tutorNames } />
    <#else>
      <#assign personalTutors = {} />
    </#if>
    <@mitcirc.checkboxesWithOther possibleContacts "contacts" "contactOther" personalTutors />
  </div>
  <div class="mitcircs-form__fields__contact-subfield mitcircs-form__fields__contact-subfield--no" style="display: none;">
    <div class="<#if features.mitcircsCovid19>covid--no</#if>">
      <@mitcirc.question_hint "Please tell us why you haven’t been able to discuss your circumstances with anyone yet." />

      <@bs3form.form_group "noContactReason">
        <@f.textarea path="noContactReason" cssClass="form-control" rows="5" />
        <@bs3form.errors path="noContactReason" />
      </@bs3form.form_group>
    </div>

    <div class="mitcircs-form__fields__section__hint">
      <p>Depending on your circumstances, you might find it helpful to talk to one or more of the following:</p>

      <ul>
        <li>Your personal tutor or another member of staff in your department</li>
        <li>Your GP or other healthcare professional</li>
        <li>Warwick’s <a href="https://warwick.ac.uk/supportservices" target="_blank">Wellbeing Support Services</a></li>
        <li><a href="https://warwick.ac.uk/insite/coronavirus/" target="_blank">Current university FAQs on coronavirus</a></li>
        <li><a href="https://www.gov.uk/government/publications/covid-19-stay-at-home-guidance/stay-at-home-guidance-for-people-with-confirmed-or-possible-coronavirus-covid-19-infection" target="_blank">Current UK government advice on coronavirus</a></li>
        <li><a href="https://111.nhs.uk/service/COVID-19/" target="_blank">Current NHS 111 advice on coronavirus</a></li>
      </ul>
    </div>
  </div>
</@mitcirc.question_section>

<@mitcirc.question_section question = "Details" covid19Hide = true>
  <#if features.mitcircsCovid19>
    <p class="mitcircs-form__fields__section__hint covid--yes">
      Please provide some details of how coronavirus has impacted you and how this has affected your assessments.
      (E.g. school was closed from 21/3/20 and I could not attend due to childcare, I developed a cough and have
      self-isolated for 7 days)
    </p>
  </#if>
  <ol class="mitcircs-form__fields__section__hint <#if features.mitcircsCovid19>covid--no</#if>">
    <li>Please provide further details of the mitigating circumstances and how they have affected your assessments.</li>
    <li>Please include anything that you've done, or that's being done by other people, to help with your issues.</li>
    <li>If you're getting treatment or other support which will eventually resolve your issues, please tell us something about how and when you expect this to take place.</li>
  </ol>

  <@bs3form.form_group "reason">
    <@f.textarea path="reason" cssClass="form-control" rows="5" />
    <@bs3form.errors path="reason" />
  </@bs3form.form_group>
</@mitcirc.question_section>

<#assign evidenceGeneralHelp>
  <p>Evidence is a vital part of a mitigating circumstances submission. Without it your claim may be rejected.</p>
  <ul>
    <li>Photocopy or scanned evidence is acceptable.</li>
    <li>If you are waiting for evidence and are worried it will not arrive in time before the mitigating circumstances deadline you should still submit your case BUT use the next section of the form to tell us about evidence that you'll provide later.</li>
    <li>Written around the time you were experiencing your claim in order for an assessment to be made on the impact of your claim. Evidence written sometime after the event will not normally be accepted.</li>
    <li>Written by an independent qualified practitioner (letters from relatives are not normally acceptable); dated and written on headed or official notepaper and in English. If the letter is in another language you must provide both a copy of the original note and a certified translation into English. The University may seek to verify the accuracy of the translation provided.</li>
  </ul>
</#assign>

<#if features.mitcircsCovid19>
  <#assign supportingEvidenceHint>
    Due to the coronavirus pandemic it may not be possible to obtain the required evidence in a timely manner, or at all. During the pandemic the University will be sympathetic to claims that do not contain such evidence <u>as long as you explain why it has not been possible to obtain it</u>.  Any evidence from the list below that you are able to provide will help your Department make an informed decision on your mitigation request. You should not compromise your safety or the safety of others in order to obtain evidence.
  </#assign>
<#else>
  <#assign supportingEvidenceHint = "Claims submitted without some independent supporting evidence will not normally be considered for mitigating circumstances. If your supporting evidence is particularly sensitive, then instead of uploading it here, you may prefer to show it to a member of staff (e.g. your personal tutor) who will then certify the evidence on your behalf." />
</#if>

<@mitcirc.question_section
  question = "Please upload any supporting evidence relevant to your submission"
  hint = supportingEvidenceHint
  covidHint = supportingEvidenceHint
  cssClass = "mitcircs-form__fields__section__evidence-upload"
  helpPopover = evidenceGeneralHelp
  helpCovidPopover = evidenceGeneralHelp
  covid19Hide = true
>
  <#function render_attachment attachment>
    <#local result><@routes.mitcircs.renderAttachment submission attachment /></#local>
    <#return result />
  </#function>
  <@bs3form.attachmentsList
    path="attachedFiles"
    labelText="Supporting documentation"
    attachedFiles=command.attachedFiles
    routeFunction=render_attachment
    help="This is a list of all supporting documents that have been attached to this mitigating circumstances submission. Click the remove link next to a document to delete it."
  />

  <@bs3form.filewidget
    basename="file"
    labelText=""
    types=[]
    multiple=true
    required=false
    customHelp=" "
  />

  <div class="checkbox">
    <label>
      <@f.checkbox path="hasSensitiveEvidence" value="true" /> I have sensitive evidence that I would prefer to show to a member of staff in person
      <@bs3form.errors path="hasSensitiveEvidence" />
    </label>
  </div>

</@mitcirc.question_section>

<@mitcirc.question_section
  question = "If you're unable to provide evidence now please tell us about evidence you’re going to upload in the future"
  hint = "Please give a brief description of this outstanding evidence and tell us when you expect to be able to provide it"
  cssClass = "form-horizontal mitcircs-form__fields__section--covid19--no"
  covid19Hide = true
>
  <@bs3form.form_group "pendingEvidenceDue">
    <@bs3form.label path="pendingEvidenceDue" cssClass="col-xs-4 col-sm-2">Due date</@bs3form.label>
    <div class="col-xs-8 col-sm-4">
      <@spring.bind path="pendingEvidenceDue">
        <div class="input-group">
          <@f.input path="pendingEvidenceDue" autocomplete="off" cssClass="form-control date-picker" />
          <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
        </div>
      </@spring.bind>

      <@bs3form.errors path="pendingEvidenceDue" />
    </div>
  </@bs3form.form_group>

  <@bs3form.form_group "pendingEvidence">
    <@bs3form.label path="pendingEvidence" cssClass="col-xs-4 col-sm-2">Description</@bs3form.label>
    <div class="col-xs-8 col-sm-10">
      <@f.textarea path="pendingEvidence" cssClass="form-control" rows="5" />
      <@bs3form.errors path="pendingEvidence" />
    </div>
  </@bs3form.form_group>
</@mitcirc.question_section>
