<#escape x as x?html>

  <#if setState == 'close'>
    <#assign pastTense = "d" />
    <#assign stateChange = false />
  <#else>
    <#assign pastTense = "" />
    <#assign stateChange = true />
  </#if>

  <div class="deptheader">
    <h1>${setState?cap_first} groups for ${academicYear.toString}</h1>
    <h4 class="with-related"><span class="muted">in</span> ${department.name}</h4>
  </div>

  <#if info.requestParameters.batchOpenSuccess?? && setState == "open">
    <div class="alert alert-info">
      Students have been notified that these groups are now ${setState} for self-sign-up
    </div>
  </#if>

  <@f.form method="post" action="" modelAttribute="setList" cssClass="form-horizonatal form-tiny">
    <p> ${setState?cap_first} these groups for self sign-up.
      <#if setState == "open"> Students will be notified via email that they can now sign up for these groups in Tabula. </#if>
    </p>
    <div class="control-group">
      <input class="btn btn-primary spinnable spinner-auto" type="submit" value="${setState?cap_first}">
    </div>


    <div id="scroll-container">
      <table id="groups-table" class="table table-striped sortable-table">

        <thead>
        <tr>
          <th><@bs3form.selector_check_all /></th>
          <th class="sortable">Module/Group</th>
        </tr>
        </thead>
        <tbody>
        <#list groupSets as set>
          <tr <#if set.openForSignups == stateChange> class='use-tooltip' title='This group is already ${setState}${pastTense} for sign-ups'</#if> >
            <td>
              <@f.checkbox
              class=(set.openForSignups == stateChange)?string('','collection-checkbox')
              path="checkedGroupsets"
              disabled=(set.openForSignups == stateChange)
              value=set.id
              />
            </td>
            <td>
						<span class=${(set.openForSignups == stateChange)?string("muted","")}>
							<@fmt.module_name set.module false /> - ${set.name}
						</span>
            </td>
          </tr>
        </#list>

        </tbody>
      </table>
    </div>

  </@f.form>
</#escape>

<script type="text/javascript" nonce="${nonce()}">
  jQuery("#groups-table").bigList({});
</script>
