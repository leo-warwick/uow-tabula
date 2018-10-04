<#import "*/modal_macros.ftl" as modal />

<#escape x as x?html>

<#if user.staff>
	<#include "search/form.ftl" />
	<hr class="full-width" />
</#if>

<h1>Identity</h1>

<section class="identity">
	<div class="row">
		<div class="col-md-6">
			<h2>${member.fullName}</h2>

			<div class="row">
				<div class="col-md-5 col-lg-4">
					<@fmt.member_photo member />
				</div>
				<div class="col-md-7 col-lg-8">
					<strong>Official name:</strong> ${member.officialName}<br/>
					<strong>Preferred name:</strong> ${member.fullName}<br/>
					<#if member.dateOfBirth??>
						<strong>Date of birth:</strong> ${member.dateOfBirth?date("yyyy-MM-dd")?string("dd/MM/yyyy")}<br/>
					</#if>
					<#if member.nationality??>
						<strong>Nationality:</strong> <@fmt.nationality member.nationality!('Unknown') />
						<#if member.secondNationality??> and <@fmt.nationality member.secondNationality /></#if>
						<br/>
					</#if>
					<#if features.disabilityRenderingInProfiles && (member.disability.reportable)!false>
						<strong>Disability:</strong>
						<a href="#" class="use-popover cue-popover" id="popover-disability" data-html="true"
						   data-content="<p><#if isSelf>You have<#else>This student has</#if> self-reported the following disability code:</p><div class='well'><h6>${member.disability.code}</h6><small>${(member.disability.sitsDefinition)!}</small></div>"> ${member.disability.definition}</a><br/>
					</#if>
					<#if features.visaInStudentProfile && member.hasTier4Visa?? && member.casUsed??>
						<strong>Tier 4 requirements:</strong>
						<#if member.casUsed && member.hasTier4Visa>Yes
						<#elseif !member.casUsed && !member.hasTier4Visa>No
						<#else>
							<#if !member.casUsed && member.hasTier4Visa>
								<#assign inconsistency = "Tier 4 visa exists but no Confirmation of Acceptance for Studies" />
							<#else>
								<#assign inconsistency = "Confirmation of Acceptance for Studies exists but no tier 4 visa" />
							</#if>
							Contact the <a href="mailto:immigrationservice@warwick.ac.uk">Immigration Service</a>
							<a class="use-popover" data-content="Contact the University's Immigration Service to find out whether tier 4
							requirements apply to this student. (${inconsistency})" data-toggle="popover"><i class="fa fa-question-circle"></i></a>
						</#if>
						<br/>
					</#if>
					<#if member.email??>
						<strong>Warwick email:</strong> <a href="mailto:${member.email}">${member.email}</a><br/>
					</#if>
					<#if member.homeEmail??>
						<strong>Alternative email:</strong> <a href="mailto:${member.homeEmail}">${member.homeEmail}</a><br/>
					</#if>
					<#if member.mobileNumber??>
						<strong>Mobile phone:</strong> ${phoneNumberFormatter(member.mobileNumber)}<br/>
					</#if>
					<#if member.universityId??>
						<strong>University ID: </strong> ${member.universityId}<br/>
					</#if>
					<#if member.userId??>
						<strong>Username:</strong> ${member.userId}<br/>
					</#if>
					<#if member.homeDepartment??>
						<strong>Home department:</strong> ${member.homeDepartment.name}<br/>
					</#if>
				</div>
			</div>

			<#if can.do("RolesAndPermissions.Create", member)>
				<#assign permissions_url><@routes.profiles.permissions member /></#assign>
				<p><@fmt.permission_button
					permission='RolesAndPermissions.Create'
					scope=member
					action_descr='modify permissions'
					classes='btn btn-primary'
					href=permissions_url
					tooltip='Permissions'
				>
					Permissions
				</@fmt.permission_button></p>
			</#if>
		</div>

		<#if courseDetails?has_content>
			<div class="col-md-6">
				<h2>Course</h2>
				<#list courseDetails as scd>
					<details class="indent" <#if courseDetails?first.scjCode == scd.scjCode>open</#if>>
						<summary>
							<strong>Course:</strong> ${scd.course.name}, ${scd.course.code}
								(${(scd.beginYear?string("0000"))!} - ${(scd.endYear?string("0000"))!})
						</summary>
						<#if scd.department??>
							<strong>Department:</strong> ${(scd.department.name)!} (${((scd.department.code)!)?upper_case})<br />
						</#if>
						<#if scd.currentRoute?? && scd.currentRoute.degreeType??>
							<strong>UG/PG:</strong> ${(scd.currentRoute.degreeType.toString)!}<br />
						</#if>
						<#if scd.award??>
							<strong>Intended award:</strong> ${(scd.award.name)!}<br />
						</#if>
						<#if scyd.modeOfAttendance??>
							<strong>Attendance:</strong> ${scyd.modeOfAttendance.fullNameAliased}<br />
						</#if>
						<#if !isSelf && scd.statusOnCourse??>
							<strong>Status on course:</strong> <@fmt.status_on_course scd /><br />
						</#if>
						<#if scd.beginDate??>
							<strong>Start date:</strong> <@fmt.date date=scd.beginDate includeTime=false /><br />
						</#if>
						<#if scd.endDate?? || scd.expectedEndDate??>
							<#if scd.endDate??>
								<strong>End date:</strong> <@fmt.date date=scd.endDate includeTime=false /><br />
							<#elseif scd.expectedEndDate??>
								<strong>Expected end date:</strong> <@fmt.date date=scd.expectedEndDate includeTime=false/><br />
							</#if>
						</#if>
						<#if scyd.route??>
							<strong>Route:</strong> ${(scyd.route.name)!} (${(scyd.route.code?upper_case)!})<br/>
						<#elseif scd.currentRoute??>
							<strong>Route:</strong> ${(scd.currentRoute.name)!} (${(scd.currentRoute.code?upper_case)!})<br/>
						</#if>
						<#if scyd.yearOfStudy??>
							<strong>Study block or year:</strong> ${(scyd.yearOfStudy)!}
							<i class="fa fa-question-circle text-primary use-tooltip" title="An intake for a particular course or period of study. E.g. an undergraduate in their third year of study with no breaks is in block 3. A third-year undergraduate who temporarily withdrew for one year is in block 4." data-placement="right"></i>
							<br/>
						</#if>
						<#if scd.hasCurrentEnrolment && scd.level?? && scyd.latest>
							<strong>Current course level:</strong> ${scd.level.code} (${scd.level.name?lower_case?cap_first})<br />
						</#if>
						<#if scd.sprCode??>
							<strong>Programme route code:</strong> ${scd.sprCode}<br />
						</#if>
						<#if scd.scjCode??>
							<strong>Course join code:</strong> ${scd.scjCode}<br />
						</#if>
					</details>
					<br />
				</#list>
			</div>
		</#if>
	</div>
