<html>
	
	<head>
		<title>Quoddy: People I Follow Search Results</title>
		<meta name="layout" content="main" />
	    <nav:resources />		
	</head>
	
	<body>
		<p />
		Matching People I Follow:
		<p />
		<ul>
			<g:each in="${allUsers}" var="user">
			
				<!-- display discrete entries here -->
				<li>
					<g:link controller="user" action="viewUser" params="[userId:user.userId]">${user.fullName}</g:link>
				</li>
			</g:each>
		</ul>
	</body>
	
</html>