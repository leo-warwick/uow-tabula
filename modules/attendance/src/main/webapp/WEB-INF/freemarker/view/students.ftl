<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#macro deptheaderroutemacro dept>
	<@routes.viewStudents dept academicYear.startYear?c />
</#macro>
<#assign deptheaderroute = deptheaderroutemacro in routes/>
<@fmt.deptheader "View students" "in" department routes "deptheaderroute" />

<#assign filterQuery = filterCommand.serializeFilter />

<#assign submitUrl><@routes.viewStudents department academicYear.startYear?c /></#assign>

<#assign filterCommand = filterCommand />
<#assign filterCommandName = "filterCommand" />
<#assign filterResultsPath = "/WEB-INF/freemarker/view/_students_results.ftl" />
<#include "/WEB-INF/freemarker/filter_bar.ftl" />

<script type="text/javascript">
	jQuery(function($) {
		$('#command input').on('change', function(e) {
			$('.send-to-sits a').addClass('disabled');
		});

		$(document).on("tabula.filterResultsChanged", function() {
			var sitsUrl = $('div.studentResults').data('sits-url');
			$('.send-to-sits a').attr('href', sitsUrl);
			$('.send-to-sits a').removeClass('disabled');

			$('.scrollable-points-table').find('table').each(function() {
				var $this = $(this);
				if (Math.floor($this.width()) > $this.parent().width()) {
					$this.wrap($('<div><div class="sb-wide-table-wrapper"></div></div>'));
					Attendance.scrollablePointsTableSetup();
				}
			});
		});
	});
</script>
</#escape>