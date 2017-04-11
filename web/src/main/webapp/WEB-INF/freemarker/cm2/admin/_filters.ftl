<#macro filter name path placeholder currentFilter allItems validItems=allItems prefix="" customPicker="">
	<@spring.bind path=path>
	<div id="${name}-filter" class="btn-group filter<#if currentFilter == placeholder> empty</#if>">
		<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">
			<span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
			<span class="caret"></span>
		</a>
		<div class="dropdown-menu filter-list">
			<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
			<ul>
				<#if customPicker?has_content>
					<li>${customPicker}</li>
				</#if>
				<#if allItems?has_content>
					<#list allItems as item>
						<#local isValid = (allItems?size == validItems?size)!true />
						<#if !isValid>
							<#list validItems as validItem>
								<#if ((validItem.id)!0) == ((item.id)!0)>
									<#local isValid = true />
								</#if>
							</#list>
						</#if>
						<li class="check-list-item" data-natural-sort="${item_index}">
							<label class="checkbox <#if !isValid>disabled</#if>">
								<#nested item isValid/>
							</label>
						</li>
					</#list>
				<#else>
					<li><small class="very-subtle" style="padding-left: 5px;">N/A for this department</small></li>
				</#if>
			</ul>
		</div>
	</div>
	</@spring.bind>
</#macro>

<#macro current_filter_value path placeholder><#compress>
	<@spring.bind path=path>
		<#if status.actualValue?has_content>
			<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
		<#else>
		${placeholder}
		</#if>
	</@spring.bind>
</#compress></#macro>

<#function contains_by_code collection item>
	<#list collection as c>
		<#if c.code == item.code>
			<#return true />
		</#if>
	</#list>
	<#return false />
</#function>

<#function contains_by_db_value collection item>
	<#list collection as c>
		<#if c.dbValue == item.dbValue>
			<#return true />
		</#if>
	</#list>
	<#return false />
</#function>