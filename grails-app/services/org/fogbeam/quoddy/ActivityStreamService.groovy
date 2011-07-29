package org.fogbeam.quoddy;

import java.util.Calendar

class ActivityStreamService {

	def userService;
	def jmsService;
	def eventQueueService;
	
	/*
	public List<Activity> getRecentFriendActivitiesForUser( User user )
	{
		List<Activity> recentActivities = new ArrayList<Activity>();
		
		List<User> friends = userService.listFriends( user );
		println "Found ${friends.size()} friends";
		List<Integer> friendIds = new ArrayList<Integer>();
		for( User friend: friends )
		{
			def id = friend.id;
			println( "Adding friend id: ${id}, userId: ${friend.userId} to list" );
			friendIds.add( id );	
		}
		
		// Entry.executeQuery( "select entry from Entry as entry, User as user where user.userId = ? and entry not in elements(user.hiddenEntries) order by entry.dateCreated desc", [user.userId] )
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, -82 );
		Date cutoffDate = cal.getTime();
		
		println "Using ${cutoffDate} as cutoffDate";
		
		List<Activity> queryResults = Activity.executeQuery( "select activity from Activity as activity where activity.dateCreated >= :cutoffDate and activity.creator.id in (:friendIds)",  ['cutoffDate':cutoffDate, 'friendIds':friendIds]);
		recentActivities.addAll( queryResults );
		return recentActivities;
	}
	*/

	public void saveActivity( Activity activity )
	{
		println "about to save activity...";
		if( !activity.save() )
		{
			println( "Saving activity FAILED");
			activity.errors.allErrors.each { println it };
		}
	}

	
	/* Note: we're cheating here and just dealing with one queue, one user, etc., just to prove
	 * the interaction from the UI layer down to here.  The real stuff will obviously pull in 
	 * activities based on friends, and whatever other "stuff" the user has registered interest in.
	*/
	
