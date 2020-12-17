<#import "*/group_components.ftlh" as components />
<#escape x as x?html>
  <h1>Events without online delivery URL</h1>

  <p>
    This page displays Online/Hybrid small group events without online delivery URL specified.
  </p>

  <#if smallGroupSets?size gt 0>
    <#list smallGroupSets as pair>
      <#assign set = pair._1()>
      <#assign module = set.module>
      <#assign deliveryMethodsWithEvents = pair._2()>
      <#assign delivoryMethods = deliveryMethodsWithEvents?keys>
      <h4>
        <span class="mod-code">${module.code?upper_case}</span>
        <span class="groupset-name">
					<a href="<@routes.groups.editset set/>">${set.name}</a>
				</span>
      </h4>

      <#list delivoryMethods as method>
        <p><span class="very-subtle">${method.entryName} events:</span> </p>
        <#assign events = mapGet(deliveryMethodsWithEvents, method)>
        <ul>
          <#list events as event>
            <li>
                ${event.group.name!"Group"}: <@components.eventShortDetails event />
              <a href="<@routes.groups.editseteventseditevent event/>?returnTo=${(info.requestedUri!"")?url}" class="btn btn-default btn-xs">Edit</a>
            </li>
          </#list>
        </ul>
      </#list>
    </#list>
  <#else>
    <div class="alert alert-info">
      All small group events have set some online delivery URL.
    </div>
  </#if>
</#escape>