</section>

<#assign canCreateMemberNote = can.do("MemberNotes.Create", member) />
<#if memberNotes?has_content || canCreateMemberNote>
	<section class="member-notes">
		<div class="row">
			<div class="col-md-12">
				<h2>Administrative notes</h2>

				<table class="table table-striped expanding-row-pairs member-notes">
					<thead>
						<tr>
							<th>Date</th>
							<th>Title</th>
							<th></th>
						</tr>
					</thead>
					<tbody>
						<#list memberNotes as memberNote>
							<#assign canDeletePurgeMemberNote = can.do("MemberNotes.Delete", memberNote) />
							<#assign canEditMemberNote = can.do("MemberNotes.Update", memberNote) />
							<tr <#if memberNote.deleted>class="deleted subtle"</#if>>
								<td data-sortby="${memberNote.creationDate.millis?c}"><@fmt.date date=memberNote.creationDate includeTime=true /></td>
								<td>${memberNote.title!}</td>
								<td>
									<#if canEditMemberNote || canDeletePurgeMemberNote>
										<div class="pull-right">
											<i class="fa fa-spinner fa-spin invisible"></i>
											<span class="dropdown">
												<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">Actions <span class="caret"></span></a>
												<ul class="dropdown-menu pull-right">
													<#if canEditMemberNote>
														<li>
															<a data-toggle="modal" data-target="#note-modal" href="#note-modal" data-url="<@routes.profiles.edit_member_note memberNote />" class="edit <#if memberNote.deleted>disabled</#if>" title="Edit note">Edit</a>
														</li>
													</#if>
													<#if canDeletePurgeMemberNote>
														<li>
															<a href="<@routes.profiles.delete_member_note memberNote />" class="delete <#if memberNote.deleted>disabled</#if>">Delete</a>
														</li>
														<li>
															<a href="<@routes.profiles.restore_member_note memberNote />" class="restore <#if !memberNote.deleted>disabled</#if>">Restore</a>
														</li>
														<li>
															<a href="<@routes.profiles.purge_member_note memberNote />" class="purge <#if !memberNote.deleted>disabled</#if>">Purge</a>
														</li>
													</#if>
												</ul>
											</span>
										</div>
									</#if>
									<small class="muted clearfix">Student note created by ${memberNote.creator.fullName}</small>
								</td>
							</tr>
							<tr>
								<td colspan="3">
									<#if memberNote.note??>
										<#noescape>${memberNote.escapedNote}</#noescape>
									</#if>
									<#if memberNote.attachments?has_content>
										<ul class="deleted-files ${memberNote.deleted?string("","hidden")}">
											<#list memberNote.attachments as file>
												<li class="muted deleted">${file.name}</li>
											</#list>
										</ul>
										<#assign mbDownloadUrl><@routes.profiles.download_member_note_attachment memberNote /></#assign>
										<div class="deleted-files ${memberNote.deleted?string('hidden','')}">
											<@fmt.download_attachments memberNote.attachments mbDownloadUrl />
										</div>
									</#if>
								</td>
							</tr>
						</#list>
					</tbody>
				</table>
				<#if canCreateMemberNote>
					<p>
						<a class="btn btn-primary create"
						   data-toggle="modal"
						   data-target="#note-modal"
						   href="#note-modal"
						   data-url="<@routes.profiles.create_member_note member />"
						   title="Create new note"
						>
							Create new
						</a>
					</p>
				</#if>
			</div>
		</div>
	</section>
