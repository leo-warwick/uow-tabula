<#assign department=command.department />

<#if saved??>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		<p>Changes saved.</p>
	</div>
</#if>

<#function route_function dept>
	<#local result><@routes.admin.sortmodules dept /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader "Arrange ${objectName}s" route_function "for" />

<noscript>
	<div class="alert">This page requires Javascript.</div>
</noscript>

<p>Drag ${objectName}s by their <i class="fa fa-bars"></i> handles to move them between departments. To select multiple departments,
	drag a box from one ${objectName} name to another. You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key and drag to add to a selection.</p>

<@spring.hasBindErrors name="${commandName}">
	<#if errors.hasErrors()>
	<div class="alert alert-danger">
		<h3>Some problems need fixing</h3>
		<#if errors.hasGlobalErrors()>
			<#list errors.globalErrors as e>
				<div><@spring.message message=e /></div>
			</#list>
		<#else>
			<div>See the errors below.</div>
		</#if>
	</div>
	</#if>
</@spring.hasBindErrors>

<@f.form commandName="${commandName}" action="${submitUrl}">
	<div class="tabula-dnd">
		<#macro renderObjects department objects>
			<div class="drag-target clearfix form-group">
				<h1>${department.name}</h1>
				<ul class="drag-list full-width" data-bindpath="mapping[${department.code}]">
					<#list objects as object>
						<li class="label label-default" title="${object.name}">
							<i class="fa fa-bars"></i>
							${object.code?upper_case}
							<input type="hidden" name="mapping[${department.code}][${object_index}]" value="${object.id}" />
						</li>
					</#list>
				</ul>
			</div>
		</#macro>

		<#list command.departments as dept>
			<@renderObjects dept command.mappingByCode[dept.code]![] />
		</#list>

		<div class="form-group">
			<input id="sort-object-submit" class="btn btn-primary" type="submit" value="Save changes" />
		</div>
	</div>
</@f.form>