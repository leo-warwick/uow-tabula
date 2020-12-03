<#escape x as x?html>
  <#import "*/group_components.ftlh" as components />

  <@bs3form.labelled_form_group path="title" labelText="Title">
    <@f.input path="title" cssClass="form-control" />
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="tutors" labelText="Tutors">
    <@bs3form.flexipicker path="tutors" placeholder="User name" list=true multiple=true auto_multiple=false />
  </@bs3form.labelled_form_group>

  <@components.week_selector "weeks" allTerms smallGroupSet />

  <@bs3form.labelled_form_group path="day" labelText="Day">
    <@f.select path="day" id="day" cssClass="form-control">
      <@f.option value="" label=""/>
      <@f.options items=allDays itemLabel="name" itemValue="asInt" />
    </@f.select>
  </@bs3form.labelled_form_group>

<#-- The time-picker causes the entire page to become a submit button, can't work out why -->
  <div class="dateTimePair">
    <@bs3form.labelled_form_group path="startTime" labelText="Start time">
      <@f.input path="startTime" cssClass="time-picker startDateTime form-control" />
      <input class="endoffset" type="hidden" data-end-offset="3600000" />
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="endTime" labelText="End time">
      <@f.input path="endTime" cssClass="time-picker endDateTime form-control" />
    </@bs3form.labelled_form_group>
  </div>

  <@bs3form.labelled_form_group path="deliveryMethod" labelText="Delivery method">
    <@f.select path="deliveryMethod" cssClass="form-control">
      <@f.option value="" label=""/>
      <@f.options items=allDeliveryMethods itemLabel="description" itemValue="entryName" />
    </@f.select>
  </@bs3form.labelled_form_group>

  <div class="online-event-fields">
    <@bs3form.labelled_form_group path="onlinePlatform" labelText="Online Platform">
      <@f.select path="onlinePlatform" cssClass="form-control">
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
    <@bs3form.labelled_form_group path="onlineDeliveryUrl" labelText=onlineDeliveryUrlLabel>
        <@f.input path="onlineDeliveryUrl" cssClass="form-control" />
    </@bs3form.labelled_form_group>
  </div>

  <div class="f2f-event-fields">
    <@bs3form.labelled_form_group path="location" labelText="Location">
      <@f.hidden path="locationId" />
      <@f.input path="location" cssClass="form-control" />
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

    <@bs3form.labelled_form_group path="locationAlias" labelText="Location display name" cssClass="location-alias-form-group">
      <@f.input path="locationAlias" cssClass="form-control" />
      <div class="help-block small">
        <a href="#" id="removeLocationAlias">Use the standard location name</a>
      </div>
    </@bs3form.labelled_form_group>
  </div>

  <#assign moreDetailsHelpText>
    <p>An optional 'More Details' link to the specified webpage from the student's timetable.</p>
  </#assign>
  <#assign moreDetailsLabel>
    Link <@fmt.help_popover id="linkHelp" content="${moreDetailsHelpText}" html=true />
  </#assign>
  <@bs3form.labelled_form_group path="relatedUrl" labelText=moreDetailsLabel>
    <@f.input path="relatedUrl" cssClass="form-control" />
  </@bs3form.labelled_form_group>

  <#assign moreDetailsLinkTextHelpText>
    <p>If a More Details link is specified, you can also specify the link text for the link that is generated.</p>
  </#assign>
  <#assign moreDetailsLinkTextLabel>
    Link text <@fmt.help_popover id="linkHelp" content="${moreDetailsLinkTextHelpText}" html=true />
  </#assign>
  <@bs3form.labelled_form_group path="relatedUrlTitle" labelText=moreDetailsLinkTextLabel>
    <@f.input path="relatedUrlTitle" cssClass="form-control" />
  </@bs3form.labelled_form_group>

</#escape>
