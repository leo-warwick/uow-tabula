<#ftl strip_text=true />
<#--

Macros for customised form elements, containers and more complex pickers.

Include by default as "form", e.g.

<@form.userpicker path="users" />

-->

<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#compress>
<#escape x as x?html>

<#-- Return the textual value of the given command property -->
<#function getvalue path>
	<@spring.bind path=path>
		<#return status.value />
	</@spring.bind>
</#function>

<#-- Return whether this given command property is set -->
<#function hasvalue path>
	<@spring.bind path=path>
		<#return status.actualValue?? && status.value != "" /><#-- use actualValue as value will usually be empty string for an empty value -->
	</@spring.bind>
</#function>

<#macro row path="" cssClass="" defaultClass="control-group form-group">

	<#if cssClass="">
		<#local baseClass=defaultClass/>
	<#else>
		<#local baseClass=defaultClass + " " + cssClass />
	</#if>
	<#if path="">
		<div class="${baseClass}"><#nested/></div>
	<#else>
		<@spring.bind path=path>
			<#if status.error>
				<#local baseClass=baseClass + " error has-error" />
			</#if>
			<div class="${baseClass}"><#nested/></div>
		</@spring.bind>
	</#if>
</#macro>

<#macro field cssClass="">
	<#if cssClass="">
		<#local baseClass="controls"/>
	<#else>
		<#local baseClass="controls " + cssClass />
	</#if>
	<div class="${baseClass}">
		<#nested/>
	</div>
</#macro>

<#macro label path="" for="" checkbox=false clazz="">
<#if checkbox>
	<#local clazz="${clazz} checkbox"?trim />
<#else>
	<#local clazz="${clazz} control-label col-sm-2"?trim />
</#if>
<#if path!="">
  <@f.label path="${path}" for="${path}" cssClass="${clazz}" ><#nested/></@f.label>
<#elseif for!="">
  <label for="${for}" class="${clazz}"><#nested /></label>
<#else>
  <label class="${clazz}"><#nested /></label>
</#if>
</#macro>
<#assign _label=label /><#-- to resolve naming conflicts -->

<#macro selector_check_all>
<div class="check-all checkbox">
	<label>
		<input type="checkbox" class="collection-check-all">
	</label>
</div>
</#macro>

<#macro selector_check_row name value><div class="checkbox"><#compress>
	<input type="checkbox" class="collection-checkbox" name="${name}" value="${value}">
</#compress></div></#macro>

<#macro errors path><@f.errors path=path cssClass="error help-inline help-block" /></#macro>

<#macro labelled_row path label cssClass="" help="" fieldCssClass="" labelCss="" helpPopover="" helpPopoverAfter="">
<@row path=path cssClass=cssClass>
	<@_label path=path clazz=labelCss >
		${label}
		<#if helpPopover?has_content>
			<@fmt.help_popover id="help-${path}" content="${helpPopover}" />
		</#if>
	</@_label>
	<@field cssClass=fieldCssClass>
		<#nested />
		<@errors path=path />
		<#if help?has_content><div class="help-block">${help}</div></#if>
	</@field>
	<#if helpPopoverAfter?has_content>
		<div class="col-md-1 control-label"><div class="pull-left">
			<@fmt.help_popover id="help-after-${path}" content="${helpPopoverAfter}" />
		</div></div>
	</#if>
</@row>
</#macro>

<#--

Render a text field with user picker.

To bind with Spring:
<@userpicker path="yourBindPath" />

To not bind:
<@userpicker name="paramName" />

-->
<#macro userpicker path="" name="" list=false object=false multiple=false spanClass="span2" htmlId="">
<#if name="">
	<@spring.bind path=path>
	<#-- This handles whether we're binding to a list or not but I think
		it might still be more verbose than it needs to be. -->
	<#local ids=[] />
	<#if status.actualValue??>
		<#if list && status.actualValue?is_sequence>
			<#local ids=status.actualValue />
		<#elseif object>
			<#local ids=[status.actualValue.userId] />
		<#elseif status.actualValue?is_string>
			<#local ids=[status.actualValue] />
		</#if>
	</#if>
	<@render_userpicker expression=status.expression value=ids multiple=multiple spanClass=spanClass htmlId=htmlId/>
	</@spring.bind>
<#else>
	<@render_userpicker expression=name value=[] multiple=multiple spanClass=spanClass htmlId=htmlId/>
</#if>
</#macro>

