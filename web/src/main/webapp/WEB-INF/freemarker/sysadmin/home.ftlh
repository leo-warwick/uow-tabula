<h1>Web system administrating system screen page</h1>

<#if user.masquerading>
  <p>Oh, hello ${user.fullName}. <em>Or should I say, ${user.realUser.fullName}?!</em></p>
</#if>

<div class="row">

  <div class="col-md-8">
    <h2>Normal regular stuff</h2>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/permissions-helper" />">Permissions helper</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/global-permissions" />">Global permissions</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/departments" />">List all departments in the system</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/relationships" />">Student relationship types</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/relationships/check" />">Student relationship checker</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/attendancetemplates" />">Attendance monitoring templates</a></p>
    <p><a class="btn btn-default" href="<@url page="/admin/masquerade" />">Masquerade</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/audit/search" />">List audit events (Index version)</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/jobs/list" />">Background jobs</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/emails/list" />">Email queue</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/features" />">Set feature flags</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/statistics" />">Internal statistics</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/complete-scheduled-notification" />">Complete scheduled notifications</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/manual-membership-warning" />">Send warnings about manual membership</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/event-calendar" />">Upcoming event calendar</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/metrics" />">Metrics</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/objectstorage" />">Object storage</a></p>
    <p><a class="btn btn-default" href="<@url page="/profiles/admin/timetablechecker" />">Timetable feed checker</a></p>
    <p><a class="btn btn-default" href="<@url page="/admin/scientia-rooms" />">Syllabus+ room mappings</a></p>
    <p><a class="btn btn-default" href="<@url page="/sysadmin/csrf" />">CSRF report/enforce</a></p>
  </div>

  <div class="col-md-4">
    <h2>God mode</h2>

    <@f.form method="post" action="${url('/sysadmin/god')}" modelAttribute="">
      <#if user.god>
        <input type="hidden" name="action" value="remove" />
        <button id="disable-godmode-button" class="btn btn-primary">Disable God mode</button>
      <#else>
        <button id="enable-godmode-button" class="btn btn-primary">Enable God mode</button>
      </#if>
    </@f.form>

    <h2>Imports</h2>

    <p>
      <@f.form method="post" action="${url('/sysadmin/import')}" modelAttribute="">
        <input class="btn btn-danger" type="submit" value="Departments, modules, routes etc." data-toggle="confirm-submit" data-message="Really? Could take a minute" />
      </@f.form>
    </p>


    <p>
      <@f.form method="post" action="${url('/sysadmin/import-department')}" modelAttribute="blankForm">
    <div class="input-group">
      <@f.input id="import-modules-dept" path="deptCode" cssClass="form-control" placeholder="deptCodes" /><span class="input-group-btn"><input
                class="btn btn-danger" type="submit" value="Modules" data-toggle="confirm-submit" data-message="Really? Could take a minute" /></span>
    </div>
    </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/import-sits')}" modelAttribute="">
        <input class="btn btn-danger" type="submit" value="SITS assignments" data-toggle="confirm-submit" data-message="Really? Could take a minute">
      </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/import-sits-all-years')}" modelAttribute="">
        <input class="btn btn-danger" type="submit" value="SITS assignments, ALL YEARS"
               onclick="return confirm('Really? Could take a minute.') && confirm('No seriously. This one takes forever. Really sure?')">
      </@f.form>
    </p>

    <p>
        <@f.form method="post" action="${url('/sysadmin/import-sits-individual-year')}" modelAttribute="blankForm">
    <div class="input-group">
      <select name="academicYear" class="form-control">
          <#list academicYears as ay>
            <option value="${ay.startYear}" <#if ay.startYear == currentAcademicYear.startYear>selected</#if>>${ay.toString}</option>
          </#list>
      </select>
      <span class="input-group-btn"><input class="btn btn-danger" type="submit" value="SITS Assignments (specific year)" data-toggle="confirm-submit" data-message="Really? Could take a minute" /></span>
    </div>
      </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/import-module-lists')}" modelAttribute="">
        <input class="btn btn-danger" type="submit" value="SITS module lists" data-toggle="confirm-submit" data-message="Really? Could take a minute">
      </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/import-profiles')}" modelAttribute="blankForm">
    <div class="input-group">
      <@f.input id="import-profiles-dept" path="deptCode" cssClass="form-control" placeholder="deptCode (optional)" /><span class="input-group-btn"><input
                class="btn btn-danger" type="submit" value="Profiles" data-toggle="confirm-submit" data-message="Really? Could take a minute" /></span>
    </div>
    </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/import-profiles')}" modelAttribute="blankForm">
        <textarea id="import-profiles-specific" name="members" class="form-control" placeholder="University IDs (one per line)" rows="2"></textarea>
        <input class="btn btn-danger" type="submit" value="Specific profile(s)">
      </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/stamp-missing')}" modelAttribute="">
        <button class="btn btn-danger" type="submit">Stamp missing profiles in SITS</button>
      </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/recheck-missing')}" modelAttribute="blankForm">
      Recheck missing stamps from
    <div class="input-group">
      <@f.input id="check-from" path="from" cssClass="date-time-picker form-control" placeholder="Click to pick a date" /><span class="input-group-btn"><input
                class="btn btn-danger" type="submit" value="Re-check" data-toggle="confirm-submit" data-message="Really? Could take a while." /></span>
    </div>
    </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/import-module-registrations')}" modelAttribute="blankForm">
        <div class="input-group">
          <select name="academicYear" class="form-control">
            <#list academicYears as ay>
              <option value="${ay.startYear}" <#if ay.startYear == currentAcademicYear.startYear>selected</#if>>${ay.toString}</option>
            </#list>
          </select>
          <span class="input-group-btn"><input class="btn btn-danger" type="submit" value="Module Registrations" data-toggle="confirm-submit" data-message="Really? Could take a minute" /></span>
        </div>
      </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/import-progression-decisions')}" modelAttribute="blankForm">
        <div class="input-group">
          <select name="academicYear" class="form-control">
            <#list academicYears as ay>
              <option value="${ay.startYear}" <#if ay.startYear == currentAcademicYear.startYear>selected</#if>>${ay.toString}</option>
            </#list>
          </select>
          <span class="input-group-btn"><input class="btn btn-danger" type="submit" value="Progression Decisions" data-toggle="confirm-submit" data-message="Really? Could take a minute" /></span>
        </div>
      </@f.form>
    </p>

    <p>
        <@f.form method="post" action="${url('/sysadmin/import-student-awards')}" modelAttribute="blankForm">
            <div class="input-group">
      <select name="academicYear" class="form-control">
          <#list academicYears as ay>
            <option value="${ay.startYear}" <#if ay.startYear == currentAcademicYear.startYear>selected</#if>>${ay.toString}</option>
          </#list>
      </select>
      <span class="input-group-btn"><input class="btn btn-danger" type="submit" value="Student Awards" data-toggle="confirm-submit" data-message="Really? Could take a minute" /></span>
    </div>
      </@f.form>
    </p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/import-locations')}" modelAttribute="blankForm">
      Check for missing event locations
    <div class="input-group">
      <select name="academicYear" class="form-control">
        <#list academicYears as ay>
          <option value="${ay.startYear}" <#if ay.startYear == currentAcademicYear.startYear>selected</#if>>${ay.toString}</option>
        </#list>
      </select>
      <span class="input-group-btn"><input class="btn btn-danger" type="submit" value="Re-check"
                                           data-toggle="confirm-submit" data-message="Really? Could take a while." /></span>
    </div>
    </@f.form>
    </p>

    <h2>Indexing</h2>

    <#macro reindex_form name text>
      <p>
      <@f.form method="post" action="${url('/sysadmin/index/run-'+name)}" modelAttribute="blankForm">
        Rebuild ${text} from
        <div class="input-group">
          <@f.input id="index-${name}-from" path="from" cssClass="date-time-picker form-control" placeholder="Click to pick a date" /><span
                  class="input-group-btn"><input class="btn btn-danger" type="submit" value="Index"
                                                 data-toggle="confirm-submit" data-message="Really? Could take a while." /></span>
        </div>
        <#if name = 'profiles'>
          For department
          <@f.input id="index-${name}-dept" path="deptCode" cssClass="form-control" placeholder="Dept code (optional)" />
        <#elseif name = 'audit'>
          For event type
          <@f.input id="index-${name}-eventType" path="eventType" cssClass="form-control" placeholder="Event type (optional)" />
        </#if>
      </@f.form>
      </p>
    </#macro>

    <@reindex_form 'audit' 'audit event index' />
    <@reindex_form 'profiles' 'profiles index' />
    <@reindex_form 'notifications' 'notification stream index' />

    <h4>Maintenance mode</h4>

    <#if maintenanceModeEnabled>
      <p>Currently <strong>enabled</strong>.</p>
    <#else>
      <p>Disabled.</p>
    </#if>

    <p><a href="<@url page="/sysadmin/maintenance"/>">Update settings</a></p>

    <h4>Emergency message</h4>
    <#if emergencyMessageEnabled>
      <p>Currently <strong>enabled</strong>.</p>
    <#else>
      <p>Disabled.</p>
    </#if>
    <p><a href="<@url page="/sysadmin/emergencymessage"/>">Update settings</a></p>

    <p>
      <@f.form method="post" action="${url('/sysadmin/jobs/create-test')}" modelAttribute="">
        <input class="btn btn-default" type="submit" value="Create test job">
      </@f.form>
    </p>

  </div>
</div>
