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
        This report is no longer available. If you need access to the full report please contact webteam@warwick.ac.uk
      </#if>
    </p>
  </div>
  <script type="text/javascript" nonce="${nonce()}">
    jQuery(function ($) {
      $("#tool-tip-${attachment.id}").popover({
        placement: 'right',
        html: true,
        content: function () {
          return $('#tip-content-${attachment.id}').html();
        },
        title: 'Turnitin report summary'
      });
    });
  </script>
</#macro>