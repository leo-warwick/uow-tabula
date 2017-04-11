<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#escape x as x?html>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3>Feedback report for ${department.name}</h3>
	</div>

	<@f.form method="post" action="${url('/coursework/admin/department/${department.code}/reports/feedback/')}" commandName="feedbackReportCommand">
	<div class="modal-body">
		<p>Generate report for assignments with closing dates </p>

        <@form.labelled_row "startDate" "From">
            <@f.input id="picker0" path="startDate" cssClass="date-time-picker" value="${startDate}" />
        </@form.labelled_row>

        <@form.labelled_row "endDate" "To">
            <@f.input id="picker0" path="endDate" cssClass="date-time-picker" value="${endDate}" />
        </@form.labelled_row>
	</div>
	<div class="modal-footer">
		<input type="submit" value="Confirm" class="btn btn-primary">
		<a data-dismiss="modal" class="close-model btn" href="#">Cancel</a>
	</div>

	</@f.form>
</#escape>