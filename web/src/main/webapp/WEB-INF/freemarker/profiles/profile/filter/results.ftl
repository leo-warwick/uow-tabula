<#escape x as x?html>

	<#function sortClass field>
		<#list filterStudentsCommand.sortOrder as order>
			<#if order.propertyName == field>
				<#if order.ascending>
					<#return "headerSortDown" />
				<#else>
					<#return "headerSortUp" />
				</#if>
			</#if>
		</#list>
		<#return "" />
	</#function>

	<#macro row student academicYear="">
		<tr class="student">
			<td>
				<@fmt.member_photo student "tinythumbnail" />
			</td>
			<td><h6>${student.firstName}</h6></td>
			<td><h6>${student.lastName}</h6></td>
			<td><a class="profile-link" href="<@routes.profiles.profile student />">${student.universityId}</a></td>
			<td>${student.groupName}</td>
			<#if academicYear?has_content>
				<#assign courseYearDetails = (student.mostSignificantCourseDetails.freshOrStaleStudentCourseYearDetailsForYear(academicYear))!"" />
				<#if !courseYearDetails?has_content>
					<#assign courseYearDetails=student.freshOrStaleStudentCourseYearDetailsForYear(academicYear) />
				</#if>
				<td>${(courseYearDetails.yearOfStudy)!""}</td>
				<td>${(courseYearDetails.route.name)!""}</td>
			<#else>
				<td>${(student.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!""}</td>
				<td>${(student.mostSignificantCourseDetails.currentRoute.name)!""}</td>
			</#if>
		</tr>
	</#macro>

	<#macro table students academicYear="">
		<table class="related_students table table-striped table-condensed">
			<thead>
				<tr>
					<th class="photo-col">Photo</th>
					<th class="student-col ${sortClass("firstName")}" data-field="firstName">First name</th>
					<th class="student-col ${sortClass("lastName")}" data-field="lastName">Last name</th>
					<th class="id-col ${sortClass("universityId")}" data-field="universityId">ID</th>
					<th class="type-col ${sortClass("groupName")}" data-field="groupName">Type</th>
					<th class="year-col ${sortClass("studentCourseYearDetails.yearOfStudy")}" data-field="studentCourseYearDetails.yearOfStudy">Year</th>
					<th class="course-but-photo-col ${sortClass("route.name")}" data-field="route.name">Course</th>
				</tr>
			</thead>

			<tbody>
				<#list students as item>
					<@row item academicYear/>
				</#list>
			</tbody>
		</table>

		<#if !student_table_script_included??>
			<script type="text/javascript">
				(function($) {
					$(function() {
						<#if totalResults lte filterStudentsCommand.studentsPerPage>
							$(".related_students").tablesorter({
								headers: { 0: { sorter: false } },
								sortList: [[2,0], [1,0]]
							});
						<#else>
							// CUSTOM TABLE SORTING
							$(".related_students").addClass('tablesorter')
								.find('th:not(:first-child)').addClass('header')
								.on('click', function(e) {
									var $th = $(this);

									if ($th.hasClass('headerSortDown')) {
										$('#sortOrder').val('desc(' + $th.data('field') + ')');
										$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
										$th.addClass('headerSortUp');
									} else {
										$('#sortOrder').val('asc(' + $th.data('field') + ')');
										$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
										$th.addClass('headerSortDown');
									}

									if (typeof(window.doRequest) === 'function') {
										window.doRequest($('#filterStudentsCommand'), true);
									} else {
										$('#filterStudentsCommand').submit();
									}
								});
						</#if>

						$(".student").on("mouseover", function(e) {
							$(this).find("td").addClass("hover");
						}).on("mouseout", function(e) {
							$(this).find("td").removeClass("hover");
						}).on("click", function(e) {
							if (! $(e.target).is("a") && ! $(e.target).is("img")) {
								window.location = $(this).find("a.profile-link")[0].href;
							}
						});

						$('.use-popover').tabulaPopover({
							trigger: 'click',
							container: 'body'
						});
					});
				})(jQuery);
			</script>
			<#assign student_table_script_included=true />
		</#if>
	</#macro>

	<#macro pagination currentPage totalResults resultsPerPage>
		<#local totalPages = (totalResults / resultsPerPage)?ceiling />
		<nav class="pull-right" style="margin-top: 0;">
			<#if can.do("Department.Reports", department) >
				<a href="<@routes.profiles.exportProfiles department academicYear filterStudentsCommand.serializeFilter />" class="btn btn-sm btn-default" style="vertical-align: top;">Export profiles</a>
			</#if>
			<ul class="pagination pagination-sm "style="margin-top: 0;">
				<#if currentPage lte 1>
					<li class="disabled"><span>&laquo;</span></li>
				<#else>
					<li><a href="?page=${currentPage - 1}" data-page="${currentPage - 1}">&laquo;</a></li>
				</#if>

				<#list 1..totalPages as page>
					<#if page == currentPage>
						<li class="active"><span>${page}</span></li>
					<#else>
					<li><a href="?page=${page}" data-page="${page}">${page}</a></li>
			    </#if>
		    </#list>

		    <#if currentPage gte totalPages>
					<li class="disabled"><span>&raquo;</span></li>
				<#else>
					<li><a href="?page=${currentPage + 1}" data-page="${currentPage + 1}">&raquo;</a></li>
				</#if>
			</ul>
		</nav>
	</#macro>

	<#if totalResults?? && students??>
		<#if totalResults gt 0>
			<div class="clearfix">
				<#if totalResults gt filterStudentsCommand.studentsPerPage>
					<div class="pull-right">
						<@pagination filterStudentsCommand.page totalResults filterStudentsCommand.studentsPerPage />
					</div>
				</#if>

				<p class="not-relative">
					<#assign startIndex = ((filterStudentsCommand.page - 1) * filterStudentsCommand.studentsPerPage) />
					<#assign endIndex = startIndex + students?size />

					Results ${startIndex + 1} - ${endIndex} of ${totalResults}
				</p>
			</div>

			<#if academicYear??>
				<@table students academicYear/>
			<#else>
				<@table students/>
			</#if>


			<div class="clearfix">
				<#if totalResults lte filterStudentsCommand.studentsPerPage>
					<div class="pull-left">
						<@fmt.bulk_email_students students=students />
					</div>
				<#else>
					<@pagination filterStudentsCommand.page totalResults filterStudentsCommand.studentsPerPage />
				</#if>
			</div>
		<#else>
			<p>No students were found.</p>
		</#if>
	</#if>

	<script type="text/javascript">
		jQuery(function($) {
			$('nav ul.pagination a').on('click', function(e) {
				e.preventDefault();
				e.stopPropagation();

				var page = $(this).data('page');
				$('#page').val(page);

				if (typeof(window.doRequest) === 'function') {
					window.doRequest($('#filterStudentsCommand'), true);
				} else {
					$('#filterStudentsCommand').submit();
				}
			});
		});
	</script>

</#escape>