<#macro originalityReport attachment>
	<#local r=attachment.originalityReport />
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
				<a target="turnitin-viewer" href="<@routes.coursework.turnitinLtiReport assignment attachment />">View full report</a>
			<#else>
				<a target="turnitin-viewer" href="<@routes.coursework.turnitinReport assignment attachment />">View full report - available via Tabula until end of August 2016</a>
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