<#macro render_userpicker expression value multiple spanClass="span2" htmlId="">
	<#if multiple><div class="user-picker-collection"></#if>



	<#-- List existing values -->
	<#if value?? && value?size gt 0>
	<#list value as id>
		<div class="user-picker-container input-prepend input-append"><span class="add-on"><i class="icon-user fa fa-user"></i></span><#--
		--><input type="text" class="user-code-picker ${spanClass}" name="${expression}" value="${id}" id="${htmlId}">
		</div>
	</#list>
	</#if>

	<#if !value?has_content || multiple>
	<#-- an empty field for entering new values in any case -->
	<div class="user-picker-container input-prepend input-append"><span class="add-on"><i class="icon-user fa fa-user"></i></span><#--
	--><input type="text" class="user-code-picker ${spanClass}" name="${expression}" id="${htmlId}">
	</div>
	</#if>

	<#if multiple></div></#if>
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

-->
<#macro flexipicker path="" list=false object=false name="" htmlId="" cssClass="" placeholder="" includeEmail="false" includeGroups="false" includeUsers="true" membersOnly="false" multiple=false auto_multiple=true>
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
		<@render_flexipicker expression=status.expression value=ids cssClass=cssClass htmlId=htmlId placeholder=placeholder includeEmail=includeEmail includeGroups=includeGroups includeUsers=includeUsers membersOnly=membersOnly multiple=multiple auto_multiple=auto_multiple />
	</@spring.bind>
<#else>
	<@render_flexipicker expression=name value=[] cssClass=cssClass htmlId=htmlId placeholder=placeholder includeEmail=includeEmail includeGroups=includeGroups includeUsers=includeUsers membersOnly=membersOnly multiple=multiple auto_multiple=auto_multiple />
</#if>
</#macro>

<#macro render_flexipicker expression cssClass value multiple auto_multiple placeholder includeEmail includeGroups includeUsers membersOnly htmlId="">
	<#if multiple><div class="flexi-picker-collection" data-automatic="${auto_multiple?string}"></#if>

	<#-- List existing values -->
		<#if value?? && value?size gt 0>
			<#list value as id>
				<div class="flexi-picker-container input-prepend input-group"><span class="input-group-addon add-on"><i class="icon-user fa fa-user"></i></span><#--
			--><input type="text" class="text flexi-picker form-control ${cssClass}"
					   name="${expression}" id="${htmlId}" placeholder="${placeholder}"
					   data-include-users="${includeUsers}" data-include-email="${includeEmail}" data-include-groups="${includeGroups}"
					   data-members-only="${membersOnly}"
					   data-prefix-groups="webgroup:" value="${id}" data-type="" autocomplete="off"
						/>
				</div>
			</#list>
		</#if>

		<#if !value?has_content || (multiple && auto_multiple)>
			<div class="flexi-picker-container input-prepend input-group"><span class="input-group-addon add-on"><i class="icon-user fa fa-user"></i></span><#--
		--><input   type="text" class="text flexi-picker form-control ${cssClass}"
					name="${expression}" id="${htmlId}" placeholder="${placeholder}"
					data-include-users="${includeUsers}" data-include-email="${includeEmail}" data-include-groups="${includeGroups}"
					data-members-only="${membersOnly}"
					data-prefix-groups="webgroup:" data-type="" autocomplete="off"
					/>
			</div>
		</#if>

	<#if multiple></div></#if>
</#macro>

<#--
	Create a file(s) upload widget, binding to an UploadedFile object.
	For multiple file support, it will use the HTML5 attribute if available,
	otherwise it will degrade to other mechanisms.

	basename - bind path to the UploadedFile.
	multiple - whether it should be possible to upload more than one file.
-->
<#macro filewidget types basename multiple=true max=10 labelText="File" maxFileSize="" required=false>
	<#-- <#local command=.vars[Request[commandVarName]] /> -->
	<#local elementId="file-upload-${basename?replace('[','')?replace(']','')?replace('.','-')}"/>
	<@row path=basename>
	<@label path="${basename}.upload">${labelText}</@label>
	<@field>
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

	<small class="subtle help-block">
		<#if required>
			<#if !multiple || max=1>
				You must attach one file.
			<#else>
				You must attach at least one file. Up to <@fmt.p max "attachment" /> allowed.
			</#if>
		<#else>
			<#if !multiple || max=1>
				One attachment allowed.
			<#else>
				Up to <@fmt.p max "attachment" /> allowed.
			</#if>
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
		    $addButton = $('<a>').addClass('btn btn-mini').append($('<i class="icon-plus fa fa-plus"></i>').attr('title','Add another attachment'));
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
	</@field>
	</@row>
</#macro>

</#escape>
</#compress>
