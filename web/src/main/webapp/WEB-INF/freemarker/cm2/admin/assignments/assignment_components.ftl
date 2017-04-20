<#ftl strip_text=true />

<#macro wizard_link label is_first is_active is_available tooltip="" url="">
<span class="arrow-right<#if !is_first> arrow-left</#if><#if is_active> active</#if><#if is_available && !is_active> use-tooltip</#if>"
	  <#if is_available && !is_active>title="${tooltip}"</#if>><#compress>
	<#if is_available && !is_active>
        <a href="${url}">${label}</a>
	<#else>
	${label}
	</#if>
</#compress></span>
</#macro>

<#macro assignment_wizard current_step module edit_mode=false assignment={}>
<p class="progress-arrows">
	<#if edit_mode>
			<#local details_url><@routes.cm2.editassignmentdetails assignment /></#local>
			<@wizard_link
	label="Assignment details"
	is_first=true
	is_active=(current_step == 'details')
	is_available=true
	tooltip="Edit details"
	url=details_url />

			<#local feedback_url><@routes.cm2.assignmentfeedback assignment 'edit' /></#local>
			<@wizard_link
	label="Feedback"
	is_first=false
	is_active=(current_step == 'feedback')
	is_available=true
	tooltip="Edit feedback"
	url=feedback_url />

			<#local students_url><@routes.cm2.assignmentstudents assignment 'edit'/></#local>
			<@wizard_link
	label="Students"
	is_first=false
	is_active=(current_step == 'students')
	is_available=true
	tooltip="Edit students"
	url=students_url />

			<#local markers_url><@routes.cm2.assignmentmarkers assignment 'edit'/></#local>
			<@wizard_link
	label="Markers"
	is_first=false
	is_active=(current_step == 'markers')
	is_available=true
	tooltip="Edit markers"
	url=markers_url />

			<#local submissions_url><@routes.cm2.assignmentsubmissions assignment 'edit' /></#local>
			<@wizard_link
	label="Submissions"
	is_first=false
	is_active=(current_step == 'submissions')
	is_available=true
	tooltip="Edit submissions"
	url=submissions_url />

			<#local options_url><@routes.cm2.assignmentoptions assignment 'edit' /></#local>
			<@wizard_link
	label="Options"
	is_first=false
	is_active=(current_step == 'options')
	is_available=true
	tooltip="Edit options"
	url=options_url />

			<#local review_url><@routes.cm2.assignmentreview assignment /></#local>
			<@wizard_link
	label="Review"
	is_first=false
	is_active=(current_step == 'review')
	is_available=true
	tooltip="Review"
	url=review_url />
		<#else>
		<#assign displayLink=assignment.id?? />
		<#local details_url><#if displayLink><@routes.cm2.editassignmentdetails assignment /></#if></#local>
		<@wizard_link
		label="Assignment details"
		is_first=true
		is_active=(current_step == 'details')
		is_available=displayLink
		tooltip="Edit details"
		url=details_url />

		<#local feedback_url><#if displayLink><@routes.cm2.assignmentfeedback assignment 'new'/></#if></#local>
		<@wizard_link
		label="Feedback"
		is_first=false
		is_active=(current_step == 'feedback')
		is_available=displayLink
		tooltip="Edit feedback"
		url=feedback_url />

		<#local students_url><#if displayLink><@routes.cm2.assignmentstudents assignment 'new' /></#if></#local>
		<@wizard_link
		label="Students"
		is_first=false
		is_active=(current_step == 'students')
		is_available=displayLink
		tooltip="Edit students"
		url=students_url />

		<#local markers_url><#if assignment.id??><@routes.cm2.assignmentmarkers assignment 'new' /></#if></#local>
		<@wizard_link
		label="Markers"
		is_first=false
		is_active=(current_step == 'markers')
		is_available=displayLink
		tooltip="Edit markers"
		url=markers_url />

		<#local submissions_url><#if displayLink><@routes.cm2.assignmentsubmissions assignment 'new' /></#if></#local>
		<@wizard_link
		label="Submissions"
		is_first=false
		is_active=(current_step == 'submissions')
		is_available=assignment.id??
		tooltip="Edit submissions"
		url=submissions_url />

		<#local options_url><#if displayLink><@routes.cm2.assignmentoptions assignment 'new' /></#if></#local>
		<@wizard_link
		label="Options"
		is_first=false
		is_active=(current_step == 'options')
		is_available=assignment.id??
		tooltip="Edit options"
		url=options_url />

		<#local review_url><#if displayLink><@routes.cm2.assignmentreview assignment /></#if></#local>
		<@wizard_link
		label="Review"
		is_first=false
		is_active=(current_step == 'review')
		is_available=displayLink
		tooltip="Review"
		url=review_url />
	</#if>
</p>
</#macro>


