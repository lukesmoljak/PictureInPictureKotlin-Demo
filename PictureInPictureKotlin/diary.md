# Task 2 - Unit tests
The project already has some tests for MainActivity and MovieActivity, however none for the 
MainViewModel. Please add some that you would think are appropriate and include any app changes that 
may be needed. Add any libraries that you would like to be able to achieve this task.

We are looking for how changes to support unit tests are made, as well as choice of things to test.

### Task 2 - Solution Diary
- create unit test package structure that matches main code structure. 
- create blank MainViewModelTest class and add dependency for junit5, with lateinit reference to 
self at top. I typically use "sut" to indicate system under test for unit tests, that way the "when"
of a unit test is very clear. 
- Create setUp function and blank unit test. Tests not received. Switched to junit 4 to see what 
happens. Blank unit test fails due to needing android framework to execute, but tests are received. 
Compatibility issues for junit5, so let's just avoid that and go with junit4. 
- The above error for junit4 is:
  java.lang.RuntimeException: Method uptimeMillis in android.os.SystemClock not mocked
To complete this we need to do the following:
1 - Create a class that can mock this functionality - see SystemClockHelper.kt. Pretty basic class,
just add a function that returns SystemClock.updateMillis()
2 - We need to add a reference to the above class into the constructor of the ViewModel. The 
constructor is used so that we can mock this in the unit test. We can also update the references to 
SystemClock to use our helper instead. 
3 - Go back to unit test and we will have a compile error for instantiating the ViewModel. We need to 
add a reference to the class we made. This class needs to be instantiated, but this is a unit test, 
so we would be breaking the whole point of unit testing we used the real implementation. The test 
would also still fail as the real implementation uses android imports. Import dependency for MockK 
library to enable mocking capabilities and use @MockK annotation on lateinit reference to 
SystemClockHelper. 
4 - It still doesn't instantiate. I forgot to initiatise MockK. Add MockKAnnotations.init to setup. 
Run test... no answer found for SystemClockHelper(systemClockHelper#1).uptimeMillis()
5 - Change MockK annotation of SystemClockHelper to @RelaxedMockK, re-run test and our blank test 
passes! Huzzah! We can change this back later to the previous annotation, should we desire. 
- So now the real testing begins. Let's map out what we want to test in this class. Let's have a 
look at our public functions and public LiveData objects. 
We have 2 function entry points:
1 - startOrPause()
2 - clear() -> Not to be confused with the ViewModel lifecycle callback onCleared, this function 
clears the millis of the timer so the output will reset. 
We have public properties:
1 - time -> LiveData of String type that is formatting millis into a readable output for user 
2 - started -> LiveData of Boolean type that flags if the timer is started or paused 
- Now let's go back to the app and go over the functionality so that we can see it working.
The app is now crashing due to ParamsBuilder, but this doesn't make sense. What I am pretty certain 
is happening, is that we haven't yet setup our DI framework, so instatiation of ViewModel is 
breaking Activity code, which is totally expected. Let's implement koin. 
1 - Create new Application class - PictureInPictureApplication, inherit from Application and override
onCreate. And let's add in the startKoin code. I forget the exact implementation so I referenced 
code I'd written in the past. 
2 - Add new application to class to android:name attribute in Application tag of Manifest
3 - Create appModule file in di package to define koin module for how we will inject our dependencies. 
4 - I'll use Factory for both the ViewModel and the SystemClockHelper as singletons should be used 
sparingly.
5 - Let's try running the app again to see what happens. Yep, it crashes again but the error has 
changed. It cannot create the ViewModel class. This is because we are creating the ViewModel with 
ViewModelProvider class, rather than koin. So let's change the reference to the ViewModel to fix this
import org.koin.androidx.viewmodel.ext.android.viewModel
private val viewModel: MainViewModel by viewModel()
Re run the app.... and now it doesn't crash!
- Back to creating test cases:
clear function has 2 scenarios depending on if the state is paused or started and maybe a third for 
if the timer hasn't yet been started and paused, but I don't see much value there. Let's write them!
Given paused timer after 1 second, When invoke clear, Then timer should be cleared
Given timer is started, When invoke clear, Then timer should reset and continue counting
Now let's write the cases for startOrPause:
Given timer is paused, When invoke startOrPause, Then timer should start
Given timer is started, When invoke startOrPause, Then timer should pause
Given timer is started, When invoke startOrPause, Then coroutine should gracefully cancel
I'm not sure how this last test case will go, I may end up removing it, but it would be nice to 
ensure that in the future no-one removes isActive coroutine call. Also I'm not clear on how awaitFrame 
will impact the test. Coroutine testing is difficult. 
- Now let's code the test cases. I start by creating all tests and asserting an always failure case 
so that if I somehow miss the test case and push, CI/CD will catch this and reject my PR immediately. 
- I also like to use assertK library for assertions so I'll add that. 
- Change @RelaxedMockK to @MockK again and let's go!
- Run test while using every mockk function to mock responses. Still failed. It's likely this is 
being invoked before the function during initialisation.
- Yes, this is the case, adding mock response to the setUp function. Testing... now different error. 
- java.lang.RuntimeException: Method getMainLooper in android.os.Looper not mocked.
Also, sut.clear() is showing a warning. This is likely test executor rule needing to be added. 
Adding dependency and importing... new error and warning disappeared. 
- Result...
  Expected :00:00:00
  Actual   :null
Likely that setting 0L on the mock for updateMillis is the cause, let's pick a day from the prior week
March 21 2024 12am UTC (1711065600000)
- Not sure where this is going to go with debugging these issues, so I'm going to push code up at this 
current place so it is clearer how I come to my solution. 
- Following error 
Function UnconfinedTestCoroutineDispatcher.dispatch can only be used by the yield function. 
If you wrap Unconfined dispatcher in your code, make sure you properly delegate isDispatchNeeded and dispatch calls.
- We are able to resolve this issue by changing the test dispatcher to use the Standard Test Dispatcher
- Now we are facing a new issue, that Choreographer needs to be mocked. 
I spend some time digging into the code as well as looking at online resources on Medium, stackoverflow, 
and generally on ChatGPT for some ideas on how to solve this.
I tried mockkStatic, which I haven't really used before but it didn't work for me in the unit test 
environment. I came to realise that Choreographer is an android view class meaning to unit test this 
I'll need to mock a helper class that contains the awaitFrame call.
Created CoroutinesHelper class to deal with this, and setup in test class and DI / ViewModel.
Ran the program to test if the helper worked in the production code. It did not, and I realised that 
I needed to wrap awaitFrame in a withContext call in the helper.
- The next issue is that awaitFrame if you just mock a 0L response then it will just run forever as 
the corouitne is in an isActive while loop. So mock this to throw CancellationException. 
- Next I need to mock updateMillis() so that the time returns correctly. Only was familiar with 
returning single values so looked up some sources online to find out how to return a variety of results 
as this function is called many times per function invocation. "answers" and "andThenAnswer" was the 
solution here.
- If we run startOrPause() and then clear(), we cannot guarantee order of execution and so unexpected 
results will happen during our test. To fix this we use advanceUntilIdle() after invoking startOrPause()
- Then we run clear and try to assert the result, however we need to capture many results of the 
live data for time to validate. This lead me down a new path to using slots, however getting this to 
work correctly was a pain, and I found a solution here: https://github.com/mockk/mockk/issues/352
where nidhindev used a mutable list of slotIds instead of a slot object. Tried this and it worked!
- However, when I tried to assert the slotIds were correct, I realised that there was a missing 
value. I ended up changing the order of sequence where instead of calling this slot call half way 
through the test, I added it to the bottom of the test and made all of my assertions there. 
- As I changed the way that I tested the function compared to my original concept, I changed the name 
of the test to reflect this.
Given timer has passed one second, When invoke clear, Then timer should be cleared
- At this point I need to re-evaluate what I am able to test for this unit, as I have not found a 
way to execute start and pause without having threading related test case issues.
- I deleted this test due to above
Given timer is started, When invoke clear, Then timer should reset and continue counting
- I merged the two tests that test "started" Boolean LiveData value as this was fairly easy to test.
Renamed to: 
When invoke startOrPause, Then timer state should be correct
- As the way these coroutines are tested, I had to remove the following test:
Given timer is started, When invoke startOrPause, Then coroutine should gracefully cancel
- I added a final test to verify format of time value
Given 12 hours and 34 minutes and 3 and a half hundredths have passed, When started timer, Then display correct time format
This one was fairly easy, just add expected values to the mock and verify output.

# Task 3 - New feature

When you move from MainActivity to MovieActivity, the timer stops and is restarted when you return 
to the activity again. Instead of that, we would like for the timer to continue running, even if you 
switch between screens. The only time the timer should stop is if the user taps on the pause icon 
in MainActivity.

If you would like to add any UX variations on this, feel free to get creative as you want, just 
ensure that you maintain the timer across navigation between activites.

We are looking for an implementation of some kind of repository where this state will be stored and 
how it is plugged into the rest of the app. Also the integration with the current app to make this 
work will be reviewed.

### Task 3 - Solution Diary
- Initial thoughts are there are multiple ways to handle this.
1 - You could use some kind of shared view model to share between the two activities, however I am 
not a fan of this approach at all as shared view models are often misused and end up causing problems 
long term. 
2 - Use a singleton repository that holds onto the value as it is updated in a map within the 
constructor and have it update when certain ui events occur, such as navigation. This could be connected 
with a UseCase and/or Manager class in the domain layer. 
3 - Use a repository that isn't a singleton and save value on certain ui events in a mechanism like 
SharedPreferences or a database. This could also be connected with a UseCase and/or Manager class 
in the domain layer like #2. 
4 - Use a delegate interface/impl for the view model, it could be scoped potentially or a singleton. 
For this I'd use a singleton.
- Decided to go for #4. The issue I have with #2 and #3 is that awaitFrame() is really an Android 
specific functionality and in my opinion is presentation layer logic. #3 also wouldn't work because 
of how many transactions would need to occur to track the timing. If we want to have the 
functionality continue through multiple views, a view model delegate to me seems like the best solution. 
- To begin I updated packages to reflect Clean Architecture layers. Moved classes into relevant sections. 
Slices are not how I would setup a real project, but for this it'll do. 
- Create interface and Impl and move across functionality from MainViewModel to TimerViewModelDelegateImpl
- Update AppModule to inject the delegate and move the previous imports over to the delegate
- This has now broken our MainViewModelTest from Task1. Let's refactor our code test class to be for 
TimerViewModelDelegateImpl. This was an easy fix, move class to new package, update class name, 
update Type for sut and modify setUp to instantiate the correct new class. Run tests, all pass. 
- Test app to see if this works... start timer and click media session button... press back and crash.
- Roll back to initial repo. Same behaviour occurs, back navigation is broken in this sample so this 
is not a new issue. Will navigate only going through buttons.
- Tested using buttons only, it seems that the ViewModels are creating new instances of the singleton,
this is unexpected. Verified by logging hashCode. 
- Went back to AppModule and realised that I had referenced it with factory rather than single.
Retested and now it works. 
- Added some text views in MovieActivity to display values and set styles to match AppCompat themes.
- If I had more time for this feature, I would investigate the implementation of a solution for 
process death by running adb shell kill <pid> and save the time value to savedStateHandle in the viewmodel.

# Task 1 - Legacy support
Change the minVersion to support API 21. Make change so that the app can gracefully support the 
newer features when possible with their API levels. You are welcome to make any changes to the UI 
as you see fit.

We are looking for some thought into UX and why you think some decisions you took might be better 
for the user. We aren’t that interested in pixel perfect design, Material components are fine, just 
some thought to UX is best.

### Task 1 - Solution Diary
Unfortunately I do not have the time to invest in this task, however I will give you a rundown on my 
initial thought process behind dealing with this task. 
- First downgrade minSdkVersion to 21 and sync and rebuild project to see issues in code.
- Activity class errors:
  - isInPictureInPictureMode - API Level 24
  - super.onPictureInPictureModeChanged - API 26
  - enterPictureInPictureMode - API 26
  - trackPipAnimationHintView - API 26
  - PictureInPictureParams.Builder() API 26
  - PictureInPictureParams.Builder().setActions - API 26
  - PictureInPictureParams.Builder().setAspectRatio - API 26
  - PictureInPictureParams.Builder().setAutoEnterEnabled - API 31
  - PictureInPictureParams.Builder().setSeamlessResizeEnabled - API 31
  - PictureInPictureParams.Builder().build - API 26
  - PictureInPictureParams.Builder().setSourceRectHint - API 26
  - RemoteAction - API 26
- XML issues
  - android:hyphenationFrequency - API 23
  - android:justificationMode - API 26
  - android:drawableTint - API 23
- Other
  - Changes to permission handling happened at API 23. This could be an issue, but not for this 
  example as there are no permissions required. 

- So to resolve the issues for PIP - I would create a lifecycle aware component and using a when 
statement and current SDK_INT I'd add appropriate handlers to the builder. This component would be 
reusable for both Activities, and could be used in other features in the future as well. There is a 
lot to consider here as this will only remove the errors in the compiler. This will not solve our 
problem of what do we do pre API 26.
- For UX in this I would set the button component visibility for PIP to GONE if API < 26. 
- What are our options here?
  - Hide mentions to PIP
  - Create our own pre-26 API version of PIP
  - Find another library that achieves PIP but API but down to level 21
I think the most likely scenario here is that pre API 26 this feature cannot be supported, unless 
there is either another library out there to use or the business invests in building out this 
functionality in house. Realistically for this I think removing mentions to PIP feature pre API 26
is the solution I would go for. 
- I would change the text depending on API level as well for the description. I do not believe 
user will care about features in later versions of Android, they only care about what they have 
currently. So I would remove any PIP text on this, and I would make the feature focus on the 
stopwatch for MainActivity and on ActivityMovie I would change the focus of the text in MovieActivity
to the feature of playing the movie itself and our new functionality of the timer/stopwatch between
Activities.
- For the XML specific issues, I would need to investigate these attributes further to determine 
their impact visually and how they look / behave in lower versions. This would be a conversation 
I'd have between development and design. 