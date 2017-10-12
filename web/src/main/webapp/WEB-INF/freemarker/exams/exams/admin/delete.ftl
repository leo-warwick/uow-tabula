<#escape x as x?html>

	<h1>Delete exam for <@fmt.module_name exam.module /></h1>

	<#assign submitAction><@routes.exams.deleteExam exam /></#assign>
	<@f.form method="post" action="${submitAction}" commandName="command">
		<h2>${exam.name} (${exam.academicYear.label})</h2>

		<!-- global errors -->
		<@f.errors cssClass="error" />

		<p>
			You can delete an exam if it has been created in error.
		</p>

		<@bs3form.checkbox path="confirm">
			<@f.checkbox path="confirm" id="confirmCheck" />
			<strong> I definitely will not need this exam again and wish to delete it entirely.</strong>
		</@bs3form.checkbox>

		<@bs3form.form_group>
			<input type="submit" value="Delete" class="btn btn-danger">
			<a href="<@routes.exams.moduleHomeWithYear module=exam.module academicYear=exam.academicYear />" class="btn btn-default">Cancel</a>
		</@bs3form.form_group>

	</@f.form>

	<script type="text/javascript">
		jQuery(function($){
			$('#confirmCheck').change(function(){
				$('.submit-buttons input[type=submit]').prop('disabled', !this.checked).toggleClass('disabled', !this.checked);
			});
			$('.submit-buttons input[type=submit]').prop('disabled',true).addClass('disabled');
		})
	</script>

</#escape>