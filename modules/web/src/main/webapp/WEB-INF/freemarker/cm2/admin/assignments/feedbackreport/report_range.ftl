<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>
	<@modal.wrapper>
		<@modal.header>
			<h3>Feedback report for ${department.name}</h3>
		</@modal.header>

		<div class="fix-area">
			<#assign actionUrl><@routes.cm2.feedbackreport department /></#assign>
			<@f.form method="post" class="form-inline" action=actionUrl commandName="feedbackReportCommand" cssClass="dirty-check">
				<@modal.body>
					<p>Generate report for assignments with closing dates </p>

					<@bs3form.labelled_form_group labelText="From">
						<@f.input id="picker0" path="startDate" cssClass="form-control date-time-picker" value="${startDate}" />
					</@bs3form.labelled_form_group>

					<@bs3form.labelled_form_group labelText="To">
						<@f.input id="picker0" path="endDate" cssClass="form-control date-time-picker" value="${endDate}" />
					</@bs3form.labelled_form_group>
				</@modal.body>
			<@modal.footer>
				<div class="submit-buttons">
					<input type="submit" value="Confirm" class="btn btn-primary">
					<a data-dismiss="modal" class="close-model btn-default" href="#">Cancel</a>
				</div>
			</@modal.footer>
			</@f.form>
		</div>
	</@modal.wrapper>
</#escape>