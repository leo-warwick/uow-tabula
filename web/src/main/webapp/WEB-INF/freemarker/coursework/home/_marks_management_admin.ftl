<#escape x as x?html>
	<#if can.do_scopeless("Marks.MarksManagement") && features.queueFeedbackForSits>
		<div class="btn-group marks-management-closure ">
			<a class="btn btn-primary" href="<@routes.coursework.marksmanagementdepts />" data-title="Manage Marks Closure" data-container="body">Manage Marks Closure</a>
		</div>
	</#if>
</#escape>