<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>


<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/feedback/new" commandName="addFeedbackCommand">

<h1>Submit feedback for ${assignment.name}</h1>

<@form.labelled_row "uniNumber" "Student university number">
<@f.input path="uniNumber" cssClass="text" />
</@form.labelled_row>

<@form.filewidget basename="file"  />

<div class="submit-buttons">
<input class="btn btn-primary" type="submit" value="Submit">
</div>
</@f.form>

</#escape>