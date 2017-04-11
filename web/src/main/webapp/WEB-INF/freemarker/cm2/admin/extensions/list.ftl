<#include "../_filters.ftl" />
<#escape x as x?html><#compress>
	<h1>Extensions</h1>

	<div class="filters btn-group-group well well-small well-sm">
		<#assign formAction><@routes.cm2.filterExtensions /></#assign>
		<@f.form
			method="GET"
			action="${formAction}"
			commandName="command"
			class="form-inline"
		>
			<button type="button" class="clear-all btn btn-xs">Clear all filters</button>

			<#-- Department filter-->
			<#assign placeholder = "All departments" />
			<#assign currentfilter><@current_filter_value "command.departments" placeholder; department>${department.code}</@current_filter_value></#assign>
			<@filter "departments" "command.departments" placeholder currentfilter command.allDepartments; department>
				<input
					type="checkbox"
					name="${status.expression}"
					value="${department.code}"
					data-related-filter="modules"
					data-short-value="${department.name}"
					${contains_by_code(command.departments, department)?string('checked','')}
				>
				${department.name}
			</@filter>

			<#-- Module filter-->
			<#assign placeholder = "All modules" />
			<#assign currentfilter><@current_filter_value "command.modules" placeholder; module>${module.code}</@current_filter_value></#assign>
			<#assign moduleCustomPicker>
				<div class="module-search input-append input-group">
					<input class="module-search-query module-picker module prevent-reload form-control" type="text" value="" placeholder="Search for a module" />
					<span class="add-on input-group-addon"><i class="icon-search fa fa-search"></i></span>
				</div>
			</#assign>
			<@filter name="modules" path="command.modules" placeholder=placeholder currentFilter=currentfilter allItems=command.allModules customPicker=moduleCustomPicker; module>
				<input
					type="checkbox"
					name="${status.expression}"
					value="${module.code}"
					data-related-value="${module.adminDepartment.code}"
					data-short-value="${module.code?upper_case}"
					${contains_by_code(command.modules, module)?string('checked','')}
				>
				${module.code?upper_case} ${module.name}
			</@filter>

			<#-- State filter-->
			<#assign placeholder = "All states" />
			<#assign currentfilter><@current_filter_value "command.states" placeholder; state>${state.description}</@current_filter_value></#assign>
			<@filter "states" "command.states" placeholder currentfilter command.allStates; state>
				<input
					type="checkbox"
					name="${status.expression}"
					value="${state.dbValue}"
					data-short-value="${state.description}"
					${contains_by_db_value(command.states, state)?string('checked','')}
				>
				${state.description}
			</@filter>

			<#-- Time filter-->
			<#assign placeholder = "All dates" />
			<#assign currentfilter><@current_filter_value "command.times" placeholder; time>${time.code}</@current_filter_value></#assign>
			<@filter "times" "command.times" placeholder currentfilter command.allTimes; time>
				<input
					type="checkbox"
					name="${status.expression}"
					value="${time.code}"
					data-short-value="${time.code}"
					${contains_by_code(command.times, time)?string('checked','')}
				>
				${time.code}
			</@filter>

		</@f.form>
	</div>
	<div class="filter-results">
		<#include "_filter_results.ftl" />
	</div>

</#compress></#escape>
