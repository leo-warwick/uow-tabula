<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

	<#list moduleInfo.assignments as info>
		<span id="admin-assignment-container-${info.assignment.id}">
			<@components.admin_assignment_info info skeleton=true/>
		</span>

	</#list>

	<script type="text/javascript">
		jQuery(function($) {

			function getAssignmentDetails(containerId) {

				var detailUrl = $('#' + containerId + ' .item-info').data('detailurl');

				$.ajax({
					url: detailUrl,
					statusCode: {
						403: function () {
							$('#' + containerId).html("<p class='text-error'><i class='icon-warning-sign'></i> Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p class='text-error'>Refresh the page and try again. If it remains a problem, please let us know using the comments link on the edge of the page.</p>");
						}
					},
					success: function (data) {
						$('#' + containerId).html(data);
					},
					error: function () {
						$('#' + containerId).html("<p>No data is currently available. Please check that you are signed in.</p>");
					},
					complete: function () {
						if (a = $assignmentsToUpdate.shift()) {
							getAssignmentDetails(a);
						}
					}
				})
			};

			var $assignmentsToUpdate = [];

			function populateAssignmentDetailsQueue() {
				$('.item-info').each(function() {
					$assignmentsToUpdate.push(this.parentElement.id);
				});
			}

			function processAssignmentDetailsQueue() {
				var ajaxRequestsLimit = 5;
				for (var i = 0; i < ajaxRequestsLimit; i++){
					if (assignmentContainerId = $assignmentsToUpdate.shift()) {
						getAssignmentDetails(assignmentContainerId);
					}
				}
			}

			populateAssignmentDetailsQueue();
			processAssignmentDetailsQueue();

		});
	</script>

</#escape>