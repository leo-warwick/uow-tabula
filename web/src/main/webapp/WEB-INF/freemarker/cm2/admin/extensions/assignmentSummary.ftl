<#import "*/cm2_macros.ftl" as cm2 />
<#assign module = assignment.module />
<#assign department = module.adminDepartment />
<#assign time_remaining=durationFormatter(assignment.closeDate) />
<#--noinspection FtlWellformednessInspection-->

<#macro row graph>
	<#assign state = (graph.extension.state.description)!"None" />
<tr class="itemContainer"
	data-contentid ="${assignment.id}__${graph.user.userId}"
	data-detailurl ="<@routes.cm2.extensiondetail assignment graph.user.userId />"
>

<#-- TAB-2063 - The extension manager will need to know who is doing the asking, so we should always show names -->
	<td class="student-col toggle-cell toggle-icon">${graph.user.firstName}</td>
	<td class="student-col toggle-cell">${graph.user.lastName}&nbsp;<#if graph.user.warwickId??><@pl.profile_link graph.user.warwickId /><#else><@pl.profile_link graph.user.userId /></#if></td>

	<td class="status-col toggle-cell content-cell">
		<dl style="margin: 0; border-bottom: 0;">
			<dt>
				<#if graph.awaitingReview>
					<span>Awaiting review</span>
				<#elseif graph.hasApprovedExtension>
					<span>Approved</span>
				<#elseif graph.hasRejectedExtension>
					<span>Rejected</span>
				<#else>
					<span>No extension</span>
				</#if>
			</dt>
			<dd style="display: none;" class="table-content-container" data-contentid="${assignment.id}__${graph.user.userId}">
				<div id="content-${assignment.id}__${graph.user.userId}" class="content-container" data-contentid="${assignment.id}__${graph.user.userId}">
					<p>No extension data is currently available.</p>
				</div>
			</dd>
		</dl>
	</td>
	<td class="duration-col toggle-cell<#if graph.hasApprovedExtension> approved<#else> very-subtle</#if>">
		<#if (graph.duration > 0)>
			${graph.duration} days
		<#elseif (graph.requestedExtraDuration > 0) >
			${graph.requestedExtraDuration} days requested
		<#else>
			N/A
		</#if>
	</td>
	<td data-datesort="${graph.deadline.millis?c!''}" class="deadline-col <#if graph.hasApprovedExtension>approved<#else>very-subtle</#if>"><#if graph.deadline?has_content><@fmt.date date=graph.deadline /></#if></td>
</tr>
</#macro>

<#escape x as x?html>
	<@cm2.assignmentHeader "Manage extensions" assignment "for" />

<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<div id="profile-modal" class="modal fade profile-subset"></div>

<div class="row extension-metadata">
	<div class="col-md-7">
		<#if assignment.closed>
			<p class="late deadline">
				<span class="time-remaining">Closed ${time_remaining}.</span>
				Deadline was <@fmt.date date=assignment.closeDate />.
			</p>
		<#else>
			<p class="deadline">
				<span class="time-remaining">Closes in ${time_remaining}.</span>
				Deadline <@fmt.date date=assignment.closeDate />.
			</p>
		</#if>
	</div>
	<div class="col-md-5">
		<p class="alert alert-info">
			Students will automatically be notified by email when you approve, modify or revoke an extension.
		</p>
	</div>
</div>

	<#if extensionGraphs?size gt 0>
	<table id="student-extension-management" class="students table table-striped sticky-table-headers expanding-table"
		   data-max-days="${maxDaysToDisplayAsProgressBar}"
		   data-row-to-open="${extensionToOpen!""}">
		<thead>
		<tr>
		<#-- TAB-2063 no respect for dept settings, we always want to see a name here -->
			<th class="student-col">First name</th>
			<th class="student-col">Last name</th>

			<th class="status-col">Status</th>
			<th class="duration-col">Length of extension</th>
			<th class="deadline-col">Submission Deadline</th>
		</tr>
		</thead>

		<tbody>
			<#list extensionGraphs as extensionGraph>
					<#if (extensionGraph.extension?has_content && can.do("Extension.Update", assignment)) || can.do("Extension.Create", assignment)>
			<#-- as this is a *management* screen, only show rows we can actually do something with -->
				<@row extensionGraph />
			</#if>
				</#list>
		</tbody>
	</table>

	<script type="text/javascript">
		(function($) {
			// add a custom parser for the date column
			$.tablesorter.addParser({
				id: 'customdate',
				is: function(s, table, cell, $cell){return false; /*return false so this parser is not auto detected*/},
				format: function(s, table, cell, cellIndex) {
					var $cell = $(cell);
					return $cell.prop('data-datesort') || s;
				},
				parsed: false,
				type: 'numeric'
			});


			$('.expanding-table').expandingTable({
				contentUrlFunction: function($row) { return $row.data('detailurl'); },
				useIframe: true,
				tableSorterOptions: {
					sortList: [[1, 0], [0, 0]],
					headers: {
						3: { sorter: false },
						4: { sorter: 'customdate' }
					}
				},
				preventContentIdInUrl: true
			});
		})(jQuery);
	</script>
	<#else>
	<p class="alert alert-info">There are no students registered for this assignment.</p>
	</#if>
</#escape>