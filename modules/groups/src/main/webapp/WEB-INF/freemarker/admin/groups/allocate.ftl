<#assign set=allocateStudentsToGroupsCommand.set />
<#assign mappingById=allocateStudentsToGroupsCommand.mappingById />
<#assign membersById=allocateStudentsToGroupsCommand.membersById />

<#macro student_item student bindpath="">
	<#assign profile = membersById[student.warwickId]!{} />
	<li class="student well well-small">
		<div class="profile clearfix">
			<div class="photo">
				<#if profile.universityId??>
					<img src="<@url page="/view/photo/${profile.universityId}.jpg" context="/profiles" />" />
				<#else>
					<img src="<@url resource="/static/images/no-photo.png" />" />
				</#if>
			</div>
			
			<div class="name">
				<h6>${profile.fullName!student.fullName}</h6>
				${(profile.studyDetails.route.name)!student.shortDepartment}
			</div>
		</div>
		<input type="hidden" name="${bindpath}" value="${student.userId}" />
	</li>
</#macro>

<#escape x as x?html>
	<h1>Allocate students to ${set.name}</h1>
	
	
	
	<noscript>
		<div class="alert">This page requires Javascript.</div>
	</noscript>
	
	<div class="tabbable fix-on-scroll-container">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#allocategroups-tab1" data-toggle="tab">Drag and Drop</a>
		</li>
		<li >
			<a href="#allocategroups-tab2" data-toggle="tab">Upload Spreadsheet</a>
		</li>
	</ul>
	
	<div class="tab-content">
	
	<div class="tab-pane active" id="allocategroups-tab1">
	
	<p>Drag students onto a group to allocate them to it. Select multiple students by dragging a box around them.
		 You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key to add to a selection.</p>
	
	<@spring.hasBindErrors name="allocateStudentsToGroupsCommand">
		<#if errors.hasErrors()>
			<div class="alert alert-error">
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
	
	<#assign submitUrl><@routes.allocateset set /></#assign>
	<@f.form method="post" action="${submitUrl}" commandName="allocateStudentsToGroupsCommand">
		<div class="btn-toolbar">
			<a class="random btn"
			   href="#" >
				<i class="icon-random"></i> Randomly allocate
			</a>
			<a class="return-items btn"
			   href="#" >
				<i class="icon-arrow-left"></i> Remove all
			</a>
		</div>
		<div class="row-fluid"> <!-- fix-on-scroll-container -->
			<div class="span5">
				<div class="students">
					<h3>Students</h3>
					<div class="well student-list drag-target">
						<h4>Not allocated to a group</h4>
					
						<ul class="drag-list return-list unstyled" data-bindpath="unallocated">
							<@spring.bind path="unallocated">
								<#list status.actualValue as student>
									<@student_item student "${status.expression}[${student_index}]" />
								</#list>
							</@spring.bind>
						</ul>
					</div>
				</div>
			</div>
			<div class="span2">
				<#-- I, for one, welcome our new jumbo icon overlords -->
				<div class="direction-icon fix-on-scroll">
					<i class="icon-arrow-right"></i>
				</div>
			</div>
			<div class="span5">
				<div class="groups fix-on-scroll">
					<h3>Groups</h3>			
					<#list set.groups as group>
						<#assign existingStudents = mappingById[group.id]![] />
						<div class="drag-target well clearfix">
							<div class="group-header">
								<#assign popoverHeader>
									Students in ${group.name}
									<button type='button' onclick="jQuery('#show-list-${group.id}').popover('hide')" class='close'>&times;</button>
								</#assign>
								<#assign groupDetails>
									<ul class="unstyled">
										<#list group.events as event>
											<li>
												<#-- Tutor, weeks, day/time, location -->
	
												<@fmt.weekRanges event />,
												${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime />,
												${event.location}
											</li>
										</#list>
									</ul>
								</#assign>
							
								<h4 class="name">
									${group.name}
								</h4>
								
								<div>
									<span class="drag-count">${existingStudents?size}</span> students
									
									<a id="show-list-${group.id}" class="show-list" data-title="${popoverHeader}" data-prelude="${groupDetails}" data-placement="left"><i class="icon-question-sign"></i></a>
								</div>
							</div>
		
							<ul class="drag-list hide" data-bindpath="mapping[${group.id}]">
								<#list existingStudents as student>
									<@student_item student "mapping[${group.id}][${student_index}]" />
								</#list>
							</ul>
						</div>
					</#list>
				</div>
			</div>
		</div>		
		
		<div class="submit-buttons">
			<input type="submit" class="btn btn-primary" value="Save">
			<a href="<@routes.depthome module />" class="btn">Cancel</a>
		</div>
	</@f.form>
	</div><!-- end 1st tab -->
	
	<div class="tab-pane" id="allocategroups-tab2">

		<#assign introText>
			<p>The spreadsheet must be in <samp>.xlsx</samp> format (created in Microsoft Excel 2007 or newer, or another compatible spreadsheet application). You can download a template spreadsheet which is correctly formatted, ready for completion.<p>
			<p>The spreadsheet must contain two columns, headed:<p>
			<ul>
				<li><b>student_id</b> - contains the student's University ID number (also known as the library card number)</li>
				<li><b>group_id</b> - contains the small group ID</li>
			</ul>
			<p>You may need to <a href='http://office.microsoft.com/en-gb/excel-help/format-numbers-as-text-HA102749016.aspx?CTT=1'>format these columns</a> as text to avoid Microsoft Excel removing 0s from the start of ID numbers.</p>
			<p>The spreadsheet may also contain other columns and information for your own reference (these will be ignored by Tabula).</p>
		</#assign>

		<p>You can set small groups for many students at once by uploading a spreadsheet.
			<a href="#"
			   id="smallgroup-intro"
			   class="use-introductory"
			data-hash="${introHash("smallgroup-intro")}"
			data-title="Small groups spreadsheet"
			data-trigger="click"
			data-placement="bottom"
			data-html="true"
			data-content="${introText}"><i class="icon-question-sign"></i></a></p>


		<p><a href="allocate/template">Download a template spreadsheet</a></p>

		<@f.form method="post" enctype="multipart/form-data" action="${submitUrl}" commandName="allocateStudentsToGroupsCommand">
		<input name="isfile" value="true" type="hidden"/>
		<table role="presentation" class="narrowed-form">
			<tr>
				<td id="multifile-column">
					<h3>Select file</h3>
					<p id="multifile-column-description">
						<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
					</p>
					<@form.labelled_row "file.upload" "Files">
					<input type="file" name="file.upload" multiple />
				</@form.labelled_row>
			</td>
		</tr>
	</table>
	<div class="submit-buttons">
		<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
	</div>
</@f.form>
	</div><!-- end 2nd tab-->
	
	</div><!-- end tab-content -->
	
	</div> <!-- end tabbable -->
	
</#escape>