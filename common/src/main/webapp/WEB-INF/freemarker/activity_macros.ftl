<#macro activity_stream max=20 minPriority=0 types=''>
<div class="activity-stream" data-max="${max}" data-minPriority="${minPriority}" <#if types?has_content>data-types="${types}"</#if>>
<#-- activity-stream.js will populate .activity-stream elements on domready -->
<div class="hint">Loading&hellip;</div>
</div>
</#macro>