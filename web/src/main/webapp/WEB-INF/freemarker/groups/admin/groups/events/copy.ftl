<#escape x as x?html>
  <#import "*/group_components.ftlh" as components />

  <div class="deptheader">
    <div class="pull-right">
      <#assign group = smallGroupEvent.group />
      <@fmt.bulk_email_group group.students "Email the students attending this event" />
    </div>
    <h1 class="with-settings">Copy event</h1>
    <h4 class="with-related"><span class="muted">for</span> ${group.name}</h4>
  </div>
    <#assign commandName="copySmallGroupEventCommand" />
    <@spring.hasBindErrors name="${commandName}">
        <#if errors.hasErrors()>
          <div class="alert alert-danger">
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

  <@f.form method="post" action="" modelAttribute=commandName>
    <#assign newRecord=true />
    <#include "_event_fields.ftl" />

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
