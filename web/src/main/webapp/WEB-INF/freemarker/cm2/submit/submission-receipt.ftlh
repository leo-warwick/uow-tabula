<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<#import "*/coursework_components.ftl" as components />
<#assign assignment=submission.assignment />
<#assign module=assignment.module />
<#if assignment.approvedExtensions[submission.usercode]??>
  <#assign extension=assignment.approvedExtensions[submission.usercode] />
</#if>

<head>
  <style type="text/css">
    body {
      font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
    }

    h1 {
      font-size: 20px;
      margin: 0 0 10px 0;
    }

    h2 {
      font-size: 18px;
      margin: 0 0 8px 0;
    }

    #header {
      border-bottom: 1px solid #888;
    }

    .mod-code {
      letter-spacing: 1px;
      font-weight: bold;
      font-size: 130%;
    }

    .mod-name {
      font-size: 85%;
    }

    .mod-name:before {
      content: '// ';
      position: relative;
      bottom: 1px;
    }

    .ass-name {
      display: block;
      margin-top: -3px;
    }
  </style>
  <title>${module.name} (${module.code?upper_case} ${module.name}) - ${assignment.name}</title>
</head>
<body>
  <div id="header">
    <img src="/static/images/id7/logo-divider-gray.png" style="width: 100%;"/>
    <h1><@fmt.module_name module /></h1>
    <h2 class="ass-name">${assignment.name}</h2>
  </div>

  <div class="assignment-details">
    <#if !assignment.openEnded>
      <p>Assignment deadline <@fmt.date date=assignment.closeDate at=true seconds=false relative=false showLocal=true showLocalStyle="extended" /></p>
    </#if>
  </div>

  <h2>Submission receipt for <#if submission.universityId??>${submission.universityId}<#else>${submission.usercode}</#if></h2>

  <div class="submission-receipt">
    <p>Submission received <@fmt.date date=submission.submittedDate at=true seconds=true relative=false showLocal=true showLocalStyle="extended" /> (<#compress>
      <#if submission.late>
        <#if (extension.approved)!false>
          <@components.extensionLateness extension submission />
        <#else>
          <@components.lateness submission />
        </#if>
      <#elseif submission.authorisedLate && extension??>
        within extension until <@fmt.date date=extension.expiryDate capitalise=false shortMonth=true relative=false showLocal=true showLocalStyle="extended" />
      <#else>
        on time
      </#if>
      </#compress>).</p>
    <p>Submission ID: ${submission.id}</p>

    <#if submission.allAttachments??>
      <p>
        Uploaded files:
      <ul>
        <#list submission.allAttachments as attachment>
          <li>${attachment.name}</li>
        </#list>
      </ul>
      </p>
    </#if>

    <#if assignment.wordCountField?? && submission.valuesByFieldName[assignment.defaultWordCountName]??>
      <p>Declared word count: ${submission.valuesByFieldName[assignment.defaultWordCountName]?number} words</p>
    </#if>

    <#if assignment.displayPlagiarismNotice>
      <p>
        In submitting my work I confirm that:
      </p>

      <ol>
        <li>I have read the guidance on plagiarism/cheating provided in the handbook and understand the University regulations in relation to
          plagiarism/cheating. I am aware of the potential consequences of committing plagiarism/cheating. I declare that the work is all my own, except where I
          have stated otherwise.
        </li>
        <li>No substantial part(s) of the work submitted here has also been submitted by me in other assessments for accredited courses of study (other than in
          the case of a resubmission of a piece of work), and I acknowledge that if this has been done an appropriate reduction in the mark I might otherwise
          have received will be made.
        </li>
        <li>I understand that should this piece of work raise concerns requiring investigation in relation to points 1) and/or 2) above, it is possible that
          other work I have submitted for assessment will be checked, even if the marking process has been completed.
        </li>
        <li>Where a proof reader, paid or unpaid was used, I confirm that the proof reader was made aware of and has complied with the <a target='_blank'
                                                                                                                                          href='https://warwick.ac.uk/proofreadingpolicy'>University’s
            proof reading policy</a>.
        </li>
      </ol>
    </#if>
  </div>
</body>
</html>
