<#ftl strip_text=true />
<#-- FIXME: implemented as part of CM2 migration but will require further reworking due to CM2 workflow changes -->

<#-- Common template parts for use in other submission/coursework templates. -->
<#macro originalityReport attachment>
	<#local r=attachment.originalityReport />
	<#local assignment=attachment.submissionValue.submission.assignment />

	<span id="tool-tip-${attachment.id}" class="similarity-${r.similarity} similarity-tooltip">${r.overlap}% similarity</span>
	<div id="tip-content-${attachment.id}" class="hide">
		<p>${attachment.name} <img src="<@url resource="/static/images/icons/turnitin-16.png"/>"></p>
		<p class="similarity-subcategories-tooltip">
			Web: ${r.webOverlap}%<br>
			Student papers: ${r.studentOverlap}%<br>
			Publications: ${r.publicationOverlap}%
		</p>
		<p>
			<#if r.turnitinId?has_content>
				<a target="turnitin-viewer" href="<@routes.cm2.turnitinLtiReport assignment attachment />">View full report</a>
			<#else>
				<a target="turnitin-viewer" href="<@routes.cm2.turnitinReport assignment attachment />">View full report - available via Tabula until end of August 2016</a>
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

<#macro urkundOriginalityReport attachment>
	<#local r=attachment.originalityReport />
	<#local assignment=attachment.submissionValue.submission.assignment />

<span id="tool-tip-urkund-${attachment.id}" class="similarity-${r.significance} similarity-tooltip">${r.significance}% significance</span>
<div id="tip-content-urkund-${attachment.id}" class="hide">
	<p>${attachment.name}</p>
	<p class="similarity-subcategories-tooltip">
		Match count: ${r.matchCount}<br>
		Source count: ${r.sourceCount}<br>
	</p>
	<p>
		<a target="_blank" href="${r.reportUrl}">View full report</a>
	</p>
</div>
<script type="text/javascript">
	jQuery(function($){
		$("#tool-tip-urkund-${attachment.id}").popover({
			placement: 'right',
			html: true,
			content: function(){return $('#tip-content-urkund-${attachment.id}').html();},
			title: 'Urkund report summary'
		});
	});
</script>
</#macro>