<#escape x as x?html>
  <#import "*/modal_macros.ftlh" as modal />
  <#import "*/_profile_link.ftl" as pl />

  <script type="text/javascript" nonce="${nonce()}">
    (function ($) {
      $(function () {
        $('.select-all').change(function (e) {
          $('.attendees').selectDeselectCheckboxes(this);
        });

      });
    }(jQuery));
  </script>

  <#assign studentMembers = [] />

  <#if command.additionalStudent??>
    <#assign already_in = false />
    <#list command.members as student>
      <#if student.universityId == command.additionalStudent.universityId>
        <#assign already_in = true />
      </#if>
    </#list>

    <#if !already_in><#assign studentMembers = studentMembers + [command.additionalStudent] /></#if>
  </#if>

  <#list command.members as student>
    <#if !(command.removeAdditionalStudent??) || command.removeAdditionalStudent.universityId != student.universityId>
      <#assign studentMembers = studentMembers + [student] />
    </#if>
  </#list>

  <@modal.modal id="addAdditional-modal"></@modal.modal>

  <div class="recordCheckpointForm" data-check-checkpoints="true">
    <div style="display:none;" class="forCloning">
      <div class="btn-group" data-toggle="radio-buttons">
        <button type="button" class="btn btn-default" data-state="" aria-label="Set to 'Not recorded'">
          <i class="fa fa-fw fa-minus" title="Set to 'Not recorded'"></i>
        </button>
        <button type="button" class="btn btn-default btn-unauthorised<#if eventInFuture> disabled use-tooltip" data-container="body"
                title="You can only record authorised absence for future events</#if>" data-state="unauthorised" aria-label="Set to 'Missed (unauthorised)'">
          <i class="fa fa-fw fa-times" title="Set to 'Missed (unauthorised)'"></i>
        </button>
        <button type="button" class="btn btn-default btn-authorised" data-state="authorised" aria-label="Set to 'Missed (authorised)'">
          <i class="fa fa-fw fa-times-circle-o" title="Set to 'Missed (authorised)'"></i>
        </button>
          <button type="button" class="btn btn-default btn-attended
            <#if !showAttendedInPersonOption> disabled use-tooltip" data-container="body"
                    title="You cannot record in-person attendance for an online-only event
            <#elseif eventInFuture> disabled use-tooltip" data-container="body"
                    title="You can only record authorised absence for future events
            </#if>" data-state="attended" aria-label="Set to 'Attended - in person'">
            <i class="fa fa-fw fa-user-check" title="Set to 'Attended - in person'"></i>
          </button>
          <button type="button" class="btn btn-default btn-attended
            <#if !showAttendedRemotelyOption> disabled use-tooltip" data-container="body"
                    title="You cannot record remote attendance for a face-to-face only event
            <#elseif eventInFuture> disabled use-tooltip" data-container="body"
                    title="You can only record authorised absence for future events
            </#if>" data-state="attended-remotely" aria-label="Set to 'Attended'">
            <i class="fa fa-fw fa-laptop-house" title="Set to 'Attended - remotely'"></i>
          </button>
      </div>
    </div>

    <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
    <div class="fix-area">
      <div class="deptheader">
        <h1>Record attendance</h1>
        <h4 class="with-related"><span class="muted">for</span>
          <#if command.event.title?has_content>${command.event.title},</#if>
          ${command.event.group.groupSet.module.code?upper_case} ${command.event.group.groupSet.nameWithoutModulePrefix},
          ${command.event.group.name}</h4>
      </div>

      <@modal.modal id="print-modal">
        <@modal.wrapper>
          <@modal.header>
            <h3 class="modal-title">Print register</h3>
          </@modal.header>
          <form method="get" action="<@routes.groups.registerPdf command.event />" style="margin-bottom: 0;">
            <input type="hidden" name="week" value="${command.week?c}" />
            <@modal.body>
              <p>Generate a PDF of this register so that it can be printed.</p>

              <@bs3form.checkbox>
                <input type="hidden" name="_showPhotos" value="" />
                <input type="checkbox" name="showPhotos" value="true" <#if (userSetting('registerPdfShowPhotos')!'t') == 't'>checked</#if> />
                Show student photos
              </@bs3form.checkbox>

              <#assign displayName = userSetting('registerPdfDisplayName')!'name' />
              <@bs3form.labelled_form_group "" "Name display">
                <@bs3form.radio>
                  <input type="radio" name="displayName" value="name" <#if displayName == 'name'>checked</#if> />
                  Show student name only
                </@bs3form.radio>
                <@bs3form.radio>
                  <input type="radio" name="displayName" value="id" <#if displayName == 'id'>checked</#if> />
                  Show University ID only
                </@bs3form.radio>
                <@bs3form.radio>
                  <input type="radio" name="displayName" value="both" <#if displayName == 'both'>checked</#if> />
                  Show both name and University ID
                </@bs3form.radio>
              </@bs3form.labelled_form_group>

              <#assign displayCheck = userSetting('registerPdfDisplayCheck')!'checkbox' />
              <@bs3form.labelled_form_group "" "Fill area">
                <@bs3form.radio>
                  <input type="radio" name="displayCheck" value="checkbox" <#if displayCheck == 'checkbox'>checked</#if> />
                  Include checkbox
                </@bs3form.radio>
                <@bs3form.radio>
                  <input type="radio" name="displayCheck" value="line" <#if displayCheck == 'line'>checked</#if> />
                  Include signature line
                </@bs3form.radio>
              </@bs3form.labelled_form_group>
            </@modal.body>
            <@modal.footer>
              <button type="submit" class="btn btn-primary">Save as PDF for printing</button>
              <button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Close</button>
            </@modal.footer>
          </form>
        </@modal.wrapper>
      </@modal.modal>
      <div class="pull-right">
        <button type="button" class="btn btn-default" data-toggle="modal" data-target="#print-modal">Print</button>
        <@fmt.bulk_email_students studentMembers />
      </div>

      <h6>
        ${command.event.day.name} <@fmt.time event.startTime /> - <@fmt.time event.endTime />
        , <@fmt.singleWeekFormat week=command.week academicYear=command.event.group.groupSet.academicYear dept=command.event.group.groupSet.module.adminDepartment />
        <#if command.event.tutors.users?has_content>
          <br /><@fmt.p number=command.event.tutors.users?size singular="Tutor" shownumber=false/>: <#list command.event.tutors.users as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list>
        </#if>
      </h6>

      <#if recordAttendanceHistory?has_content>
        <p>
          <strong>Attendance last updated</strong>: <@fmt.date date=(recordAttendanceHistory?last).recorded />
          <button type="button" class="btn btn-default btn-xs" data-toggle="modal" data-target="#small-group-register-history-modal"><i class="fal fa-history" aria-hidden="true"></i><span class="sr-only">View history</span></button>
        </p>

        <@modal.modal id="small-group-register-history-modal">
          <@modal.wrapper cssClass="modal-lg">
            <@modal.header>
              <h3 class="modal-title">Attendance history</h3>
            </@modal.header>
            <@modal.body>
              <table class="table table-condensed table-striped">
                <thead>
                  <tr>
                    <th class="col-sm-4">Date</th>
                    <th class="col-sm-3">User</th>
                    <th class="col-sm-5">Changes</th>
                  </tr>
                </thead>
                <tbody>
                  <#list recordAttendanceHistory as history>
                    <tr>
                      <td>
                        <@fmt.date date=history.recorded />
                        <#if history.wasRecordedLate>
                          <span tabindex="0" class="tabula-tooltip" data-title="Recorded after the day of the event"><i class="fal fa-clock" aria-label="Recorded after the day of the event"></i></span>
                        </#if>
                      </td>
                      <td>${history.user.fullName!(history.user.userId)}</td>
                      <td>
                        <#if history.changes??>
                          <#if history.changes?has_content>
                            <ul class="fa-ul">
                              <#list history.changes as change>
                                <li>
                                  <span class="fa-li">
                                    <#if change.state.dbValue == 'attended'>
                                      <i class="fa fa-check attended" aria-label="Attended"></i>
                                    <#elseif change.state.dbValue == 'authorised'>
                                      <i class="fa fa-times-circle-o authorised" aria-label="Missed (authorised)"></i>
                                    <#elseif change.state.dbValue == 'unauthorised'>
                                      <i class="fa fa-times unauthorised" aria-label="Missed (unauthorised)"></i>
                                    <#else>
                                      <i class="fa fa-minus" aria-label="Not recorded"></i>
                                    </#if>
                                  </span>
                                  ${change.student.fullName} (${change.student.universityId})
                                </li>
                              </#list>
                            </ul>
                          <#else>
                            <em>No attendance changes</em>
                          </#if>
                        <#else>
                          <em>Unknown</em>
                        </#if>
                      </td>
                    </tr>
                  </#list>
                </tbody>
              </table>
            </@modal.body>
            <@modal.footer>
              <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </@modal.footer>
          </@modal.wrapper>
        </@modal.modal>
      </#if>

      <#if !studentMembers?has_content>
        <p><em>There are no students allocated to this group.</em></p>
      </#if>

      <#macro studentRow student added_manually linked_info={}>
        <tr id="student-${student.universityId}">
          <td>
            <@pl.profile_link student.universityId />
          </td>
          <td>
            <@fmt.member_photo student "tinythumbnail" true />
          </td>
          <td>
            ${student.firstName}
          </td>
          <td>
            ${student.lastName}
          </td>
          <td>
            ${student.universityId}
          </td>
          <td>
            <div
            <div class="pull-right">
              <#local hasState = mapGet(command.studentsState, student.universityId)?? />
              <#if hasState>
                <#local currentState = mapGet(command.studentsState, student.universityId) />
              </#if>
              <#if hasState>
                <div class="hidden-desktop visible-print">
                  <span class="label">${currentState.description}</span><br />
                  ${(command.attendanceMetadata(student.universityId))!}
                </div>
              </#if>
              <select id="studentsState-${student.universityId}" name="studentsState[${student.universityId}]" data-universityid="${student.universityId}">
                <option value="" <#if !hasState>selected</#if>>Not recorded</option>
                <#list allCheckpointStates as state>
                  <option value="${state.code}" <#if hasState && currentState.code == state.code>selected</#if>>${state.description}</option>
                </#list>
              </select>
              <#if features.attendanceMonitoringNote>
                <#local hasNote = mapGet(mapGet(command.attendanceNotes, student), command.occurrence)?? />
                <#if hasNote>
                  <#local note = mapGet(mapGet(command.attendanceNotes, student), command.occurrence) />
                  <#if note.hasContent>
                    <a
                            id="attendanceNote-${student.universityId}-${command.occurrence.id}"
                            class="btn btn-default use-tooltip attendance-note edit"
                            title="Edit attendance note"
                            aria-label="Edit attendance note"
                            data-container="body"
                            href="<@routes.groups.editNote student=student occurrence=command.occurrence returnTo=((info.requestedUri!"")?url) />&dt=${.now?string('iso')}"
                    >
                      <i class="fa fa-pencil-square-o attendance-note-icon"></i>
                    </a>
                  <#else>
                    <a
                            id="attendanceNote-${student.universityId}-${command.occurrence.id}"
                            class="btn btn-default use-tooltip attendance-note"
                            title="Add attendance note"
                            aria-label="Add attendance note"
                            data-container="body"
                            href="<@routes.groups.editNote student=student occurrence=command.occurrence returnTo=((info.requestedUri!"")?url) />"
                    >
                      <i class="fa fa-pencil-square-o attendance-note-icon"></i>
                    </a>
                  </#if>
                <#else>
                  <a
                          id="attendanceNote-${student.universityId}-${command.occurrence.id}"
                          class="btn btn-default use-tooltip attendance-note"
                          title="Add attendance note"
                          aria-label="Add attendance note"
                          data-container="body"
                          href="<@routes.groups.editNote student=student occurrence=command.occurrence returnTo=((info.requestedUri!"")?url) />"
                  >
                    <i class="fa fa-pencil-square-o attendance-note-icon"></i>
                  </a>
                </#if>
              </#if>
            </div>
            <#if added_manually>
              <#local popoverText>
                <p>${student.fullName} has been manually added to this event.</p>

                <#if linked_info.replacesAttendance??>
                  <p>Attending instead of
                    <#if linked_info.replacesAttendance.occurrence.event.title?has_content>${linked_info.replacesAttendance.occurrence.event.title},</#if>
                    ${linked_info.replacesAttendance.occurrence.event.group.groupSet.name}, ${linked_info.replacesAttendance.occurrence.event.group.name}:
                    ${linked_info.replacesAttendance.occurrence.event.day.name} <@fmt.time linked_info.replacesAttendance.occurrence.event.startTime />
                    - <@fmt.time linked_info.replacesAttendance.occurrence.event.endTime />
                    , <@fmt.singleWeekFormat week=linked_info.replacesAttendance.occurrence.week academicYear=linked_info.replacesAttendance.occurrence.event.group.groupSet.academicYear dept=linked_info.replacesAttendance.occurrence.event.group.groupSet.module.adminDepartment />
                    .
                  </p>
                </#if>

                <button class="btn btn-mini btn-danger" type="button" data-action="remove" data-student="${student.universityId}">Remove from this occurrence
                </button>
              </#local>

              <a class="use-popover" data-container="body" data-content="${popoverText}" data-html="true" href="#" data-trigger="click focus">
                Manually added
              </a>
            </#if>

            <#if linked_info.replacedBy??>
              <#list linked_info.replacedBy as replaced_by>
                <#local titleText><#compress>
                  Replaced by attendance at
                  <#if replaced_by.occurrence.event.title?has_content>${replaced_by.occurrence.event.title},</#if>
                  ${replaced_by.occurrence.event.group.groupSet.name}, ${replaced_by.occurrence.event.group.name}:
                  ${replaced_by.occurrence.event.day.name} <@fmt.time replaced_by.occurrence.event.startTime /> - <@fmt.time replaced_by.occurrence.event.endTime />,
                  <@fmt.singleWeekFormat week=replaced_by.occurrence.week academicYear=replaced_by.occurrence.event.group.groupSet.academicYear dept=replaced_by.occurrence.event.group.groupSet.module.adminDepartment />.
                </#compress></#local>
                <a class="use-tooltip" data-container="body" title="${titleText}" data-html="true" href="#">
                  Replaced
                </a>
              </#list>
            </#if>

            <@spring.bind path="command.studentsState[${student.universityId}]">
              <#list status.errorMessages as err>${err}</#list>
              <div class="text-error"><@f.errors path="studentsState[${student.universityId}]" cssClass="error"/></div>
            </@spring.bind>
          </td>
        </tr>
        <script type="text/javascript" nonce="${nonce()}">
          AttendanceRecording.createButtonGroup('#studentsState-${student.universityId}');
          AttendanceRecording.wireButtons('#student-${student.universityId}');
        </script>
      </#macro>

      <div class="attendees">
        <@f.form id="recordAttendance" action="" method="post" data\-occurrence="${command.occurrence.id}" class="dirty-check">
          <script type="text/javascript" nonce="${nonce()}">
            AttendanceRecording.bindButtonGroupHandler();
          </script>

          <table class="table table-striped table-condensed">
            <thead class="fix-header">
            <tr>
              <th colspan="6" class="no-sort">
								<span class="studentsLoadingMessage">
									<i class="id7-koan-spinner id7-koan-spinner--xs id7-koan-spinner--inline" aria-hidden="true"></i> Loading&hellip;
								</span>
                <script type="text/javascript" nonce="${nonce()}">
                  jQuery(function ($) {
                    $('.studentsLoadingMessage').hide();
                  });
                </script>
                <div class="pull-left">
                  <div class="form-inline">
                    <label for="additionalStudentQuery">Add student:</label>
                    <span class="profile-search input-group"
                          data-target="<@routes.groups.students_json command.event.group.groupSet />?excludeEvent=${command.event.id}&excludeWeek=${command.week}"
                          data-form="<@routes.groups.addAdditionalStudent command.event command.week />" data-modal="#addAdditional-modal">
											<input type="hidden" name="additionalStudent" />
											<input style="width: 300px;" class="form-control" type="text" name="query" value="" id="additionalStudentQuery"
                             placeholder="Search for a student to add&hellip;" />
											<span class="input-group-btn">
												<button class="btn btn-default" type="button" aria-label="Search">
													<i class="fa fa-search"></i>
												</button>
											</span>
										</span>

                    <#assign helpText>
                      <p>If a student not normally in this group has attended or will be attending this session, you can search for them here.</p>

                      <p>Only students registered to small groups in this module are included in the search results.</p>
                    </#assign>
                    <a href="#"
                       class="use-popover"
                       data-title="Adding a student not normally present to an event occurrence"
                       data-trigger="click focus"
                       data-placement="bottom"
                       data-html="true"
                       data-content="${helpText}"><i class="icon-question-sign icon-fixed-width"></i></a>
                  </div>
                </div>
                <div class="pull-right" style="display: none;">
									<span class="checkAllMessage">
										Record all attendance
										<#assign popoverContent>
                      <p><i class="fa fa-minus fa-fw"></i> Not recorded</p>
                      <p><i class="fa fa-times fa-fw unauthorised"></i> Missed (unauthorised)</p>
                      <p><i class="fa fa fa-times-circle-o fa-fw authorised"></i> Missed (authorised)</p>
                      <p><i class="fa fa-check fa-fw attended"></i> Attended - in person</p>
                      <p><i class="fa fa-laptop-house fa-fw attended-remotely"></i> Attended - remotely</p>
                    </#assign>
										<a class="use-popover"
                       tabindex="0" role="button"
                       aria-label="Help"
                       data-title="Key"
                       data-placement="bottom"
                       data-container="body"
                       data-content='${popoverContent}'
                       data-trigger="click focus"
                       data-html="true"><#compress>
											<i class="fa fa-question-circle"></i>
                      </#compress></a>
										</span>
                  <div class="btn-group">
                    <button type="button" aria-label="Set all to 'Not recorded'" class="btn btn-default">
                      <i class="fa fa-minus fa-fw" title="Set all to 'Not recorded'"></i>
                    </button>
                    <button type="button" aria-label="Set all to 'Missed (unauthorised)'"
                            class="btn btn-default btn-unauthorised<#if eventInFuture> disabled use-tooltip" data-container="body"
                            title="You can only record authorised absence for future events</#if>">
                      <i class="fa fa-times fa-fw" title="Set all to 'Missed (unauthorised)'"></i>
                    </button>
                    <button type="button" aria-label="Set all to 'Missed (authorised)'" class="btn btn-default btn-authorised">
                      <i class="fa fa fa-times-circle-o fa-fw" title="Set all to 'Missed (authorised)'"></i>
                    </button>
                    <button type="button" aria-label="Set all to 'Attended - in person'" class="btn btn-default btn-attended
                      <#if !showAttendedInPersonOption> disabled use-tooltip" data-container="body" title="You cannot record in-person attendance for an online-only event
                      <#elseif eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events
                      </#if>
                      ">
                      <i class="fa fa-user-check fa-fw" title="Set all to 'Attended - in person'"></i>
                    </button>
                    <button type="button" aria-label="Set all to 'Attended - remotely'" class="btn btn-default btn-attended
                      <#if !showAttendedRemotelyOption> disabled use-tooltip" data-container="body" title="You cannot record remote attendance for a face-to-face only event
                      <#elseif eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events
                      </#if>
                      ">
                      <i class="fa fa-laptop-house fa-fw" title="Set all to 'Attended - remotely'"></i>
                    </button>
                  </div>
                  <#if features.attendanceMonitoringNote>
                    <a class="btn btn-default use-tooltip attendance-note bulk-attendance-note"
                       title="Add an attendance note for all students"
                       aria-label="Add an attendance note for all students"
                       href="<@routes.groups.bulkEditNote command.occurrence />">
                      <i class="fa fa-pencil-square-o"></i>
                    </a>
                  </#if>
                </div>
              </th>
            </tr>
            <tr>
              <th class="no-sort"></th>
              <th>Photo</th>
              <th>First name</th>
              <th>Last name</th>
              <th>ID</th>
              <th class="no-sort"></th>
            </tr>
            </thead>
            <tbody>

            <#if command.additionalStudent??>
              <#assign already_in = false />
              <#list command.members as student>
                <#if student.universityId == command.additionalStudent.universityId>
                  <#assign already_in = true />
                </#if>
              </#list>

              <#if !already_in><@studentRow command.additionalStudent true command.linkedAttendance /></#if>
            </#if>

            <#list command.members as student>
              <#if !(command.removeAdditionalStudent??) || command.removeAdditionalStudent.universityId != student.universityId>
                <@studentRow student command.manuallyAddedUniversityIds?seq_contains(student.universityId) mapGet(command.attendances, student) />
              </#if>
            </#list>

            </tbody>
          </table>

          <#if studentMembers?has_content>

            <div class="fix-footer submit-buttons">
              <div class="pull-right">
                <input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
                <a class="btn btn-default dirty-check-ignore" href="${returnTo}">Cancel</a>
              </div>
              <div class="pull-left checkpoints-message alert alert-info" style="display: none;">
                Saving this attendance will set monitoring points for <span class="inner"></span>
              </div>
            </div>

          </#if>
        </@f.form>
      </div>

    </div>
  </div>

  <script type="text/javascript" nonce="${nonce()}">
    var sorterFieldLookup = {
      2: 'firstName', // I don't know why this is one-fewer than the header index, but there you go
      3: 'lastName',
      4: 'universityId'
    };
    var sorterOrderLookup = {
      0: 'asc',
      1: 'desc'
    };
    jQuery(function ($) {
      var $table = $('.attendees table');
      var $printForm = $('#print-modal').find('form');
      $table.tablesorter({
        cancelSelection: false,
        initialized: function() { AttendanceRecording.recordMonitoringPointsScripts() } ,
        headers: {
          0: {sorter: false},
          1: {sorter: false},
          2: {sorter: false},
          6: {sorter: false}
        }
      }).on('sortEnd', function () {
        $printForm.find('input[name^=studentSort]').remove();
        $.each($table.data('tablesorter').sortList, function (i, fieldOrderPair) {
          $printForm.append($('<input/>').attr({
            type: 'hidden',
            name: 'studentSortFields[' + i + ']',
            value: sorterFieldLookup[fieldOrderPair[0]]
          })).append($('<input/>').attr({
            type: 'hidden',
            name: 'studentSortOrders[' + i + ']',
            value: sorterOrderLookup[fieldOrderPair[1]]
          }));
        });
      });
    });
  </script>
</#escape>
