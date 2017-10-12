<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
	<@cm2.assignmentHeader "Delete assignment" assignment "" />

	<#assign submitUrl><@routes.cm2.assignmentdelete assignment /></#assign>
	<@f.form method="post" action=submitUrl commandName="deleteAssignmentCommand">
		<!-- global errors -->
		<@f.errors cssClass="error" />

		<p>
			You can delete an assignment if it's been created in error.
		</p>

		<@f.errors path="confirm" cssClass="error" />
		<@bs3form.labelled_form_group path="confirm" labelText="">
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" id="confirmCheck" />
				<strong> I definitely will not need this assignment again and wish to delete it entirely.</strong>
			</@bs3form.checkbox>
		</@bs3form.labelled_form_group>


		<div class="submit-buttons">
			<input type="submit" value="Delete" class="btn btn-danger">
			<#if assignment.cm2Assignment>
				<#assign detailsUrl><@routes.cm2.editassignmentdetails assignment /></#assign>
			<#else>
				<#assign detailsUrl><@routes.coursework.assignmentedit assignment /></#assign>
			</#if>
			<a href="${detailsUrl}" class="btn btn-default">Cancel</a>
		</div>
	</@f.form>

	<script>
		jQuery(function($){
			$('#confirmCheck').change(function(){
				$('.submit-buttons input[type=submit]').prop('disabled', !this.checked).toggleClass('disabled', !this.checked);
			});
			$('.submit-buttons input[type=submit]').prop('disabled',true).addClass('disabled');
		})
	</script>

</#escape>