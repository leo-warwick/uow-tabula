<#ftl strip_text=true />
<#--

Macros for customised form elements, containers and more complex pickers.

-->

<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#compress>
<#escape x as x?html>

<#macro errors path>
	<div class="has-error">
		<@f.errors path=path cssClass="help-block" />
	</div>
</#macro>

<#macro label path="" for="" cssClass="">
	<#if path?has_content>
		<@f.label path="${path}" for="${path}" cssClass="control-label ${cssClass}" ><#compress><#nested/></#compress></@f.label>
	<#elseif for?has_content>
		<label for="${for}" class="control-label ${cssClass}"><#compress><#nested/></#compress></label>
	<#else>
		<label class="control-label ${cssClass}"><#compress><#nested/></#compress></label>
	</#if>
</#macro>

<#macro form_group path="" checkbox=false radio=false cssClass="">
	<#local errorClass="" />
	<#if path?has_content>
		<@spring.bind path=path>
			<#if status.error>
				<#local errorClass = "has-error" />
			</#if>
		</@spring.bind>
	</#if>
	<div class="<#compress>form-group <#if checkbox>checkbox</#if> <#if radio>radio</#if> ${cssClass} ${errorClass}</#compress>">
		<#nested />
	</div>
</#macro>

<#macro labelled_form_group path="" labelText="" help="" cssClass="">
	<@form_group path=path cssClass=cssClass>
		<#if labelText?has_content>
			<@label path=path><#compress><#noescape>${labelText}</#noescape></#compress></@label>
		</#if>
		<#if path?has_content>
			<@spring.bind path=path>
				<#nested />
			</@spring.bind>
			<@errors path=path />
		<#else>
			<#nested />
		</#if>
		<#if help?has_content><div class="help-block">${help}</div></#if>
	</@form_group>
</#macro>

<#macro radio>
	<div class="radio">
		<label><#compress><#nested /></#compress></label>
	</div>
</#macro>

<#macro checkbox path="">
	<#local errorClass="" />
	<#if path?has_content>
		<@spring.bind path=path>
			<#if status.error>
				<#local errorClass = "has-error" />
			</#if>
		</@spring.bind>
	</#if>
	<div class="checkbox ${errorClass}">
		<label><#compress>
			<#nested />
			<#if path?has_content>
			<@errors path=path />
		</#if>
		</#compress></label>
	</div>
</#macro>

<#macro selector_check_all>
	<div class="check-all">
		<input type="checkbox" class="collection-check-all">
	</div>
</#macro>

<#macro selector_check_row name value readOnly=false><#compress>
	<input type="checkbox" class="collection-checkbox<#if readOnly>disabled</#if>" name="${name}" value="${value}" <#if readOnly>disabled</#if>>
</#compress></#macro>

<#macro filewidget types basename multiple=true max=10 labelText="File" maxFileSize="" required=false>
<#-- <#local command=.vars[Request[commandVarName]] /> -->
	<#local elementId="file-upload-${basename?replace('[','')?replace(']','')?replace('.','-')}"/>
	<@labelled_form_group basename labelText>
		<@errors path="${basename}" />
		<@errors path="${basename}.upload" />
		<@errors path="${basename}.attached" />
		<@spring.bind path="${basename}">
			<#local f=status.actualValue />
			<div id="${elementId}">
				<#if f.exists>
					<#list f.attached as attached>
						<#local uploadedId=attached.id />
						<div class="hidden-attachment" id="attachment-${uploadedId}">
							<input type="hidden" name="${basename}.attached" value="${uploadedId}">
						${attached.name} <a id="remove-attachment-${uploadedId}" href="#">Remove attachment</a>
						</div>
						<div id="upload-${uploadedId}" style="display:none">

						</div>
					</#list>
				</#if>

				<#if multiple>
					<input type="file" id="${basename}.upload" name="${basename}.upload" multiple>
					<noscript>
						<#list (2..max) as i>
							<br><input type="file" name="${basename}.upload">
						</#list>
					</noscript>
				<#else>
					<input type="file" id="${basename}.upload" name="${basename}.upload" >
				</#if>
			</div>

			<small class="very-subtle help-block">
				<#if required>
					<#if !multiple || max=1>
						You must attach one file.
					<#else>
						You must attach at least one file. Up to <@fmt.p max "attachment" /> allowed.
					</#if>
				<#else>
					<#if !multiple || max=1>One attachment allowed.<#else>Up to <@fmt.p max "attachment" /> allowed.</#if>
				</#if>
				<#if types?size &gt; 0>
					File types allowed: <#list types as type>${type}<#if type_has_next>, </#if></#list>.
				</#if>
				<#if maxFileSize?has_content>
					Maximum file size per file: ${maxFileSize}MB
				</#if>
				<#if multiple && max!=1>
					<span id="multifile-column-description" class="muted"><#include "/WEB-INF/freemarker/multiple_upload_help.ftl" /></span>
				</#if>
			</small>

			<script><!--

			jQuery(function($){
				var $container = $('#${elementId}'),
						$file = $container.find('input[type=file]'),
						$addButton;
				if (Supports.multipleFiles) {
					// nothing, already works
				} else {
					// Add button which generates more file inputs
					$addButton = $('<a>').addClass('btn btn-mini').append($('<i class="fa fa-plus"></i>').attr('title','Add another attachment'));
					$addButton.click(function(){
						$addButton
								.before($('<br/>'))
								.before($('<input type="file">').attr('name',"${basename}.upload"));
						if ($container.find('input[type=file]').length >= ${max}) {
							$addButton.hide(); // you've got enough file input thingies now.
						}
					});
					$file.after($addButton);
				}

				$container.find('.hidden-attachment a').click(function(ev){
					ev.preventDefault();
					$(this).parent('.hidden-attachment').remove();
					if ($addButton && $container.find('input[type=file],input[type=hidden]').length < ${max}) {
						$addButton.show();
					}
					return false;
				});
			});

			//--></script>
		</@spring.bind>
	</@labelled_form_group>
