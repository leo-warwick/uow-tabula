<#escape x as x?html>

  <h1>Publish groups in ${department.name}</h1>
  <#if info.requestParameters.batchReleaseSuccess??>
    <div class="alert alert-info">
      These small group allocations have been published
    </div>
  </#if>

  <@f.form method="post" action="" modelAttribute="moduleList">
    <p>Publish these groups so they are shown in Tabula to:</p>
    <@bs3form.checkbox path="notifyStudents">
      <@f.checkbox path="notifyStudents"/> Students
    </@bs3form.checkbox>
    <@bs3form.checkbox path="notifyTutors">
      <@f.checkbox path="notifyTutors"/>Tutors
    </@bs3form.checkbox>
    <hr>
    <@bs3form.checkbox path="sendEmail">
      <@f.checkbox path="sendEmail" />
      Send an email about this and any future changes to group allocation
    </@bs3form.checkbox>

    <@bs3form.form_group>
      <input class="btn btn-primary" type="submit" value="Publish">
    </@bs3form.form_group>

    <div id="scroll-container">
      <div id="tooltip-container"></div>
      <table id="modules-table" class="table table-striped sortable-table">
        <thead>
        <tr>
          <th><@bs3form.selector_check_all /></th>
          <th class="sortable">Module</th>
        </tr>
        </thead>
        <tbody>
        <#list modules as module>
          <tr <#if !module.hasUnreleasedGroupSets(academicYear)> class='use-tooltip' title='Groups for this module have already been published'</#if>
                  data-container="#tooltip-container">
            <td>
              <@f.checkbox
              class=module.hasUnreleasedGroupSets(academicYear)?string('collection-checkbox','')
              path="checkedModules"
              disabled=(!module.hasUnreleasedGroupSets(academicYear))
              value=module.code
              />
            </td>
            <td>
							<span class="${module.hasUnreleasedGroupSets(academicYear)?string('','muted')}">
								<@fmt.module_name module false />
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
  jQuery("#modules-table").bigList({});
</script>
