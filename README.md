HandlerCallAdapterFactory
=====================

A Retrofit CallAdapter.Factory that posts Call callbacks with an Android Handler.

On Android, Retrofit 2 installs a CallAdapter.Factory that handles all Call types and posts the callbacks on the main thread. Many messages build up in the main thread Handler's MessageQueue, and the callback may not be run for up to hundreds of milliseconds. With HandlerCallAdapterFactory, service methods can be annotated with `@UseHandlerPost` to use `Hander.postAtFrontOfQueue` to get results posted much sooner.

Download
--------

Gradle:

```groovy
implementation 'com.nightlynexus.handlercalladapterfactory:handlercalladapterfactory:0.1.0'
```

Usage
-----

```java
interface Service {
  @UseHandlerPost(atFrontOfQueue = true) @GET("/") Call<Void> get();
}

Retrofit retrofit = new Retrofit.Builder()
    // Add the HandlerCallAdapterFactory after other CallAdapters factories
    // to let other CallAdapters delegate.
    .addCallAdapterFactory(HandlerCallAdapterFactory.createMain())
    .build();
```

License
--------

    Copyright 2019 Eric Cochran

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
