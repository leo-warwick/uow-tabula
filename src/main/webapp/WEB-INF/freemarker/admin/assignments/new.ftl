<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#-- 
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines. 
-->

<h1>Create assignment for <@fmt.module_name module /></h1>
<#assign commandName="addAssignmentCommand" />
<#assign command=addAssignmentCommand />
<@f.form method="post" action="/admin/module/${module.code}/assignments/new" commandName=commandName cssClass="form-horizontal">

<#if command.prefilled>
<div class="alert alert-success">
Some fields have been pre-filled from another recently created assignment for convenience.
</div>
</#if>

<@f.errors cssClass="error form-errors" />

<#assign newRecord=true />

<#include "_fields.ftl" />

<div class="submit-buttons">
<input type="submit" value="Create" class="btn btn-primary">
or <a class="btn" href="<@routes.depthome module=module />">Cancel</a>
</div>

</@f.form>

</#escape>