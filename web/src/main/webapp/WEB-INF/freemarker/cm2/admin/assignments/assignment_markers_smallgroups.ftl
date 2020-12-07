<#escape x as x?html>
  <#import "*/assignment_components.ftl" as components />
  <#import "*/cm2_macros.ftl" as cm2 />
  <#include "assign_marker_macros.ftl" />

  <@cm2.assignmentHeader "Assign markers" assignment "for" />

  <div class="fix-area">
    <@components.assignment_wizard 'markers' assignment.module false assignment />
    <p class="btn-toolbar">
      <a class="return-items btn btn-default" href="<@routes.cm2.assignmentmarkers assignment mode />">
        Return to drag and drop
      </a>
    </p>
    <#if sets?has_content>
      <#assign actionUrl><@routes.cm2.assignmentmarkerssmallgroups assignment mode /></#assign>
    <@f.form method="post" action=actionUrl cssClass="dirty-check double-submit-protection" modelAttribute="smallGroupCommand">
      <div class="has-error"><@f.errors cssClass="error help-block" /></div>

    <#if allocationWarnings?size != 0>
      <div class="alert alert-info">
        <h4>Allocation warnings</h4>

        <ul>
          <#list allocationWarnings as warning>
            <li>${warning}</li>
          </#list>
        </ul>

        <@bs3form.checkbox path="allowSameMarkerForSequentialStages">
          <@f.checkbox path="allowSameMarkerForSequentialStages" id="allowSameMarkerForSequentialStages" /> Save these allocations anyway
        </@bs3form.checkbox>
      </div>
    </#if>

    <@bs3form.labelled_form_group path="" labelText="Small group set">
    <@f.select path="smallGroupSet" cssClass="form-control set-selector">
      <option disabled<#if !smallGroupCommand.smallGroupSet??> selected</#if>></option>
    <#list sets as set>
      <option value="${set.id}"<#if smallGroupCommand.smallGroupSet?? && smallGroupCommand.smallGroupSet.id == set.id> selected</#if>>${set.name}</option>
    </#list>
    </@f.select>
    </@bs3form.labelled_form_group>

    <#list sets as set>
      <#assign allocation = setsMap[set.id]>
      <#assign index = 0>
      <div class="set" id="set-${set.id}" style="display:none">
        <#list allocationOrder as roleOrStage>
          <#assign groupAllocations = mapGet(allocation.allocations, roleOrStage)![]>
          <#assign stages = mapGet(stageNames, roleOrStage)>
          <div class="role" data-stages="${stages?join(",")}">
            <h2>${roleOrStage}</h2>
            <#list groupAllocations as group>
              <div class="group well">
                <h5>${group.name}</h5>
                <div class="row">
                  <div class="col-md-5">
                    <strong>Students</strong>
                    <ul>
                      <#list group.students as student>
                        <li>
                          ${student.fullName}&nbsp;${student.warwickId!userId}
                        </li>
                      </#list>
                    </ul>
                  </div>
                  <div class="col-md-7">
                    <@bs3form.labelled_form_group path="setIdWithGroupMarkerAllocations[${set.id}][${index}]" labelText="Marker">
                      <@f.hidden path="setIdWithGroupMarkerAllocations[${set.id}][${index}].group" value=group.id />
                      <#list stages as stage>
                        <@f.hidden path="setIdWithGroupMarkerAllocations[${set.id}][${index}].stages" value=stage />
                      </#list>
                      <@f.select path="setIdWithGroupMarkerAllocations[${set.id}][${index}].marker" cssClass="form-control marker-select">
                        <#-- list tutors from this group before tutors from other groups -->
                        <#list (group.tutors + group.otherTutors) as tutor>
                          <#assign groupMarkerAllocation = smallGroupCommand.setIdWithGroupMarkerAllocations[set.id][index] />
                          <#-- even though all three below  are not null it still gives ftl exception locally if we remove this if condition as part of validation screen.Perhaps something to do with lazy list??? -->
                          <#if groupMarkerAllocation?? && groupMarkerAllocation.marker?? && groupMarkerAllocation.marker.userId??>
                            <#assign selected = groupMarkerAllocation.marker.userId == tutor.userId/>
                          <#else>
                            <#assign selected = false />
                          </#if>
                          <option value="${tutor.userId}"<#if selected> selected</#if>>${tutor.fullName}</option>

                        </#list>
                      </@f.select>
                    </@bs3form.labelled_form_group>
                    <#assign index = index + 1>
                  </div>
                </div>
              </div>
            </#list>
          </div>
        </#list>
        <div class="fix-footer">
          <div class="alert alert-info">
            <i class="fa fa-info-circle"></i> Any existing marker allocations for this assignment will be overwritten
          </div>

          <input
                  type="submit"
                  class="btn btn-primary"
                  name="${ManageAssignmentMappingParameters.createAndAddSubmissions}"
                  value="Save and continue"
          />
          <input
                  type="submit"
                  class="btn btn-primary"
                  name="${ManageAssignmentMappingParameters.createAndAddMarkers}"
                  value="Save and exit"
          />
        </div>
      </div>
    </#list>

      <script type="text/javascript" nonce="${nonce()}">
        (function ($) {
          // show / hide sets and enable their inputs
          $('.set-selector').on('change', function () {
            var set = $(this).val();
            $('.set').hide().find('select').prop('disabled', true);
            var $set = $('#set-' + set);
            $set.show().find('select').prop('disabled', false);
          }).trigger('change');
        })(jQuery);
      </script>
    </@f.form>
    <#else>
      <div>
        <p>
          The small group set allocation for <@fmt.module_name module=assignment.module withFormatting=false />
          in ${assignment.academicYear.toString} could not be imported.
        </p>
        The list of markers in the marking workflow for this assignment must include all tutors in the small group
        set allocation. Return to the <a href="<@routes.cm2.editassignmentdetails assignment />">
          Assignment details</a> section and ensure that you have added all the small group tutors as markers.
      </div>
    </#if>
  </div>
</#escape>
