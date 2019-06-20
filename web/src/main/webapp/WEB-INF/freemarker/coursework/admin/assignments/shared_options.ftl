<#--

	Form shown in a modal popup to

	This is currently also included by _fields.ftl since the forms to add/edit a single assignment
	use the same fields.

-->
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

  <#assign command = sharedAssignmentPropertiesForm />

  <#assign submitUrl><@routes.coursework.setupSitsAssignmentsSharedOptions department /></#assign>
  <@f.form method="post" action=submitUrl modelAttribute="sharedAssignmentPropertiesForm" cssClass="form-horizontal">

    <#if submitted?? && submitted && !hasErrors>
      <span class="ajax-response" data-status="success"></span>
    </#if>

  <#-- This is also included by _fields.ftl -->
    <#include "_common_fields.ftl" />
    <#include "_submissions_common_fields.ftl" />

  </@f.form>

</#escape>