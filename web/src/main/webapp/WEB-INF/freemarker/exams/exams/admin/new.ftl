<#escape x as x?html>

<#if features.exams>

	<#import "*/sits_groups.ftl" as sits_groups />

	<h1>Create exam for <@fmt.module_name module /></h1>

	<#assign createExamUrl><@routes.exams.createExam module academicYear /></#assign>

	<@f.form id="newExamForm" method="post" action="${createExamUrl}" commandName="command">

		<#include "_common_fields.ftl" />

		<div class="">
			<input type="submit" value="Create" class="btn btn-primary">
			<a class="btn btn-default" href="<@routes.exams.moduleHomeWithYear module=module academicYear=academicYear />">Cancel</a>
		</div>
	</@f.form>

</#if>
</#escape>

<script>
	jQuery(function ($) {

		$('#newExamForm').on('submit',function (){
			$('.upstreamGroupsHidden').remove();
			$('.upstreamGroups:checked').each(function(i,input) {
				$('<input>', { 'class': 'upstreamGroupsHidden', type: 'hidden', name: 'upstreamGroups['+i+']', value:input.value }).appendTo('#sits-table');
			});
		});

	});
</script>