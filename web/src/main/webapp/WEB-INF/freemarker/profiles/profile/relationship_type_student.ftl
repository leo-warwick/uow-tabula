<#import "*/modal_macros.ftlh" as modal />
<#import "*/profiles_macros.ftl" as profiles />
<#escape x as x?html>

  <#if scheduledAgentChange?has_content>
    <div class="alert alert-info">
      Change of ${relationshipType.agentRole} scheduled for <@fmt.date scheduledAgentChange />
    </div>
  <#elseif scheduledAgentChangeCancel!false>
    <div class="alert alert-info">
      Scheduled change of ${relationshipType.agentRole} cancelled
    </div>
  </#if>

  <@profiles.profile_header member isSelf />

  <h1>${relationshipType.agentRole?cap_first}</h1>

  <#if !relationshipsToDisplay?has_content>
    <div class="alert alert-info">No ${relationshipType.agentRole} details found for this course and academic year</div>
  </#if>

  <div class="row">
    <#list relationshipsToDisplay as relationship>
      <div class="col-md-4">
        <div class="clearfix">
          <#if relationship.agentMember??>
            <@fmt.relation_photo member relationship "thumbnail" />
          </#if>

        </div>
        <br />
        <#if relationship.agentMember??>
          <p>
            <strong>Name:</strong> ${relationship.agentMember.fullName!relationshipType.agentRole?cap_first}<br />
            <#if relationship.agentMember.email?has_content><strong>Warwick email:</strong> <a
                    href="mailto:${relationship.agentMember.email}">${relationship.agentMember.email}</a><br /></#if>
            <strong>University ID:</strong> ${relationship.agentMember.universityId}<br />
            <#if relationship.agentMember.userId??><strong>Username:</strong> ${relationship.agentMember.userId}<br /></#if>
            <span class="peoplesearch-info" data-href="<@routes.profiles.peoplesearchData relationship.agentMember />"></span>
            <#if (canEditRelationship!false) && !studentCourseDetails.student.deceased>
              <a class="btn btn-primary ajax-modal"
                 href="<@routes.profiles.relationship_edit relationshipType studentCourseDetails.urlSafeId relationship.agentMember />"
                 data-target="#change-agent"
                 data-toggle="modal"
              >
                Edit ${relationshipType.agentRole}
              </a>
            </#if>
          </p>
        <#else>
          <p>
            <strong>Name:</strong> ${relationship.agentName}<br />
            <span class="muted">External to Warwick</span>
          </p>
        </#if>
      </div>
    </#list>
  </div>

  <div class="row">
    <div class="col-md-12">
      <p>
        <#if (canEditRelationship!false) &&  !studentCourseDetails.student.deceased >
          <a class="btn btn-primary ajax-modal"
             href="<@routes.profiles.relationship_add relationshipType studentCourseDetails.urlSafeId />"
             data-target="#change-agent"
             data-toggle="modal"
          >
            Add ${relationshipType.agentRole}
          </a>
        </#if>
        <a class="btn btn-default" data-target="#timeline" data-toggle="modal" href="#">View timeline</a>
      </p>
    </div>
  </div>

  <@modal.modal id="timeline">
    <@modal.wrapper>
    <@modal.header>
      <h3 class="modal-title">Changes to ${relationshipType.agentRole}</h3>
    </@modal.header>
    <@modal.body>
    <#if !pastAndPresentRelationships?has_content>
      <em>No past or present ${relationshipType.agentRole} details found for this course</em>
    <#else>
      <table class="table table-hover table-condensed table-striped sb-no-wrapper-table-popout">
        <thead>
        <tr>
          <th></th>
          <th>Start date</th>
          <th>End date</th>
        </tr>
        </thead>
        <tbody>
        <#macro timeline_row relationship>
          <tr>
            <td>
              <@fmt.relation_photo member relationship "tinythumbnail" />
              ${relationship.agentName}
              <#if relationship.percentage?has_content>
                <span class="percentage muted">(${relationship.percentage}%)</span>
              </#if>
            </td>
            <td>
              <#if relationship.startDate??>
                <span tabindex="0" class="use-tooltip" data-container="body"
                      title="<@fmt.date date=relationship.startDate includeTime=true relative=false />">
											<@fmt.date date=relationship.startDate includeTime=false />
										</span>
              <#else>
                <em>Unknown</em>
              </#if>
            </td>
            <td>
              <#if relationship.endDate??>
                <span tabindex="0" class="use-tooltip" data-container="body" title="<@fmt.date date=relationship.endDate includeTime=true relative=false />">
											<@fmt.date date=relationship.endDate includeTime=false />
										</span>
              <#else>
                -
              </#if>
            </td>
          </tr>
        </#macro>
        <#list pastAndPresentRelationships as rel>
          <#if rel.current>
            <@timeline_row rel />
          </#if>
        </#list>
        <#list pastAndPresentRelationships as rel>
          <#if !rel.current>
            <@timeline_row rel />
          </#if>
        </#list>
        </tbody>
      </table>
    </#if>

    <#if scheduledRelationshipChanges?has_content>
      <h4>Scheduled changes</h4>
      <table class="table table-hover table-condensed table-striped sb-no-wrapper-table-popout">
        <thead>
        <tr>
          <th>Change</th>
          <th>Date</th>
          <#if (canEditRelationship!false)>
            <th></th>
          </#if>
        </tr>
        </thead>
        <tbody>
        <#list scheduledRelationshipChanges as relationship>
          <#assign action = '' />
          <#if (relationship.replacesRelationships?has_content)>
            <#assign action = 'replace' />
          <#elseif (relationship.startDate?? && relationship.startDate.afterNow)>
            <#assign action = 'add' />
          <#elseif (relationship.endDate?? && relationship.endDate.afterNow)>
            <#assign action = 'remove' />
          </#if>
          <tr>
            <td>
              <#if action == 'replace'>
                Replace <#list relationship.replacesRelationships as replaced>${replaced.agentName}<#if replaced_has_next>, </#if></#list> with ${relationship.agentName}
              <#elseif action == 'add'>
                Add ${relationship.agentName}
              <#elseif action == 'remove'>
                Remove ${relationship.agentName}
              </#if>
            </td>
            <td>
              <#if (relationship.startDate?? && relationship.startDate.afterNow)>
                <span tabindex="0" class="use-tooltip" data-container="body"
                      title="<@fmt.date date=relationship.startDate includeTime=true relative=false />">
											<@fmt.date date=relationship.startDate includeTime=false relative=false shortMonth=true />
										</span>
              <#elseif (relationship.endDate?? && relationship.endDate.afterNow)>
                <span tabindex="0" class="use-tooltip" data-container="body" title="<@fmt.date date=relationship.endDate includeTime=true relative=false />">
											<@fmt.date date=relationship.endDate includeTime=false relative=false shortMonth=true />
										</span>
              </#if>
            </td>
            <#if (canEditRelationship!false)>
              <td>
                <#assign formAction><@routes.profiles.relationship_scheduled_change_cancel relationship /></#assign>
                <@f.form action=formAction method="post">
                  <input type="hidden" name="notifyStudent" value="false" />
                  <input type="hidden" name="notifyOldAgent" value="false" />
                  <input type="hidden" name="notifyNewAgent" value="false" />
                  <#assign popover>
                    <@bs3form.labelled_form_group path="" labelText="Notify these people via email of this cancellation">
                      <@bs3form.checkbox>
                        <input type="checkbox" name="notifyStudent" checked />
                        ${relationshipType.studentRole?cap_first}
                      </@bs3form.checkbox>
                      <@bs3form.checkbox>
                        <input type="checkbox" name="notifyOldAgent" <#if action != 'add'>checked<#else>disabled</#if> />
                        ${relationshipType.agentRole?cap_first}s no longer removed
                      </@bs3form.checkbox>
                      <@bs3form.checkbox>
                        <input type="checkbox" name="notifyNewAgent" <#if action != 'remove'>checked<#else>disabled</#if> />
                        ${relationshipType.agentRole?cap_first}s no longer assigned
                      </@bs3form.checkbox>
                    </@bs3form.labelled_form_group>
                    <button type="button" class="btn btn-danger cancel-scheduled-change">Cancel scheduled change</button>
                  </#assign>
                  <button
                          type="button"
                          class="btn btn-default btn-sm use-popover"
                          data-html="true"
                          data-trigger="click focus"
                          data-title="Cancel scheduled change"
                          data-content="${popover}"
                          data-placement="left"
                  >
                    Cancel
                  </button>
                </@f.form>
              </td>
            </#if>
          </tr>
        </#list>
        </tbody>
      </table>
      <script nonce="${nonce()}">
        jQuery(function ($) {
          $('body').on('click', '.cancel-scheduled-change', function () {
            var $this = $(this), $form = $this.closest('.popover').data('creator').closest('form');
            $this.closest('div').find('input:checked').each(function () {
              $form.find('input[name=' + $(this).prop('name') + ']').val('true');
            });
            $form.submit();
          });
        });
      </script>
    </#if>
    </@modal.body>
    </@modal.wrapper>
  </@modal.modal>

  <@modal.modal id="change-agent"></@modal.modal>

  <h2>Record of meetings</h2>

  <#if !(canReadMeetings!true)>
    <div class="alert alert-danger">You do not have permission to view the meetings for this student</div>
  <#else>
    <section class="meetings">
      <#if !meetings?has_content>
        <div class="alert alert-info">No meeting records exist for this academic year</div>
      <#else>
        <table class="table table-striped table-condensed expanding-row-pairs">
          <thead>
          <tr>
            <th>Date</th>
            <th>Title</th>
            <th>Status</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          <#list meetings as meeting>
            <#assign expand = defaultExpand(meeting)/>
            <tr ${expand?string('class=expand','')} <#if meeting.deleted>class="deleted subtle"</#if>>
              <td data-sortby="${meeting.meetingDate.millis?c}"><@fmt.interval start=meeting.meetingDate end=meeting.meetingEndDate /></td>
              <td>${meeting.title!}</td>
              <td><@meetingStateSummary meeting /></td>
              <td>
                <#if meeting.scheduled>
                  <#assign editUrl><@routes.profiles.edit_scheduled_meeting_record meeting studentCourseDetails thisAcademicYear relationshipType /></#assign>
                <#else>
                  <#assign editUrl><@routes.profiles.edit_meeting_record studentCourseDetails thisAcademicYear meeting /></#assign>
                </#if>
                <#if ((meeting.scheduled && can.do_with_selector("Profiles.ScheduledMeetingRecord.Manage", meeting, relationshipType)) ||
                (!meeting.scheduled && user.universityId! == meeting.creator.universityId && !meeting.approved))
                >
                  <div class="pull-right">
                    <i class="fa fa-spinner fa-spin invisible"></i>
                    <span class="dropdown">
											<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown" href="#">Actions <span class="caret"></span></a>
											<ul class="dropdown-menu pull-right">
												<li>
													<a href="${editUrl}" class="edit-meeting-record <#if meeting.deleted>disabled</#if>" title="Edit record">Edit</a>
												</li>
												<li>
													<a href="<@routes.profiles.delete_meeting_record meeting />" class="delete-meeting-record <#if meeting.deleted>disabled</#if>"
                             title="Delete record">Delete</a>
												</li>
												<li>
													<a href="<@routes.profiles.restore_meeting_record meeting />" class="restore-meeting-record <#if !meeting.deleted>disabled</#if>"
                             title="Restore record">Restore</a>
												</li>
												<li>
													<a href="<@routes.profiles.purge_meeting_record meeting />" class="purge-meeting-record <#if !meeting.deleted>disabled</#if>"
                             title="Purge record">Purge</a>
												</li>
											</ul>
										</span>
                  </div>
                <#elseif user.sysadmin>
                  <div class="pull-right">
                    <i class="fa fa-spinner fa-spin invisible"></i>
                    <span class="dropdown">
											<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown" href="#">Actions <span class="caret"></span></a>
											<ul class="dropdown-menu pull-right">
												<li>
													<a href="<@routes.profiles.delete_meeting_record meeting />" class="delete-meeting-record <#if meeting.deleted>disabled</#if>"
                             title="Delete record">Delete</a>
												</li>
												<li>
													<a href="<@routes.profiles.restore_meeting_record meeting />" class="restore-meeting-record <#if !meeting.deleted>disabled</#if>"
                             title="Restore record">Restore</a>
												</li>
												<li>
													<a href="<@routes.profiles.purge_meeting_record meeting />" class="purge-meeting-record <#if !meeting.deleted>disabled</#if>"
                             title="Purge record">Purge</a>
												</li>
											</ul>
										</span>
                  </div>
                </#if>
              </td>
            </tr>
            <tr>
              <td colspan="4">
                <#if meeting.missedReason?has_content>
                  <div class="alert alert-danger">
                    <p>This meeting did not take place because:</p>
                    <blockquote>${meeting.missedReason}</blockquote>
                  </div>
                </#if>

                <#if meeting.description??>
                  <div class="description">
                    <#noescape>${meeting.escapedDescription}</#noescape>
                  </div>
                </#if>

                <#if (meeting.attachments?? && meeting.attachments?size > 0)>
                  <#assign mrDownloadUrl><@routes.profiles.download_meeting_record_attachment relationshipType meeting /></#assign>
                  <@fmt.download_attachments meeting.attachments mrDownloadUrl "for this meeting record" meeting.title?replace("/", "-")?url />
                </#if>

                <#if meeting.scheduled>
                  <@scheduledMeetingState meeting />
                <#else>
                  <@meetingState meeting />
                </#if>
              </td>
            </tr>
          </#list>
          </tbody>
        </table>
      </#if>

      <#if relationshipsToDisplay?has_content && manageableRelationships?has_content>
        <p>
          <#if canCreateScheduledMeetings && features.scheduledMeetings>
            <a class="btn btn-default new-meeting-record"
               href="<@routes.profiles.create_scheduled_meeting_record studentCourseDetails thisAcademicYear relationshipType />">Schedule meeting</a>
          <#else>
            <#if relationshipsToDisplay?size gt 1>
              <#assign tooltipTitle = "You can't use Tabula to schedule a meeting with any of your ${relationshipType.agentRole?lower_case}s. This isn't allowed by their departments." />
            <#else>
              <#assign agentFirstName = relationshipsToDisplay[0].agentMember.firstName />
              <#assign tooltipTitle = "You can't use Tabula to schedule a meeting with ${agentFirstName}. This isn't allowed by ${agentFirstName}'s department." />
            </#if>
            <span tabindex="0" class="btn btn-default disabled use-tooltip" title="${tooltipTitle}" data-placement="top">
						Schedule meeting
					</span>
          </#if>
          <#if canCreateMeetings>
            <a class="btn btn-default new-meeting-record"
               href="<@routes.profiles.create_meeting_record studentCourseDetails thisAcademicYear relationshipType />">Record meeting</a>
            <a class="btn btn-default new-meeting-record"
               href="<@routes.profiles.create_missed_meeting_record studentCourseDetails thisAcademicYear relationshipType />">Record missed meeting</a>
          </#if>
        </p>
      </#if>

      <@modal.modal id="meeting-modal"></@modal.modal>
    </section>
  </#if>

  <#function defaultExpand meeting>
    <#if meeting.scheduled>
      <#return meeting.pendingAction && can.do("Profiles.ScheduledMeetingRecord.Confirm", meeting) />
    <#else>
      <#return meeting.pendingApprovalBy(user) || meeting.pendingRevisionBy(user) />
    </#if>
  </#function>

  <#macro meetingStateSummary meeting>
    <#if meeting.scheduled>
      <#if meeting.pendingAction>
        Pending confirmation by ${meeting.creator.fullName}
      <#elseif meeting.missed>
        Missed
      <#else>
        Scheduled
      </#if>
    <#else>
      <#if meeting.missed>
        Missed
      <#elseif meeting.approved>
        Approved
      <#elseif meeting.rejected>
        Pending revision by ${meeting.creator.fullName}
      <#else>
        Pending approval by ${meeting.pendingApprovalsDescription}
      </#if>
    </#if>
  </#macro>

  <#macro meetingState meeting>
    <#if meeting.pendingRevisionBy(user)>
      <p class="very-subtle">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
      <div class="alert alert-info">
        <#list meeting.rejectedApprovals as rejectedApproval>
          <p>This record has been returned with comments by ${rejectedApproval.approver.fullName} because:</p>
          <blockquote>${rejectedApproval.comments}</blockquote>
          <p>Please edit the record and submit it for approval again.</p>
        </#list>
        <a class="edit-meeting-record btn btn-primary" href="<@routes.profiles.edit_meeting_record studentCourseDetails thisAcademicYear meeting/>">Edit</a>
      </div>
    <#elseif meeting.rejectedBy(currentMember)>
      <p class="very-subtle">Pending revision. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
      <div class="alert alert-info">
        <p>You sent this record back to the other party, who will review the record and submit it for approval again.</p>
      </div>
    <#elseif meeting.rejected>
      <p class="very-subtle">Pending revision. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
      <div class="alert alert-info">
        <#list meeting.rejectedApprovals as rejectedApproval>
          <p>This record has been returned to ${meeting.creator.fullName} with comments by ${rejectedApproval.approver.fullName}.</p>
          <blockquote>${rejectedApproval.comments}</blockquote>
          <p>${meeting.creator.fullName} needs to edit the record and submit it for approval again.</p>
        </#list>
      </div>
    <#elseif meeting.pendingApprovalBy(user)>
      <p class="very-subtle">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
      <div class="alert alert-info">
        This record needs your approval. Please review, then approve or return it with comments.
        <#if meetingApprovalWillCreateCheckpoint[meeting.id]>
          <br />
          Approving this meeting record will mark a monitoring point as attended.
        </#if>
      </div>

      <!-- not a spring form as we don't want the issue of binding multiple sets of data to the same command -->
      <#assign formAction><@routes.profiles.save_meeting_approval meeting /></#assign>
      <@f.form method="post" cssClass="approval double-submit-protection" id="meeting-${meeting.id}" action=formAction>
        <@bs3form.form_group>
          <@bs3form.radio>
            <input type="radio" name="approved" value="true">
            Approve
          </@bs3form.radio>
          <@bs3form.radio>
            <input class="reject" type="radio" name="approved" value="false">
            Return with comment
          </@bs3form.radio>
          <div class="rejection-comment" style="display:none">
            <textarea class="form-control" name="rejectionComments"></textarea>
          </div>
        </@bs3form.form_group>

        <div class="submit-buttons">
          <button type="submit" class="btn btn-primary spinnable spinner-auto">Submit</button>
        </div>
      </@f.form>
    <#elseif meeting.pendingApproval>
      <p class="very-subtle">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
      <div class="alert alert-info">
        This meeting record needs to be approved by ${meeting.pendingApprovalsDescription}.
      </div>
    <#else>
      <p class="very-subtle">
        ${(meeting.format.description)!"Unknown format"} between ${meeting.allParticipantNames}<#if meeting.missed> recorded as missed</#if>.
        Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate />.
        <#if meeting.approved && (meeting.approvedBy?has_content || meeting.approvedDate?has_content)>
          Approved<#if meeting.approvedBy?has_content> by ${meeting.approvedBy.fullName},</#if><#if meeting.approvedDate?has_content> <@fmt.date meeting.approvedDate /></#if>
        </#if>
      </p>
    </#if>
  </#macro>

  <#macro scheduledMeetingState meeting>
    <#local pendingAction = meeting.pendingAction />
    <#local canConvertMeeting = can.do("Profiles.ScheduledMeetingRecord.Confirm", meeting) />

    <#if pendingAction && !canConvertMeeting>
      <p class="very-subtle">Pending confirmation. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
      <#if !meeting.pendingActionBy(user)>
        <div class="alert alert-info">
          ${meeting.creator.fullName} needs to confirm that this meeting took place.
        </div>
      </#if>
    <#elseif pendingAction && canConvertMeeting>
      <p class="very-subtle">Pending confirmation. ${(meeting.format.description)!"Unknown format"} between ${meeting.allParticipantNames}. Created
        by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></p>
      <#if meeting.deleted>
        <p class="very-subtle">This meeting has been deleted</p>
      <#else>
        <div class="alert alert-info">
          Please confirm whether this scheduled meeting took place.
        </div>
        <#assign formAction><@routes.profiles.choose_action_scheduled_meeting_record meeting studentCourseDetails thisAcademicYear meeting.relationships[0].relationshipType /></#assign>
        <@f.form method="post" cssClass="scheduled-action" id="meeting-${meeting.id}"
              action=formAction>
          <@bs3form.form_group>
            <@bs3form.radio>
              <input checked type="radio" name="action" value="confirm"
                     data-formhref="<@routes.profiles.confirm_scheduled_meeting_record meeting studentCourseDetails thisAcademicYear meeting.relationships[0].relationshipType />">
              Confirm
            </@bs3form.radio>
            <@bs3form.radio>
              <input type="radio" name="action" value="reschedule" />
              Reschedule
            </@bs3form.radio>
            <@bs3form.radio>
              <input type="radio" name="action" value="missed" class="reject"
                     data-formhref="<@routes.profiles.missed_scheduled_meeting_record meeting meeting.relationships[0].relationshipType />">
              Record that the meeting did not take place
            </@bs3form.radio>
            <div class="rejection-comment" style="display:none">
              <textarea class="form-control" name="missedReason"></textarea>
            </div>
          </@bs3form.form_group>

          <div class="ajaxErrors alert alert-danger" style="display: none;"></div>
          <button type="submit" class="btn btn-primary">Submit</button>
        </@f.form>
      </#if>
    <#elseif meeting.missed>
      <div class="alert alert-danger">
        <#if meeting.missedReason?has_content>
          <p>This meeting did not take place because:</p>
          <blockquote>${meeting.missedReason}</blockquote>
        <#else>
          <p>This meeting did not take place (no reason given)</p>
        </#if>
      </div>
      <p class="very-subtle">${(meeting.format.description)!"Unknown format"} between ${meeting.allParticipantNames}. Created by ${meeting.creator.fullName}
        , <@fmt.date meeting.lastUpdatedDate /></p>
    <#else>
      <p class="very-subtle">${(meeting.format.description)!"Unknown format"} between ${meeting.allParticipantNames}. Created by ${meeting.creator.fullName}
        , <@fmt.date meeting.lastUpdatedDate /></p>
    </#if>
  </#macro>

</#escape>