</#macro>

<#--
	flexipicker

	A user/group picker using Bootstrap Typeahead
	Combination of the userpicker and
	the flexipicker in Sitebuilder

	Params
	name: If set, use this as the form name and don't bind values from spring.
	path: If set, bind to this Spring path and use its values.
	list: whether we are binding to a List in Spring - ignored if using name instead of path
	multiple: whether the UI element will grow to allow multiple items
	object: True if binding to User objects, otherwise binds to strings.
	    This might not actually work - better to register a property editor for the field
	    if you are binding to and from Users.
	delete_existing: whether the UI element will allow you to remove existing users that are pre-populated

-->
<#macro flexipicker path="" list=false object=false name="" htmlId="" cssClass="" placeholder="" includeEmail="false" includeGroups="false" includeUsers="true" membersOnly="false" multiple=false auto_multiple=true delete_existing=true>
	<#if name="">
		<@spring.bind path=path>
		<#-- This handles whether we're binding to a list or not but I think
				it might still be more verbose than it needs to be. -->
			<#local ids=[] />
			<#if status.value??>
				<#if list && status.actualValue?is_sequence>
					<#local ids=status.actualValue />
				<#elseif object>
					<#local ids=[status.value.userId] />
				<#elseif status.value?is_string>
					<#local ids=[status.value] />
				</#if>
			</#if>
			<@render_flexipicker expression=status.expression value=ids cssClass=cssClass htmlId=htmlId placeholder=placeholder includeEmail=includeEmail includeGroups=includeGroups includeUsers=includeUsers membersOnly=membersOnly multiple=multiple auto_multiple=auto_multiple delete_existing=delete_existing><#nested /></@render_flexipicker>
		</@spring.bind>
	<#else>
		<@render_flexipicker expression=name value=[] cssClass=cssClass htmlId=htmlId placeholder=placeholder includeEmail=includeEmail includeGroups=includeGroups includeUsers=includeUsers membersOnly=membersOnly multiple=multiple auto_multiple=auto_multiple delete_existing=delete_existing><#nested /></@render_flexipicker>
	</#if>
</#macro>

<#macro render_flexipicker expression cssClass value multiple auto_multiple placeholder includeEmail includeGroups includeUsers membersOnly delete_existing htmlId="">
	<#if multiple><div class="flexi-picker-collection" data-automatic="${auto_multiple?string}"></#if>
	<#local nested><#nested /></#local>
	<#-- List existing values -->
	<#if value?? && value?size gt 0>
		<#list value as id>
			<div class="flexi-picker-container <#if nested?has_content>input-group</#if>"><#--
			--><input type="text" class="flexi-picker form-control ${cssClass}"
					name="${expression}" id="${htmlId}" placeholder="${placeholder}"
					data-include-users="${includeUsers}" data-include-email="${includeEmail}"
					data-include-groups="${includeGroups}" data-members-only="${membersOnly}"
					data-prefix-groups="webgroup:" data-can-delete="${delete_existing?c}" value="${id}" data-type="" autocomplete="off"
				/>
				<#noescape>${nested}</#noescape>
			</div>
		</#list>
	</#if>

	<#if !value?has_content || (multiple && auto_multiple)>
		<div class="flexi-picker-container <#if nested?has_content>input-group</#if>"><#--
			--><input type="text" class="flexi-picker form-control ${cssClass}"
				name="${expression}" id="${htmlId}" placeholder="${placeholder}"
				data-include-users="${includeUsers}" data-include-email="${includeEmail}" data-include-groups="${includeGroups}"
				data-members-only="${membersOnly}" data-can-delete="true"
				data-prefix-groups="webgroup:" data-type="" autocomplete="off"
			/>
			<#noescape>${nested}</#noescape>
		</div>
	</#if>

	<#if multiple></div></#if>
</#macro>

</#escape>
</#compress>
