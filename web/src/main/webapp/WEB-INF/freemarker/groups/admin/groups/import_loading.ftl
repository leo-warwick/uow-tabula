<#escape x as x?html>
	<#function route_function dept>
		<#local result><@routes.groups.import_groups_for_year dept academicYear /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader title="Import small groups from Syllabus+" route_function=route_function preposition="for"/>

	<#assign post_url><@routes.groups.import_groups department /></#assign>
	<@f.form method="post" id="import-form" action="${post_url}" commandName="command">
		<input type="hidden" name="action" value="populate" />
		<@f.hidden path="academicYear" />
	</@f.form>

	<div class="very-subtle">Loading, please wait&hellip;</div>

	<script type="text/javascript">
		jQuery(function($) {
			$('#import-form').submit();
		});
	</script>
</#escape>