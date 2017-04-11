<#if features.exams>
	<#escape x as x?html>

	<#import "*/sits_groups.ftl" as sits_groups />

	<h1>Edit exam for <@fmt.module_name exam.module /></h1>

	<#assign updateExamUrl><@routes.exams.editExam exam /></#assign>

	<@f.form id="exams.editExamForm" method="post" action="${updateExamUrl}" commandName="command">

		<#include "_common_fields.ftl" />

		<div>
			<input type="submit" value="Update" class="btn btn-primary">
			<a class="btn btn-default" href="<@routes.exams.moduleHomeWithYear module=exam.module academicYear=exam.academicYear />">Cancel</a>
		</div>

	</@f.form>

	</#escape>
</#if>

<script>
	jQuery(function ($) {
		$('#exams.editExamForm').on('submit',function (){
			$('.upstreamGroupsHidden').remove();
			$('.upstreamGroups:checked').each(function(i,input) {
				$('<input>', { 'class': 'upstreamGroupsHidden', type: 'hidden', name: 'upstreamGroups['+i+']', value:input.value }).appendTo('#sits-table');
			});
		});

	});
</script>