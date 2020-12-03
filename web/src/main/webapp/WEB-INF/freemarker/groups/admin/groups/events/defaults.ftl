<#escape x as x?html>
  <#import "*/group_components.ftlh" as components />

  <div class="deptheader">
    <h1>Set default event properties</h1>
    <h4 class="with-related"><span class="muted">for</span> ${smallGroupSet.name}</h4>
  </div>

  <p>Defaults set here will apply to any new events. You can adjust these properties for individual events.</p>

  <@f.form method="post" action="" modelAttribute="command">
    <@f.errors cssClass="error form-errors" />

    <@bs3form.labelled_form_group path="defaultTutors" labelText="Tutors">
      <@bs3form.flexipicker path="defaultTutors" placeholder="User name" list=true multiple=true auto_multiple=false />
    </@bs3form.labelled_form_group>

    <@components.week_selector "defaultWeeks" allTerms smallGroupSet />

    <@bs3form.labelled_form_group path="defaultDay" labelText="Day">
      <@f.select path="defaultDay" id="defaultDay" cssClass="form-control">
        <@f.option value="" label=""/>
        <@f.options items=allDays itemLabel="name" itemValue="asInt" />
      </@f.select>
    </@bs3form.labelled_form_group>

  <#-- The time-picker causes the entire page to become a submit button, can't work out why -->
    <div class="dateTimePair">
      <@bs3form.labelled_form_group path="defaultStartTime" labelText="Start time">
        <@f.input path="defaultStartTime" cssClass="time-picker startDateTime form-control" />
        <input class="endoffset" type="hidden" data-end-offset="3600000" />
      </@bs3form.labelled_form_group>

      <@bs3form.labelled_form_group path="defaultEndTime" labelText="End time">
        <@f.input path="defaultEndTime" cssClass="time-picker endDateTime form-control" />
      </@bs3form.labelled_form_group>
    </div>

    <@bs3form.labelled_form_group path="defaultDeliveryMethod" labelText="Delivery method">
      <@f.select path="defaultDeliveryMethod" cssClass="form-control">
        <@f.option value="" label=""/>
        <@f.options items=allDeliveryMethods itemLabel="description" itemValue="entryName" />
      </@f.select>
    </@bs3form.labelled_form_group>

    <div class="online-event-fields">
      <@bs3form.labelled_form_group path="defaultOnlinePlatform" labelText="Online Platform">
        <@f.select path="defaultOnlinePlatform" cssClass="form-control">
          <@f.option value="" label=""/>
          <@f.options items=allOnlinePlatforms itemLabel="entryName" itemValue="entryName" />
        </@f.select>
      </@bs3form.labelled_form_group>

      <#assign onlineDeliveryUrlHelpText>
        <p>A 'Go to event' link that will be shown on the student's timetable. Use a direct link to the event wherever possible.</p>
      </#assign>
      <#assign onlineDeliveryUrlLabel>
        Online delivery URL <@fmt.help_popover id="onlineDeliveryUrlHelp" content="${onlineDeliveryUrlHelpText}" html=true />
      </#assign>
      <@bs3form.labelled_form_group path="defaultOnlineDeliveryUrl" labelText=onlineDeliveryUrlLabel>
        <@f.input path="defaultOnlineDeliveryUrl" cssClass="form-control" />
      </@bs3form.labelled_form_group>
    </div>
    <div class="f2f-event-fields">
      <@bs3form.labelled_form_group path="defaultLocation" labelText="Location">
        <@f.hidden path="defaultLocationId" />
        <@f.input path="defaultLocation" cssClass="form-control" />
        <div class="help-block small">
          <a href="#" id="showLocationAlias">Use a different name for this location</a>
        </div>
      </@bs3form.labelled_form_group>

      <div class="alert alert-info" id="namedLocationAlert" style="display: none">
        <p>
          This location couldn't be found on the campus map.
        </p>

        <@bs3form.checkbox path="useNamedLocation">
          <@f.checkbox path="useNamedLocation" /> Use this location anyway
        </@bs3form.checkbox>
      </div>

      <@bs3form.labelled_form_group path="defaultLocationAlias" labelText="Location display name" cssClass="location-alias-form-group">
        <@f.input path="defaultLocationAlias" cssClass="form-control" />
        <div class="help-block small">
          <a href="#" id="removeLocationAlias">Use the standard location name</a>
        </div>
      </@bs3form.labelled_form_group>
    </div>

    <@bs3form.checkbox "resetExistingEvents">
      <@f.checkbox path="resetExistingEvents" value="true" />
      Reset existing events to these defaults
    </@bs3form.checkbox>

    <div class="submit-buttons">
      <input
              type="submit"
              class="btn btn-primary"
              name="create"
              value="Save"
      />
      <a class="btn btn-default" href="${cancelUrl}">Cancel</a>
    </div>
  </@f.form>
</#escape>
