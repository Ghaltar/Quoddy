<div class="aseWrapper">
	
	<div class="aseAvatarBlock">
		<img height="48" width="48" src="${createLinkTo(dir:'images', file:'rss-icon.jpg')}"/>
	</div>
	<div class="aseTitleBar">
	
		<span class="aseTitleBarUserLink" >
		${item.streamObject.owningSubscription.name}
		</span> 
		
		<span class="aseTitleBarPermalink">
		<g:link controller="permalink" action="index" params="${[uuid:item.uuid]}" title="${formatDate(date:item.dateCreated)}">
				<g:formatDate date="${item.dateCreated}" type="datetime" style="SHORT" timeStyle="SHORT" />
		</g:link>
		</span>
		
        <div class="commentButtonBar">
      		<span class="plusOneButton" id="plusOneButton.${item.uuid}" name="plusOneButton.${item.uuid}" >
        		<a href="#" class="btn">+1</a>
      		</span>
      		<span class="shareButton" id="shareButton.${item.uuid}" name="shareButton.${item.uuid}" >
        		<a href="#" class="btn">Share</a>
      		</span>
      		<span class="showHideCommentsButton">
        		<a href="#" class="btn">Hide Comments</a>
      		</span>
    	</div>		
		
	</div>	
	
	
	<div class="activityStreamEntry rssFeedItem"> 
		<p>
		    ${item.streamObject.name}
		</p>
		<p>
			<a href="${item.streamObject.linkUrl}">${item.streamObject.linkUrl}</a>
			<hr />
			
			<em>${item.streamObject.leadingSnippet}</em>
		</p>
	</div>
	<!-- begin aseClear -->
	<div class="aseClear"></div>
	<!--  end aseClear -->
	
	<!-- begin aseFooter -->
	<div class="aseFooter">
	
		<!-- begin commentboxWrapper -->
		<div class="commentBoxWrapper">
		
			<!-- begin commentsArea -->
			<div id="commentsArea" class="commentsArea">
				<!--  render comments on the Event here -->
				<g:render template="/renderComments" var="comments"
					bean="${item.streamObject.comments}" />
			</div>
			<!-- end commentsArea -->
			
			<form name="addCommentForm" id="addCommentForm" class="addCommentForm">
        		
        		<!-- <label>Add a comment</label> -->
				<input name="addCommentTextInput" id="addCommentTextInput"
					class="addCommentTextInput" type="textbox" value="Add a Comment"></input>
				<br />
				<input name="eventId" type="hidden" value="${item.id}" /> <input
					name="submitCommentBtn" id="submitCommentBtn"
					class="btn submitCommentBtn" style="display: none;" type="submit"
					value="Submit" /> <input name="cancelCommentBtn"
					id="cancelCommentBtn" class="btn cancelCommentBtn"
					style="display: none;" type="submit" value="Cancel" />
			</form>
			<!--  end addCommentForm -->
			
		</div>
		<!--  end commentBoxWrapper -->
	</div>
	
	<!--  end aseFooter -->
</div>