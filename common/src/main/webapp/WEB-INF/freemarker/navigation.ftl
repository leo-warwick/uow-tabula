<#if isCollapsed>
	<ul class="nav navbar-nav">
		<#if user.staff>
			<li class="profiles-active"><a href="<@url page="/" context="/profiles" />">Profiles</a></li>
		<#elseif user.student>
			<li class="profiles-active"><a href="<@url page="/" context="/profiles" />">My Student Profile</a></li>
		<#elseif canViewProfiles>
			<li class="profiles-active"><a href="<@url page="/" context="/profiles" />">Profiles</a></li>
		</#if>

		<#if features.smallGroupTeaching>
			<li class="groups-active"><a href="<@url page="/" context="/groups" />">Small Group Teaching</a></li>
		</#if>

		<li class="courses-active"><a href="<@url page="/" context="/coursework" />">Coursework Management</a></li>

		<#if examsEnabled || examGridsEnabled>
			<li class="exams-active"><a href="<@url page="/" context="/exams" />">Exam Management</a></li>
		</#if>

		<#if features.attendanceMonitoring>
			<#if user.staff>
				<li class="attendance-active"><a href="<@url page="/" context="/attendance" />">Monitoring Points</a></li>
			<#elseif user.student>
				<li class="attendance-active"><a href="<@url page="/profile" context="/attendance" />">My Monitoring Points</a></li>
			</#if>
		</#if>

		<#if features.reports && canDeptAdmin>
			<li class="reports-active"><a href="<@url page="/" context="/reports" />">Reports</a></li>
		</#if>

		<#if canAdmin>
			<li class="admin-active"><a href="<@url page="/" context="/admin" />">Administration and Permissions</a></li>
		</#if>

	</ul>
<#else>
	<ul id="home-list">
		<#if user.staff>
			<li><h2><a href="<@url page="/" context="/profiles" />">Profiles</a></h2>
				<span class="hint">View staff and student information, your personal timetable, and your department's timetable</span>
			</li>
		<#elseif user.student>
			<li><h2><a href="<@url page="/" context="/profiles" />">My Student Profile</a></h2>
				<span class="hint">View your student information and timetable</span>
			</li>
		<#elseif canViewProfiles>
			<li><h2><a href="<@url page="/" context="/profiles" />">Profiles</a></h2>
				<span class="hint">View staff and student information</span>
			</li>
		</#if>

		<#if features.smallGroupTeaching>
			<li><h2><a href="<@url page="/" context="/groups" />">Small Group Teaching</a></h2>
				<#if user.staff>
					<span class="hint">Create seminars, tutorials and lab groups</span>
				<#else>
					<span class="hint">View your seminars, tutorials and lab groups</span>
				</#if>
			</li>
		</#if>

		<li><h2><a href="<@url page="/" context="/coursework" />">Coursework Management</a></h2>
			<#if user.staff>
				<span class="hint">Create assignments, give feedback and add marks</span>
			<#else>
				<span class="hint">Submit coursework, view feedback and see your marks</span>
			</#if>
		</li>

		<#if examsEnabled || examGridsEnabled>
			<li>
				<h2><a href="<@url page="/" context="/exams" />">Exam Management</a></h2>
				<#if examsEnabled && examGridsEnabled>
					<span class="hint">Manage exam marks and exam board grids</span>
				<#elseif examsEnabled>
					<span class="hint">Manage exam marks</span>
				<#else>
					<span class="hint">Manage exam board grids</span>
				</#if>
			</li>
		</#if>

		<#if features.attendanceMonitoring>
			<#if user.staff>
				<li>
					<h2><a href="<@url page="/" context="/attendance" />">Monitoring Points</a></h2>
					<span class="hint">View and record attendance at specified monitoring points</span>
				</li>
			<#elseif user.student>
				<li>
					<h2><a href="<@url page="/profile" context="/attendance" />">My Monitoring Points</a></h2>
					<span class="hint">View your attendance at specified monitoring points</span>
				</li>
			</#if>
		</#if>

		<#if features.reports && canDeptAdmin>
			<li>
				<h2><a href="<@url page="/" context="/reports" />">Reports</a></h2>
				<span class="hint">View reports for various aspects of Tabula</span>
			</li>
		</#if>

		<#if canAdmin>
			<li>
				<h2><a href="<@url page="/" context="/admin" />">Administration and Permissions</a></h2>
				<span class="hint">Manage department, module and route settings and permissions</span>
			</li>
		</#if>

	</ul>
</#if>