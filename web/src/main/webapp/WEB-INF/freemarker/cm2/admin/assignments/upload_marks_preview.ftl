<#import "*/cm2_macros.ftl" as cm2 />
<#assign commandName="command" />
<@spring.bind path=commandName>
  <#assign hasErrors=status.errors.allErrors?size gt 0 />
</@spring.bind>
<#assign noun_verb_passive="files uploaded successfully"/>
<#assign isfile=RequestParameters.isfile/>
<#if isfile = "true">
  <#assign text_acknowledge="Your ${noun_verb_passive} with marks and feedback for"/>
  <#assign text_problems="However, there are some problems, which are shown below.
				You need to correct these problems with the spreadsheet and upload it again.
				If you choose to confirm without amending the spreadsheet, any rows with errors
				are ignored."/>
  <#assign column_headings_warning="The first row in spreadsheets is assumed to contain column headings and is ignored."/>
<#else>
  <#assign text_acknowledge="You are submitting marks for "/>
  <#assign text_problems="However, there are some problems, which are shown below.
				You need to return to the previous page, correct these problems and try again.
				If you choose to confirm without fixing the data, any rows with errors
				are ignored."/>
  <#assign column_headings_warning=""/>
</#if>

<#escape x as x?html>
  <@cm2.assignmentHeader "Submit marks and feedback" assignment />
  <div class="fix-area">
    <@f.form method="post" action=formUrl modelAttribute=commandName>

      <@spring.bind path="marks">
        <#assign itemsList=status.actualValue />
        <#assign modifiedCount = 0 />
        <#list itemsList as item>
          <#if item.valid><#assign modifiedCount = modifiedCount + 1 /></#if>
        </#list>
        <p>
          <#if itemsList?size gt 0>
            ${text_acknowledge} ${modifiedCount} students.
            <#if hasErrors>
              ${text_problems}
            </#if>
          <#else>
            Your ${noun_verb_passive} but do not appear to contain any rows that look like
            marks. ${column_headings_warning}
          </#if>
        </p>
      </@spring.bind>

      <@spring.bind path="marks">
        <#assign itemList=status.actualValue />
        <#if itemList?size gt 0>
          <table class="table upload-marks-preview">
            <tr>
              <th>University ID</th>
              <#if assignment.showSeatNumbers>
                <th>Seat number</th>
              </#if>
              <th>Mark</th>
              <th>Grade</th>
              <#list assignment.feedbackFields as field>
                <th>${field.label}</th>
              </#list>
            </tr>
            <#list itemList as item>
              <@spring.nestedPath path="marks[${item_index}]">
                <tr>
                  <@f.hidden path="id" />
                  <@f.hidden path="actualMark" />
                  <@f.hidden path="actualGrade" />
                  <input type="hidden" name="marks[${item_index}].isValid" value="<@spring.bind path="valid">${status.value}</@spring.bind>" />
                  <td>
                    <@spring.bind path="id">${status.value}</@spring.bind>
                    <@f.errors path="id" cssClass="error" />
                    <#if item.modified>
                      <span class="warning">Feedback and/or marks have already been uploaded for this student. These will be overwritten when you click confirm.</span>
                    </#if>
                  </td>
                  <#if assignment.showSeatNumbers && item.user(assignment)??>
                    <td>${assignment.getSeatNumber(item.user(assignment))!""}</td>
                  </#if>
                  <td>
                    <@spring.bind path="actualMark">${status.value}</@spring.bind>
                    <@f.errors path="actualMark" cssClass="error" />
                  </td>
                  <td>
                    <@spring.bind path="actualGrade">${status.value}</@spring.bind>
                    <@f.errors path="actualGrade" cssClass="error" />
                  </td>
                  <#list assignment.feedbackFields as field>
                    <@f.hidden path="fieldValues[${field.name}]" />
                    <td>
                      <@spring.bind path="fieldValues[${field.name}]">${status.value!}</@spring.bind>
                      <@f.errors path="fieldValues[${field.name}]" cssClass="error" />
                    </td>
                  </#list>
                </tr>
              </@spring.nestedPath>
            </#list>
          </table>
        </#if>
      </@spring.bind>

      <div class="submit-buttons form-actions fix-footer">
        <input type="hidden" name="confirm" value="true">
        <input class="btn btn-primary" type="submit" value="Confirm">
        <a class="btn btn-default" href="${cancelUrl}">Cancel</a>
      </div>
    </@f.form>
  </div>

</#escape>