<#escape x as x?html>

<style>
  .feature-flags th { text-align: right; }
  .feature-flags td, .feature-flags th {
    padding: 1em 0.5em;
  }
</style>

<h1>Feature flags</h1>

<p>
This controller uses ActiveMQ to broadcast changes to other instances.
When you set a property here, <strong>all</strong> properties will be kept in sync.
</p>

<table class="feature-flags">
  <#list currentValues as feature>
    <tr>
      <th style="width: 1px;">${feature.name}</th>
      <td>${feature.value?string}</td>
      <td>
        <form id="${feature.name}_form" action="<@url page="/sysadmin/features"/>" method="POST">
          <input type="hidden" name="name" value="${feature.name}">
          Set to
          <input type="submit" name="value" value="false" class="btn btn-default"> or
          <input type="submit" name="value" value="true" class="btn btn-default">
        </form>
      </td>
    </tr>
  </#list>
</table>

</#escape>