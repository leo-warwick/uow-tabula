<#escape x as x?html>
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/modal_macros.ftl" as modal />

<#assign manualFormAction><@routes.profiles.relationship_allocate department relationshipType /></#assign>
<#assign uploadFormAction><@routes.profiles.relationship_allocate_upload department relationshipType /></#assign>
<#assign previewFormAction><@routes.profiles.relationship_allocate_preview department relationshipType /></#assign>

<#function route_function dept>
	<#local result><@routes.profiles.relationship_allocate dept relationshipType /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader "Allocate ${relationshipType.description}s" route_function "in" />

<div class="tabbable">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#allocatestudents-tab1" data-toggle="tab">Manually allocate students</a>
		</li>
		<li>
			<a href="#allocatestudents-tab2" data-toggle="tab">Upload spreadsheet</a>
		</li>
	</ul>
</div>

<div class="tab-content">
	<#include "_allocate_manual_tab.ftl" />

	<div class="tab-pane" id="allocatestudents-tab2">
		<@f.form commandName="uploadCommand" action="${uploadFormAction}" method="POST" cssClass="form-inline" enctype="multipart/form-data">
			<#list command.additions?keys as entity>
				<#list command.additions[entity] as student>
					<input type="hidden" name="additions[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.removals?keys as entity>
				<#list command.removals[entity] as student>
					<input type="hidden" name="removals[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.additionalEntities as entity>
				<input type="hidden" name="additionalEntities" value="${entity}" />
			</#list>

			<#assign introText>
				<p>The spreadsheet must be in <samp>.xlsx</samp> format (created in Microsoft Excel 2007 or newer, or another compatible spreadsheet application). You can download a template spreadsheet which is correctly formatted, ready for completion.<p>
				<p>The spreadsheet must contain two columns, headed:<p>
				<ul>
					<li><b>student_id</b> - contains the student's University ID number (also known as the library card number)</li>
					<li><b>agent_id</b> - contains the ${relationshipType.agentRole}'s University ID number</li>
				</ul>
				<p>You may need to <a href='http://office.microsoft.com/en-gb/excel-help/format-numbers-as-text-HA102749016.aspx?CTT=1'>format these columns</a> as text to avoid Microsoft Excel removing 0s from the start of ID numbers.</p>
				<p>The spreadsheet may also contain other columns and information for your own reference (these will be ignored by Tabula).</p>
			</#assign>

			<p>You can set ${relationshipType.agentRole}s for many students at once by uploading a spreadsheet.
				<a href="#"
				   id="agent-intro"
				   class="use-introductory"
				   data-hash="${introHash("agent-intro")}"
				   data-title="${relationshipType.agentRole} spreadsheet"
				   data-trigger="click"
				   data-placement="bottom"
				   data-html="true"
				   data-content="${introText}"><i class="fa fa-question-circle"></i></a></p>

			<ol>
				<li>
					<button type="submit" class="btn btn-default dirty-check-ignore" name="templateWithChanges" value="true">Download a template spreadsheet</button>
					<br />
					This will be prefilled with the names and University ID numbers of students and their ${relationshipType.agentRole} (if they have one) in ${department.name}. In Excel you may need to <a href="http://office.microsoft.com/en-gb/excel-help/what-is-protected-view-RZ101665538.aspx?CTT=1&section=7">exit protected view</a> to edit the spreadsheet.
					<br /><br />
					<div class="alert alert-info">
						<p>This will include any changes made in the manually allocate tab. You can also <a href="<@routes.profiles.relationship_template department relationshipType />">download a template without these changes</a>.</p>
					</div>
				</li>
				<li><strong>Allocate students</strong> to ${relationshipType.agentRole}s using the dropdown menu in the <strong>${relationshipType.agentRole?cap_first} name</strong> column or by typing a ${relationshipType.agentRole}'s University ID into the <strong>agent_id</strong> column. The <strong>agent_id</strong> field will be updated with the University ID for that ${relationshipType.agentRole} if you use the dropdown.</li>
				<li><strong>Save</strong> your updated spreadsheet.</li>
				<li>
					<@bs3form.labelled_form_group path="file.upload" labelText="Choose your updated spreadsheet">
						<input type="file" name="file.upload"  />
					</@bs3form.labelled_form_group>
				</li>
			</ol>


			<div class="submit-buttons">
				<button class="btn btn-primary btn-lg" name="doPreviewSpreadsheetUpload">Upload</button>
			</div>
		</@f.form>
	</div>
</div>

<div id="profile-modal" class="modal fade profile-subset"></div>

</#escape>
