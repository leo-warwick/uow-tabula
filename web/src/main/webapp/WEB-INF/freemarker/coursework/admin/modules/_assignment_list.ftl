<@form.row path "">
	<@form.label path>
		Assignments
		<label>
			<input type="checkbox" class="collection-check-all">
			<span class="very-subtle">Select / unselect all</span>
		</label>
	</@form.label>

	<@form.field cssClass="">
		<#list modules as module>
			<#if map[module.code]??>
				<#if showSubHeadings!false>
					<h6 class="module-split">
						<small><@fmt.module_name module /></small>
					</h6>
				</#if>
				<#list map[module.code] as assignment>
					<div class="checkbox"><#compress>
						<label class="checkbox">
							<input type="checkbox" class="collection-checkbox" name="assignments" value="${assignment.id}">
							${assignment.name}
							<small><span class="muted">${assignment.academicYear.toString}</span></small>
						</label>
					</#compress></div>
				</#list>
			</#if>
		</#list>
	</@form.field>
</@form.row>