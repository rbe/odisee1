<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
	<head>
		<meta name="layout" content="irb" />
	</head>
	<body>
		<p>
			<g:remoteLink controller="oooDocument" action="add" update="[success: 'pagecontent', failure: 'pagecontent']">Add document</g:remoteLink>
			<g:remoteLink controller="oooDocument" action="list" update="[success: 'pagecontent', failure: 'pagecontent']">Documents</g:remoteLink>
			<g:remoteLink controller="glueMimeType" action="list" update="[success: 'pagecontent', failure: 'pagecontent']">MIME Types</g:remoteLink>
		</p>
		<div id="pagecontent"></div>
	</body>
</html>
