<#macro student_item student index stages marker={}>
	<li class="student well well-sm">
		<div class="name">
			<h6>${student.fullName}&nbsp;${student.warwickId!userId}</h6>
			<#list stages as stage>
				<#-- one input per stage. if no marker is defined leave the name blank (unallocated students aren't bound) -->
				<input
					type="hidden"
					<#if marker?has_content>name="allocations['${stage}']['${marker.userId}'][${index}]"</#if>
					value="${student.userId}"
				>
			</#list>
		</div>
	</li>
</#macro>

<#macro allocateStudents assignment role stages markers unassigned assigned>
<h2>Allocate students to ${role}s</h2>
<p>Drag students onto a ${role} to allocate them. Select multiple students by dragging a box around them. You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key and drag to add to a selection.</p>
<div class="fix-area">
	<div class="tabula-dnd marker-allocation"
		 data-item-name="student"
		 data-text-selector=".name h6"
		 data-selectables=".students .drag-target"
		 data-scroll="true"
		 data-remove-tooltip="Unassign this student from the marker">

		<!-- persist header -->
		<div class="fix-header pad-when-fixed">
			<p class="btn-toolbar">
				<a class="random btn btn-default" data-toggle="randomise" data-disabled-on="empty-list" href="#" >
					Randomly allocate
				</a>
				<a class="return-items btn btn-default" data-toggle="return" data-disabled-on="no-allocation" href="#" >
					Remove all
				</a>
			</p>
			<div class="row">
				<div class="col-md-5">
					<h3>Students</h3>
				</div>
				<div class="col-md-2"></div>
				<div class="col-md-5">
					<h3>${role}s</h3>
				</div>
			</div>
		</div>
		<!-- end persist header -->
		<div class="row fix-on-scroll-container">
			<div class="col-md-5">
				<div id="${role}StudentsList" class="students" data-item-selector=".student-list li">
					<div class="well">
						<h4>Not allocated to a marker</h4>
						<#if assignment.anonymousMarking>
							You have set anonymity to on. Markers cannot see students' names or University IDs.
						</#if>
						<div class="student-list drag-target">
							<ul class="drag-list return-list unstyled" data-nobind="true">
								<#list unassigned as student>
									<@student_item student student_index stages />
								</#list>
							</ul>
						</div>
					</div>
				</div>
			</div>
			<div class="col-md-2">
				<#-- all hail our jumbo icon overlords! -->
				<div class="direction-icon fix-on-scroll">
					<i class="fa fa-arrow-right"></i>
				</div>
			</div>
			<div class="col-md-5">
				<div id="${role}MarkerList" class="groups fix-on-scroll">
					<#list markers as marker>
						<#assign existingStudents = mapGet(assigned, marker)![] />
						<div class="drag-target well clearfix ${role}-${marker.userId}">
							<div class="group-header">
								<#assign popoverHeader>Students assigned to ${marker.fullName}</#assign>
								<h4 class="name">
									${marker.fullName}
								</h4>
								<div>
									<#assign count = existingStudents?size />
									<span class="drag-count">${count}</span> <span class="drag-counted" data-singular="student" data-plural="students">student<#if count != 1>s</#if></span>
									<a id="show-list-${role}-${marker.userId}" class="show-list" title="View students" data-container=".${role}-${marker.userId}" data-title="${popoverHeader}" data-placement="left"><i class="fa fa-pencil-square-o"></i></a>
								</div>
							</div>

							<#assign bindpath><#compress>
								<#list stages as stage>allocations['${stage}']['${marker.userId}']<#if stage_has_next>,</#if></#list>
							</#compress></#assign>

							<ul class="drag-list hide" data-bindpath="${bindpath}">
								<#if existingStudents?has_content><#list existingStudents as student>
									<@student_item student student_index stages marker/>
								</#list></#if>
							</ul>
						</div>
					</#list>
				</div>
			</div>
		</div>
	</div>
</div>
</#macro>
