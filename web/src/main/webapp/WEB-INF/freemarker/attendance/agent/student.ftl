<#escape x as x?html>
	<#import "../attendance_variables.ftl" as attendance_variables />
	<#import "../attendance_macros.ftl" as attendance_macros />

<h1>${student.fullName}</h1>

<section class="identity">
	<div class="row">
		<div class="col-md-6">
			<div class="row">
				<div class="col-md-5 col-lg-4">
					<@fmt.member_photo student />
				</div>
				<div class="col-md-7 col-lg-8">
					<strong>Official name:</strong> ${student.officialName}<br/>
					<strong>Preferred name:</strong> ${student.fullName}<br/>
					<details class="indent">
						<summary>
							<strong>More info</strong>
						</summary>
						<#if student.dateOfBirth??>
							<strong>Date of birth:</strong> ${student.dateOfBirth?date("yyyy-MM-dd")?string("dd/MM/yyyy")}<br/>
						</#if>
						<#if student.nationality??>
							<strong>Nationality:</strong> <@fmt.nationality student.nationality!('Unknown') /><#if student.secondNationality??> and <@fmt.nationality student.secondNationality /></#if><br/>
						</#if>
						<#if features.disabilityRenderingInProfiles && (student.disability.reportable)!false>
							<strong>Disability:</strong>
							<a href="#" class="use-popover cue-popover" id="popover-disability" data-html="true"
							   data-content="<p>This student has self-reported the following disability code:</p><div class='well'><h6>${student.disability.code}</h6><small>${(student.disability.sitsDefinition)!}</small></div>"> ${student.disability.definition}</a><br/>
						</#if>
						<#if features.visaInStudentProfile && student.hasTier4Visa?? && student.casUsed??>
							<strong>Tier 4 requirements:</strong>
							<#if student.casUsed && student.hasTier4Visa>Yes
							<#elseif !student.casUsed && !student.hasTier4Visa>No
							<#else>
								<#if !student.casUsed && student.hasTier4Visa>
									<#assign inconsistency = "Tier 4 visa exists but no Confirmation of Acceptance for Studies" />
								<#else>
									<#assign inconsistency = "Confirmation of Acceptance for Studies exists but no tier 4 visa" />
								</#if>
								Contact the <a href="mailto:immigrationservice@warwick.ac.uk">Immigration Service</a>
								<a class="use-popover" data-content="Contact the University's Immigration Service to find out whether tier 4
								requirements apply to this student. (${inconsistency})" data-toggle="popover"><i class="fa fa-question-circle"></i></a>
							</#if>
							<br/>
						</#if>
					</details>
					<#if student.email??>
						<strong>Warwick email:</strong> <a href="mailto:${student.email}">${student.email}</a><br/>
					</#if>
					<#if student.homeEmail??>
						<strong>Alternative email:</strong> <a href="mailto:${student.homeEmail}">${student.homeEmail}</a><br/>
					</#if>
					<#if student.mobileNumber??>
						<strong>Mobile phone:</strong> ${phoneNumberFormatter(student.mobileNumber)}<br/>
					</#if>
					<#if student.universityId??>
						<strong>University ID: </strong> ${student.universityId}<br/>
					</#if>
					<#if student.userId??>
						<strong>Username:</strong> ${student.userId}<br/>
					</#if>
					<#if student.homeDepartment??>
						<strong>Home department:</strong> ${student.homeDepartment.name}<br/>
					</#if>
				</div>
			</div>
		</div>
	</div>
</section>

<#if groupedPointMap?keys?size == 0>
	<p><em>No monitoring points found for this academic year.</em></p>
<#else>
	<#assign returnTo><@routes.attendance.agentStudent relationshipType academicYear student /></#assign>
	<a class="btn btn-primary" href="<@routes.attendance.agentRecord relationshipType academicYear student returnTo />">Record attendance</a>
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if groupedPointMap[term]??>
			<@attendance_macros.groupedPointsBySection groupedPointMap term; groupedPointPair>
				<#assign point = groupedPointPair._1() />
				<div class="col-md-10">
					${point.name}
					(<span class="use-tooltip" data-html="true" title="
						<@fmt.wholeWeekDateFormat
							point.startWeek
							point.endWeek
							point.scheme.academicYear
						/>
					"><@fmt.monitoringPointWeeksFormat
						point.startWeek
						point.endWeek
						point.scheme.academicYear
						department
					/></span>)
				</div>
				<div class="col-md-2">
					<#if groupedPointPair._2()??>
						<@attendance_macros.checkpointLabel department=department checkpoint=groupedPointPair._2() />
					<#else>
						<@attendance_macros.checkpointLabel department=department point=groupedPointPair._1() student=student />
					</#if>
				</div>
			</@attendance_macros.groupedPointsBySection>
		</#if>
	</#list>

	<#list monthNames as month>
		<#if groupedPointMap[month]??>
			<@attendance_macros.groupedPointsBySection groupedPointMap month; groupedPointPair>
				<#assign point = groupedPointPair._1() />
				<div class="col-md-10">
					${point.name}
					(<@fmt.interval point.startDate point.endDate />)
				</div>
				<div class="col-md-2">
					<#if groupedPointPair._2()??>
						<@attendance_macros.checkpointLabel department=department checkpoint=groupedPointPair._2() />
					<#else>
						<@attendance_macros.checkpointLabel department=department point=groupedPointPair._1() student=student />
					</#if>
				</div>
			</@attendance_macros.groupedPointsBySection>
		</#if>
	</#list>
</#if>

</#escape>