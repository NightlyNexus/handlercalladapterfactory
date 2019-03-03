package com.nightlynexus.handlercalladapterfactory;

import android.os.Handler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public final class HandlerCallAdapterFactoryTest {
  interface Service {
    @UseHandlerPost(atFrontOfQueue = false) @GET("/") Call<Void> get();

    @UseHandlerPost() @GET("/") Call<Void> getAtFrontOfQueue();
  }

  @Test public void postOnResponse() throws Exception {
    final CountDownLatch latch = new CountDownLatch(2);
    Handler mockHandler = mock(Handler.class);
    when(mockHandler.post(any(Runnable.class))).thenAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) {
        latch.countDown();
        Runnable runnable = invocation.getArgument(0);
        runnable.run();
        return null;
      }
    });

    MockWebServer server = new MockWebServer();
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/"))
        .addCallAdapterFactory(HandlerCallAdapterFactory.create(mockHandler))
        .build();
    Service service = retrofit.create(Service.class);
    server.enqueue(new MockResponse());

    Call<Void> call = service.get();
    call.enqueue(new Callback<Void>() {
      @Override public void onResponse(Call<Void> call, Response<Void> response) {
        latch.countDown();
      }

      @Override public void onFailure(Call<Void> call, Throwable t) {
        throw new AssertionError();
      }
    });
    assertThat(latch.await(10, SECONDS)).isTrue();
  }

  @Test public void postOnFailure() throws Exception {
    final CountDownLatch latch = new CountDownLatch(2);
    Handler mockHandler = mock(Handler.class);
    when(mockHandler.post(any(Runnable.class))).thenAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) {
        latch.countDown();
        Runnable runnable = invocation.getArgument(0);
        runnable.run();
        return null;
      }
    });

    MockWebServer server = new MockWebServer();
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/"))
        .addCallAdapterFactory(HandlerCallAdapterFactory.create(mockHandler))
        .build();
    Service service = retrofit.create(Service.class);
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    Call<Void> call = service.get();
    call.enqueue(new Callback<Void>() {
      @Override public void onResponse(Call<Void> call, Response<Void> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<Void> call, Throwable t) {
        latch.countDown();
      }
    });
    assertThat(latch.await(10, SECONDS)).isTrue();
  }

  @Test public void postAtFrontOfQueueOnResponse() throws Exception {
    final CountDownLatch latch = new CountDownLatch(2);
    Handler mockHandler = mock(Handler.class);
    when(mockHandler.postAtFrontOfQueue(any(Runnable.class))).thenAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) {
        latch.countDown();
        Runnable runnable = invocation.getArgument(0);
        runnable.run();
        return null;
      }
    });

    MockWebServer server = new MockWebServer();
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/"))
        .addCallAdapterFactory(HandlerCallAdapterFactory.create(mockHandler))
        .build();
    Service service = retrofit.create(Service.class);
    server.enqueue(new MockResponse());

    Call<Void> call = service.getAtFrontOfQueue();
    call.enqueue(new Callback<Void>() {
      @Override public void onResponse(Call<Void> call, Response<Void> response) {
        latch.countDown();
      }

      @Override public void onFailure(Call<Void> call, Throwable t) {
        throw new AssertionError();
      }
    });
    assertThat(latch.await(10, SECONDS)).isTrue();
  }

  @Test public void postAtFrontOfQueueOnFailure() throws Exception {
    final CountDownLatch latch = new CountDownLatch(2);
    Handler mockHandler = mock(Handler.class);
    when(mockHandler.postAtFrontOfQueue(any(Runnable.class))).thenAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) {
        latch.countDown();
        Runnable runnable = invocation.getArgument(0);
        runnable.run();
        return null;
      }
    });

    MockWebServer server = new MockWebServer();
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/"))
        .addCallAdapterFactory(HandlerCallAdapterFactory.create(mockHandler))
        .build();
    Service service = retrofit.create(Service.class);
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    Call<Void> call = service.getAtFrontOfQueue();
    call.enqueue(new Callback<Void>() {
      @Override public void onResponse(Call<Void> call, Response<Void> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<Void> call, Throwable t) {
        latch.countDown();
      }
    });
    assertThat(latch.await(10, SECONDS)).isTrue();
  }

  @Test public void callCanceledBetweenOnResponseAndHandlerPostCallsOnFailure() throws Exception {
    final AtomicReference<Call<Void>> callReference = new AtomicReference<>();
    Handler mockHandler = mock(Handler.class);
    when(mockHandler.post(any(Runnable.class))).thenAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) {
        callReference.get().cancel();
        Runnable runnable = invocation.getArgument(0);
        runnable.run();
        return null;
      }
    });

    MockWebServer server = new MockWebServer();
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/"))
        .addCallAdapterFactory(HandlerCallAdapterFactory.create(mockHandler))
        .build();
    Service service = retrofit.create(Service.class);
    server.enqueue(new MockResponse());

    final CountDownLatch latch = new CountDownLatch(1);
    Call<Void> call = service.get();
    callReference.set(call);
    call.enqueue(new Callback<Void>() {
      @Override public void onResponse(Call<Void> call, Response<Void> response) {
        throw new AssertionError();
      }

      @Override public void onFailure(Call<Void> call, Throwable t) {
        latch.countDown();
      }
    });
    assertThat(latch.await(10, SECONDS)).isTrue();
  }
}
