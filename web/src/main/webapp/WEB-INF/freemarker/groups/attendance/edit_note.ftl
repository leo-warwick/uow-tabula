<#escape x as x?html>
  <#import "*/modal_macros.ftlh" as modal />
  <#if success??>
    <#assign isAdd = !command.attendanceNote.note?has_content && !command.attendanceNote.attachment?has_content && !command.attendanceNote.absenceType?has_content />
    <div
            class="attendance-note-success"
            data-linkid="#attendanceNote-${student.universityId}-${command.occurrence.id}"
            data-state="<#if isAdd>Add<#else>Edit</#if>"
    ></div>
  </#if>

  <@modal.wrapper cssClass="modal-lg" enabled=(isModal!false && !isIframe)>

    <#if !isIframe>
      <@modal.header enabled=isModal!false>
        <#if command.isNew()>
          <h3 class="modal-title">Create attendance note for ${student.fullName}</h3>
        <#else>
          <h3 class="modal-title">Edit attendance note for ${student.fullName}</h3>
        </#if>
      </@modal.header>
    </#if>


    <@modal.body enabled=isModal!false />

    <#if isModal!false>
      <@modal.footer>
        <form class="double-submit-protection">
					<span class="submit-buttons">
						<button class="btn btn-primary spinnable" type="submit" name="submit" data-loading-text="Saving&hellip;">
							Save
						</button>
						<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Cancel</button>
					</span>
        </form>
      </@modal.footer>

    <#else>

      <p>
        <#if command.customState??>
          ${command.customState.description}:
        <#elseif command.attendance??>
          ${command.attendance.state.description}:
        <#else>
          Not recorded:
        </#if>
        <#if command.occurrence.event.title?has_content>${command.occurrence.event.title},</#if>
        ${command.occurrence.event.group.groupSet.name},
        ${command.occurrence.event.group.name},
        ${command.occurrence.event.day.name} <@fmt.time command.occurrence.event.startTime /> - <@fmt.time command.occurrence.event.endTime />,
        <@fmt.singleWeekFormat week=command.occurrence.week academicYear=command.occurrence.event.group.groupSet.academicYear dept=command.occurrence.event.group.groupSet.module.adminDepartment />

        <#if command.customState?? && command.attendance?? && command.customState.dbValue != command.attendance.state.dbValue>
          <small class="subtle help-block">
            This attendance has not yet been saved.
          </small>
        </#if>
      </p>

      <@f.form id="attendance-note-form" method="post" enctype="multipart/form-data" action="" modelAttribute="command" class="double-submit-protection">

        <@bs3form.labelled_form_group "absenceType" "Absence type">
          <@f.select path="absenceType" cssClass="form-control">
            <option value="" style="display: none;">Please select one&hellip;</option>
            <#list allAbsenceTypes as type>
              <@f.option value="${type.dbValue}" label="${type.description}" />
            </#list>
          </@f.select>
        </@bs3form.labelled_form_group>

        <@bs3form.labelled_form_group "note" "Note">
          <@f.textarea path="note" cssClass="form-control" rows="5" cssStyle="height: 150px;" />
        </@bs3form.labelled_form_group>

        <#if command.attachedFile?has_content>
          <@bs3form.labelled_form_group "attachedFile" "Attached file">
            <@fmt.download_link
            filePath="/groups/note/${command.member.universityId}/${command.occurrence.id}/attachment/${command.attachedFile.name}"
            mimeType=command.attachedFile.mimeType
            title="Download file ${command.attachedFile.name}"
            text="Download ${command.attachedFile.name}"
            />
            &nbsp;
            <@f.hidden path="attachedFile" value="${command.attachedFile.id}" />
            <i class="fa fa-times-circle remove-attachment"></i>

            <small class="very-subtle help-block">
              This is the file attached to this attendance note.
              Click the remove link next to a document to delete it.
            </small>

          </@bs3form.labelled_form_group>

          <script nonce="${nonce()}">
            jQuery(function ($) {
              $(".remove-attachment").on("click", function (e) {
                $(this).closest('form').find('.attendance-file').show();
                $(this).closest(".control-group").remove();
                return false;
              });
            });
          </script>
        </#if>

        <div class="attendance-file" <#if command.attachedFile?has_content>style="display:none;"</#if>>
          <@bs3form.filewidget basename="file" types=[] multiple=false />
        </div>

        <#if !isIframe>

          <div class="form-actions">
            <input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
            <a class="btn btn-default" href="${returnTo}">Cancel</a>
          </div>

        </#if>

      </@f.form>

    </#if>

  </@modal.wrapper>

</#escape>