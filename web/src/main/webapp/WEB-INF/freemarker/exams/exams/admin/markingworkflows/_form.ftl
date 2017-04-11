<#--

Adding or editing a new marking workflow

-->
<#if view_type="add">
	<#assign submit_text="Create" />
<#elseif view_type="edit">
	<#assign submit_text="Save" />
</#if>

<#assign department=command.department />

<#escape x as x?html>
<#compress>

<h1 class="with-settings">Define marking workflow</h1>
<#assign commandName="command" />

<@f.form method="post" action="${form_url}" commandName=commandName>
	<@f.errors cssClass="error form-errors" />

	<#--
	Common form fields.
	-->
	<@bs3form.labelled_form_group path="name" labelText="Name">
		<@f.input path="name" cssClass="form-control" />
		<div class="help-block">
			A descriptive name that will be used to refer to this marking workflow elsewhere.
		</div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="markingMethod" labelText="Workflow type">

		<#assign isDisabled = (view_type == "edit") />

		<@f.select disabled=isDisabled path="markingMethod" cssClass="form-control">
			<option <#if !command.markingMethod?has_content>selected="selected"</#if> value=""></option>
			<#if !isExams>
				<option value="StudentsChooseMarker"
					<#if ((command.markingMethod.toString)!"") = "StudentsChooseMarker">selected="selected"</#if>
					data-firstrolename="Marker"
					data-firstrolename=""
				>
					Students choose marker
				</option>
				<#if features.newSeenSecondMarkingWorkflows || ((command.markingMethod.toString)!"") == "SeenSecondMarking">
					<option value="SeenSecondMarking" class="uses-second-markers"
							<#if ((command.markingMethod.toString)!"") = "SeenSecondMarking">selected="selected"</#if>
							data-firstrolename="First marker"
							data-secondrolename="Second marker"
							>
						Seen second marking
					</option>
				</#if>
				<#if !features.newSeenSecondMarkingWorkflows || ((command.markingMethod.toString)!"") == "SeenSecondMarkingLegacy">
					<option value="SeenSecondMarkingLegacy" class="uses-second-markers"
							<#if ((command.markingMethod.toString)!"") = "SeenSecondMarkingLegacy">selected="selected"</#if>
							data-firstrolename="First marker"
							data-secondrolename="Second marker"
							>
						Seen second marking <#if features.newSeenSecondMarkingWorkflows>(legacy)</#if>
					</option>
				</#if>

				<option value="ModeratedMarking" class="uses-second-markers"
					<#if ((command.markingMethod.toString)!"") = "ModeratedMarking">selected="selected"</#if>
					data-firstrolename="Marker"
					data-secondrolename="Moderator"
				>
					Moderated marking
				</option>
			</#if>
			<option value="FirstMarkerOnly"
				<#if ((command.markingMethod.toString)!"") = "FirstMarkerOnly">selected="selected"</#if>
				data-firstrolename="Marker"
			>
				First marker only
			</option>
		</@f.select>


		<#if view_type=="edit">
			<div class="help-block">
				It is not possible to modify the marking method once a marking workflow has been created.
			</div>
		</#if>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="firstMarkers" labelText="${command.firstMarkerRoleName!'Marker'}">
		<@bs3form.flexipicker path="firstMarkers" list=true multiple=true />
	</@bs3form.labelled_form_group>

	<div class="second-markers-container hide">
		<@bs3form.labelled_form_group path="secondMarkers" labelText="${command.secondMarkerRoleName!'Second Marker'}">
			<@bs3form.flexipicker path="secondMarkers" list=true multiple=true />
		</@bs3form.labelled_form_group>
	</div>

	<#-- Script to change marker labels to reflect type dependant role names -->
	<script>
		jQuery(function($) {
			$('#markingMethod').change(function() {
				var $selectedOption = $(this).find('option:selected')
				var firstrolename = $selectedOption.data('firstrolename');
				var secondrolename = $selectedOption.data('secondrolename');
				if (firstrolename) $('label[for=firstMarkers]').html(firstrolename);
				if (secondrolename) $('label[for=secondMarkers]').html(secondrolename);
			});
		});
	</script>

	<div>
		<input type="submit" value="${submit_text}" class="btn btn-primary">
		<a class="btn btn-default" href="<@routes.exams.markingWorkflowList department />">Cancel</a>
	</div>

</@f.form>

</#compress>
</#escape>