</#if>

<#if features.profilesCircumstances && (extenuatingCircumstances?has_content || canCreateMemberNote)>
	<section class="circumstances">
		<div class="row">
			<div class="col-md-12">
				<h2>Extenuating circumstances</h2>

				<table class="table table-striped expanding-row-pairs">
					<thead>
					<tr>
						<th>Created by</th>
						<th>Dates</th>
					</tr>
					</thead>
					<tbody>
						<#list extenuatingCircumstances as circumstances>
							<#assign canDeletePurgeCircumstances = can.do("MemberNotes.Delete", circumstances) />
							<#assign canEditCircumstances = can.do("MemberNotes.Update", circumstances) />
							<tr <#if circumstances.deleted>class="deleted subtle"</#if>>
								<td>${(circumstances.creator.fullName)!"[Unknown]"} - Created: <@fmt.date date=circumstances.creationDate includeTime=true /></td>
								<td>
									<#if canEditCircumstances>
										<div class="pull-right">
											<i class="fa fa-spinner fa-spin invisible"></i>
											<span class="dropdown">
												<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">Actions <span class="caret"></span></a>
												<ul class="dropdown-menu pull-right">
													<li>
														<a data-toggle="modal" data-target="#note-modal" href="#note-modal" data-url="<@routes.profiles.edit_circumstances circumstances />" class="edit <#if circumstances.deleted>disabled</#if>" title="Edit extenuating circumstances">Edit</a>
													</li>
													<#if canDeletePurgeCircumstances>
														<li>
															<a href="<@routes.profiles.delete_circumstances circumstances />" class="delete <#if circumstances.deleted>disabled</#if>">Delete</a>
														</li>
															<li>
															<a href="<@routes.profiles.restore_circumstances circumstances />" class="restore <#if !circumstances.deleted>disabled</#if>">Restore</a>
														</li>
														<li>
															<a href="<@routes.profiles.purge_circumstances circumstances />" class="purge <#if !circumstances.deleted>disabled</#if>">Purge</a>
														</li>
													</#if>
												</ul>
											</span>
										</div>
									</#if>
									${circumstances.startDate.toDate()?string("dd/MM/yy")} - ${circumstances.endDate.toDate()?string("dd/MM/yy")}
								</td>
							</tr>
							<tr>
								<td colspan="2">
									<#if circumstances.note??>
										<#noescape>${circumstances.escapedNote}</#noescape>
									</#if>
									<#if circumstances.attachments?has_content>
										<ul class="deleted-files ${circumstances.deleted?string("","hidden")}">
											<#list circumstances.attachments as file>
												<li class="muted deleted">${file.name}</li>
											</#list>
										</ul>
										<#assign mbDownloadUrl><@routes.profiles.download_circumstances_attachment circumstances /></#assign>
										<div class="deleted-files ${circumstances.deleted?string('hidden','')}">
											<@fmt.download_attachments circumstances.attachments mbDownloadUrl />
										</div>
									</#if>
								</td>
							</tr>
						</#list>
					</tbody>
				</table>
				<#if canCreateMemberNote>
					<p>
						<a class="btn btn-primary create"
						   data-toggle="modal"
						   data-target="#note-modal"
						   href="#note-modal"
						   data-url="<@routes.profiles.create_circumstances member />"
						   title="Create new extenuating circumstances"
						>
							Create new
						</a>
					</p>
				</#if>
			</div>
		</div>
	</section>
</#if>

<div id="note-modal" class="modal fade">
	<@modal.wrapper>
		<@modal.header>
			<h3 class="modal-title"><span></span> for ${member.fullName}</h3>
		</@modal.header>
		<@modal.body></@modal.body>
		<@modal.footer>
			<input id="member-note-save" type="submit" class="btn btn-primary" value="Save">
		</@modal.footer>
	</@modal.wrapper>
</div>

<script>
	jQuery(function($){
		var $table = $('table.member-notes');
		$table.tablesorter({
			headers: {1:{sorter:false}},
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.data('sortby')) {
					return $el.data('sortby');
				} else {
					return $el.text().trim();
				}
			}
		});
		$table.on('sortStart', function(){
			$table.find('tr.expanded + tr').detach();
		}).on('sortEnd', function(){
			$table.find('tr').each(function(){
				var $row = $(this);
				if ($row.hasClass('expanded')) {
					$row.after($row.data('expandRow'));
				}
			});
		});
	});
</script>
</#escape>