---
title: "Rx onError Guidelines"
author: "Netflix Technology Blog"
date: "May 1, 2019"
url: "https://netflixtechblog.com/android-rx-onerror-guidelines-e68e8dc7383f"
tags: ['Rxjava', 'Rxjava2', 'Rx', 'API']
---

# Rx onError Guidelines

By Ed Ballot

“Creating a good API is hard.” — _anyone who has created an API used by others_

As with any API, wrapping your data stream in a Rx observable requires consideration for reasonable error handling and intuitive behavior. The following guidelines are intended to help developers create consistent and intuitive API.

![image](../images/e2e1a330427b6d81.png)

Since we frequently create Rx Observables in our Android app, we needed a common understanding of when to use onNext() and when to use onError() to make the API more consistent for subscribers. The divergent understanding is partially because the name “onError” is a bit misleading. The item emitted by onError() is not a simple error, but a throwable that can cause significant damage if not caught. **Our app has a global handler that prevents it from crashing outright, but an uncaught exception can still leave parts of the app in an unpredictable state.**

**TL;DR** — Prefer onNext() and only use onError() for exceptional cases.

## Considerations for onNext / onError

The following are points to consider when determining whether to use onNext() versus onError().

## The Contract

First here are the definitions of the two from the ReactiveX [contract page](http://reactivex.io/documentation/contract.html):

> **_OnNext  
> _**_conveys an _item_ that is _emitted_ by the Observable to the observer_**_OnError  
> _**_indicates that the Observable has terminated with a specified error condition and that it will be emitting no further items_

As pointed out in the above definition, a subscription is automatically disposed after onError(), just like after onComplete(). Because of this, onError() should only be used to signal a fatal error and never to signal an intermittent problem where more data is expected to stream through the subscription after the error.

## Treat it like an Exception

**Limit using onError() for exceptional circumstances when you’d also consider throwing an Error or Exception.** The reasoning is that the onError() parameter is a Throwable. An example for differentiating: a database query returning zero results is typically not an exception. The database returning zero results because it was forcibly closed (or otherwise put in a state that cancels the running query) would be an exceptional condition.

## Be Consistent

Do not make your observable emit a mix of both deterministic and non-deterministic errors. Something is deterministic if the same input always results in the same output, such as dividing by 0 will fail every time. Something is non-deterministic if the same inputs may result in different outputs, such as a network request which may timeout or may return results before the timeout. Rx has convenience methods built around error handling, such as [retry()](http://reactivex.io/documentation/operators/retry.html) (and our retryWithBackoff()). The primary use of retry() is to automatically re-subscribe an observable that has non-deterministic errors. When an observable mixes the two types of errors, it makes retrying less obvious since retrying a deterministic failures doesn’t make sense — or is wasteful since the retry is guaranteed to fail. (Two notes: 1. retry can also be used in certain deterministic cases like user login attempts, where the failure is caused by incorrectly entering credentials. 2. For mixed errors, retryWhen() could be used to only retry the non-deterministic errors.) If you find your observable needs to emit both types of errors, consider whether there is an appropriate separation of concerns. It may be that the observable can be split into several observables that each have a more targeted purpose.

## Be Consistent with Underlying APIs

When wrapping an asynchronous API in Rx, consider maintaining consistency with the underlying API’s error handling. For example, if you are wrapping a touch event system that treats moving off the device’s touchscreen as an exception and terminates the touch session, then it may make sense to emit that error via onError(). On the other hand, if it treats moving off the touchscreen as a data event and allows the user to drag their finger back onto the screen, it makes sense to emit it via onNext().

## Avoid Business Logic

Related to the previous point. Avoid adding business logic that interprets the data and converts it into errors. The code that the observable is wrapping should have the appropriate logic to perform these conversions. In the rare case that it does not, consider adding an abstraction layer that encapsulates this logic (for both normal and error cases) rather than building it into the observable.

## Passing Details in onError()

If your code is going to use onError(), remember that the throwable it emits should include appropriate data for the subscriber to understand what went wrong and how to handle it.

For example, our Falcor response handler uses a FalcorError class that includes the Status from the callback. Repositories could also throw an extension of this class, if extra details need to be included.

---
**Tags:** Rxjava · Rxjava2 · Rx · API
