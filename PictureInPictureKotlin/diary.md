### Task 2 - Unit tests
The project already has some tests for MainActivity and MovieActivity, however none for the 
MainViewModel. Please add some that you would think are appropriate and include any app changes that 
may be needed. Add any libraries that you would like to be able to achieve this task.

We are looking for how changes to support unit tests are made, as well as choice of things to test.

# Task 2 - Solution Diary
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

