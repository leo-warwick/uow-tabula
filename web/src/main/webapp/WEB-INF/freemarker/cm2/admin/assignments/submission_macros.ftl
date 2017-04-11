<#macro stage_delete stage>
	<#if stage.messageCode?default("")?length gt 0>
	<div class="stage<#if !stage.completed> incomplete<#if !stage.preconditionsMet> preconditions-not-met</#if></#if><#if stage.started && !stage.completed> current</#if>">
		<#if stage.completed>
			<#if stage.health.toString == 'Good'>
				<i class="icon-ok"></i>
			<#else>
				<i class="icon-remove"></i>
			</#if>
		<#else>
			<i class="icon-blank"></i>
		</#if>
		<#nested/>
	</div>
	</#if>
</#macro>


<#macro originalityReport attachment>
	<#local r=attachment.originalityReport />
	<#local assignment=attachment.submissionValue.submission.assignment />
	<#if r.similarity == 0>
		<#assign similarity='label label-success'/>
	<#elseif  r.similarity == 1>
		<#assign similarity='label label-info'/>
	<#elseif  r.similarity == 2>
		<#assign similarity='label label-primary'/>
	<#elseif  r.similarity == 3>
		<#assign similarity='label label-warning'/>
	<#elseif  r.similarity == 4>
		<#assign similarity='label label-danger'/>
	</#if>


<span id="tool-tip-${attachment.id}" class="${similarity} similarity-tooltip">${r.overlap}% similarity</span>
<div id="tip-content-${attachment.id}" class="hide">
	<p>${attachment.name} <img src="<@url resource="/static/images/icons/turnitin-16.png"/>"></p>
	<p class="similarity-subcategories-tooltip">
		Web: ${r.webOverlap}%<br>
		Student papers: ${r.studentOverlap}%<br>
		Publications: ${r.publicationOverlap}%
	</p>
	<p>
		<#if r.turnitinId?has_content>
			<a target="turnitin-viewer" href="<@routes.coursework.turnitinLtiReport assignment attachment />">View full report</a>
		</#if>
	</p>
</div>
<script type="text/javascript">
	jQuery(function($){
		$("#tool-tip-${attachment.id}").popover({
			placement: 'right',
			html: true,
			content: function(){return $('#tip-content-${attachment.id}').html();},
			title: 'Turnitin report summary'
		});
	});
</script>
</#macro>
