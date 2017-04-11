<#escape x as x?html>
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/modal_macros.ftl" as modal />

<#assign manualFormAction><@routes.profiles.relationship_reallocate department relationshipType agentId /></#assign>
<#assign previewFormAction><@routes.profiles.relationship_allocate_preview department relationshipType /></#assign>

<h1>Reallocate students from ${command.agentEntityData.displayName}</h1>

<div class="tabbable">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#allocatestudents-tab1" data-toggle="tab">Manually allocate students</a>
		</li>
	</ul>
</div>

<div class="tab-content">
	<#include "_allocate_manual_tab.ftl" />
</div>

<div id="profile-modal" class="modal fade profile-subset"></div>

</#escape>
