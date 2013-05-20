<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#macro student_item student bindpath="">
<li class="student">
	<i class="icon-white icon-user"></i> ${student.displayValue}
	<input type="hidden" name="${bindpath}" value="${student.userCode}" />
</li>
</#macro>

<#macro assignStudents studentList markerList class name>
	<div class="btn-toolbar">
		<a class="random btn btn-mini"
		   href="#" >
			<i class="icon-random"></i> Randomly allocate
		</a>
		<a class="return-items btn btn-mini"
		   href="#" >
			<i class="icon-arrow-left"></i> Remove all
		</a>
	</div>
	<div class="row-fluid">
		<div class="students span4">
			<h3>Students</h3>
			<div class="student-list drag-target">
				<ul class="drag-list return-list" data-nobind="true">
					<#list studentList as student>
						<@student_item student "" />
					</#list>
				</ul>
			</div>
		</div>
		<div class="${class} span8">
			<h3>${name}</h3>
			<#list markerList as marker>
				<#assign existingStudents = marker.students />
				<div class="marker drag-target">
					<span class="name">${marker.fullName}</span>
					<span class="drag-count badge badge-info">${existingStudents?size}</span>

					<ul class="drag-list hide" data-bindpath="markerMapping[${marker.userCode}]">
						<#list existingStudents as student>
							<@student_item student "markerMapping[${marker.userCode}][${student_index}]" />
						</#list>
					</ul>

					<a href="#" class="btn show-list" data-title="Students to be marked by ${marker.fullName}"><i class="icon-list"></i> List</a>

				</div>
			</#list>
		</div>
	</div>
</#macro>


<#escape x as x?html>
	<h1>Assign markers for ${assignment.name}</h1>
	
	<p>Drag students by their <i class="icon-th"></i> onto a marker.</p>
	
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/assign-markers')}" commandName="assignMarkersCommand">
	<div id="assign-markers">
		<ul class="nav nav-tabs">
			<li class="active">
				<a href="#first-markers">
					First markers
				</a>
			</li>
			<li>
				<a href="#second-markers">
					Second markers
				</a>
			</li>
		</ul>
		<div class="tab-content">
			<div class="tab-pane active" id="first-markers">
				<@assignStudents
					assignMarkersCommand.firstMarkerUnassignedStudents
					assignMarkersCommand.firstMarkers
					"first-markers"
					"First Markers"
				/>
			</div>
			<div class="tab-pane" id="second-markers">
				<@assignStudents
					assignMarkersCommand.secondMarkerUnassignedStudents
					assignMarkersCommand.secondMarkers
					"second-markers"
					"Second Markers"
				/>
			</div>
		</div>
	</div>
	<div class="submit-buttons">
		<input type="submit" class="btn btn-primary" value="Save">
		<a href="<@routes.depthome module />" class="btn">Cancel</a>
	</div>
	</@f.form>
</#escape>


