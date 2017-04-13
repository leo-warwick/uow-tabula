Your browser doesn't seem able to handle uploading multiple files<noscript>
(or it does, but your browser is not running the Javascript needed to support it)</noscript>.
A recent browser like Google Chrome or Firefox will be able to upload multiple files.
You can still upload a single file here if you want.
<div id="multifile-column-description-enabled" class="help-block" style="display:none">
	This uploader allows you to upload multiple files at once. They
	will need to be in the same folder on your computer for you to be
	able to select them all. To select multiple files at once hold
	down the Ctrl key and click on each file that you would like to
	upload.
</div>

<script>
	if (Supports.multipleFiles) {
		jQuery('#multifile-column').find('h3').html('Select files');
		jQuery('#multifile-column-description').html(jQuery('#multifile-column-description-enabled').html());
	}
</script>