	public List<Activity> getRecentActivitiesForUser( final User user, final int maxCount )
	{
		println "getRecentActivitiesForUser: ${user.userId} - ${maxCount}";
		/*
		 
		 so what do we do here?  Ok... we receive a request for up to maxCount recent activities.
		 Since, by definition, the stuff in the queue is most recent, we read up to maxCount entries
		 from the queue. If the queue has more than maxCount activities we ??? (what? Blow away the
		 extras? Leave 'em hanging around for later? Force a flush to the db? ???)
		 
		 If the queue had less than maxCount records (down to as few as NONE), we retrieve
		 up to (maxCount - readfromQueueCount) matching records from the db. 
		
		 The resulting list is the union of the set of activities retrieved from the queue and
		 the activities loaded from the DB.
		
		 Note: Since we really want to show "newest at top" or "newest first" we really wish this
		 "queue" were actually a stack, so we'd be starting with the newest messages and
		 getting progressively older ones.  We need to explore the possibility of having our
		 underlying messaging system offer stack semantics, OR implement an intermediate 
		 queue, that reads the messages from the underlying messaging fabric, and offers them
		 to us in the right order.  Possibly explore using Camel for this, or roll our own
		 thing?
		
		 we could also just read everything that's currently on the queue, sort by timestamp,
		 use up to maxCount of the messages, and then throw away anything that's left-over.
		 but if we do too much of this, we wind up throwing away a lot of queued messages, which
		 negates the benefit of not having to read from the DB.
		
		 ok, just to get something prototyped... let's pretend that the queue we're reading from
		 right here *is* the "intermediate queue" and everything is just magically in the right order.
		 "no problem in computer science that you can't solve by adding a layer of abstraction" right?
		
		  Also, for now let's pretend that the queue we're reading from has already been filtered so that
		  it only contains messages that we are interested in; including expiring messages for age, etc.
		
		*/
		
		int msgsOnQueue = eventQueueService.getQueueSizeForUser( user.userId );
		println "Messages available on queue: ${msgsOnQueue}";
		int msgsToRead = 0;
		if( msgsOnQueue > 0 )
		{
			if( msgsOnQueue <= maxCount )
			{
				msgsToRead = msgsOnQueue;
			}
			else 
			{
				msgsToRead = maxCount - msgsOnQueue;	
			}
		}
		
		println "Messages to read from queue: ${msgsToRead}";
		
		long oldestOriginTime = Long.MAX_VALUE;
		
		// NOTE: we could avoid iterating over this list again by returning the "oldest message time"
		// as part of this call.  But it'll mean wrapping this stuff up into an object of some
		// sort, or returning a Map of Maps instead of a List of Maps
		List<Map> messages = eventQueueService.getMessagesForUser( user.userId, msgsToRead );
		for( Map msg : messages )
		{
			println "msg.originTime: ${msg.originTime}";
			if( msg.originTime < oldestOriginTime )
			{
				oldestOriginTime = msg.originTime;
			}
		}
		
		println "oldestOriginTime: ${oldestOriginTime}";
		
		// convert our messages to Activity instances and
		// put them in this list...
		List<Activity> recentActivities = new ArrayList<Activity>();
		
		// NOTE: we wouldn't really want to iterate over this list here... better
		// to build up this list above, and never bother storing the JMS Message instances
		// at all...  but for now, just to get something so we can prototype the
		// behavior up through the UI...
		for( int i = 0; i < messages.size(); i++ )
		{
			Map msg = messages.get(i);
			println "got message: ${msg} off of queue";
			Activity activity = new Activity();
			
			// println "msg class: " + msg?.getClass().getName();
			activity.creator = userService.findUserByUserId( msg.creator ); 
			activity.text = msg.text;
			activity.originTime = msg.originTime;
			recentActivities.add( activity );	
		}
		
		println "recentActivities.size() = ${recentActivities.size()}"
		
		/* NOTE: here, we need to make sure we don't retrieve anything NEWER than the OLDEST
		 * message we may have in hand - that we received from the queue.  Otherwise, we risk
		 * showing the same event twice.
		 */
		
		// now, do we need to go to the DB to get some more activities?
		if( maxCount > msgsToRead ) 
		{
			int recordsToRetrieve = maxCount - msgsToRead;
			println "retrieving up to ${recordsToRetrieve} records from the database";
			
			// NOTE: get up to recordsToRetrieve records, but don't retrieve anything that
			// would already be in our working set.
			// also... we need to make a distinction between the "get recent" method which has
			// this cutoff logic and the generic "get older" method that can be used to incrementally
			// step backwards into history as far as (they want to go | as far as we let them go)
			
			
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR_OF_DAY, -600 );
			Date cutoffDate = cal.getTime();
			
			println "Using ${cutoffDate} as cutoffDate";
						
			List<User> friends = userService.listFriends( user );
			if( friends != null && friends.size() > 0 ) 
			{
				println "Found ${friends.size()} friends";
				List<Integer> friendIds = new ArrayList<Integer>();
				for( User friend: friends )
				{
					def id = friend.id;
					println( "Adding friend id: ${id}, userId: ${friend.userId} to list" );
					friendIds.add( id );
				}
			
			
				List<Activity> queryResults = 
					Activity.executeQuery( "select activity from Activity as activity where activity.dateCreated >= :cutoffDate and activity.creator.id in (:friendIds) and activity.originTime < :oldestOriginTime order by activity.dateCreated desc",
						['cutoffDate':cutoffDate, 'oldestOriginTime':oldestOriginTime, 'friendIds':friendIds], ['max': recordsToRetrieve ]);
			
					println "adding ${queryResults.size()} activities read from DB";
					recentActivities.addAll( queryResults );
			}
			else
			{
				println( "no friends, so no activity read from DB" );	
			}
		}
		else
		{
			println "Reading NO messages from DB";	
		}
		
		println "recentActivities.size() = ${recentActivities.size()}";
		return recentActivities;
	}	
}
