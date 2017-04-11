/* Some functions to include in a QUnit test page to make setting things up easier. */

/* Instead of a script tag pointing at the file you're testing (which might be in a hard
   to reference relative location), use includeScript with a path to append to the end of
   /modules/ in the project. */
function includeScript(path) {
	var top = "/modules/";
	var href = window.location.href;
	if (href.indexOf('http') === 0) {
		// If HTTP, assume we're in a JUnit test, serving everything from under /modules/
		var url = "/" + path;
	} else {
		var base = href.substring(0, href.indexOf(top) + top.length);
		var url = base + path;
	}
	document.write('<script src="'+url+'"></script>');
}

// Even more convenient form of includeScript, for scripts in the static home JS folder (most of them).
function includeHomeScript(path) {
	includeScript('home/src/main/webapp/static/js/' + path);
}