package com.nightlynexus.handlercalladapterfactory;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public final class HandlerCallAdapterFactory extends CallAdapter.Factory {
  final Handler callbackHandler;

  HandlerCallAdapterFactory(Handler callbackHandler) {
    this.callbackHandler = callbackHandler;
  }

  public static HandlerCallAdapterFactory createMain() {
    return create(new Handler(Looper.getMainLooper()));
  }

  public static HandlerCallAdapterFactory create(Handler callbackHandler) {
    return new HandlerCallAdapterFactory(callbackHandler);
  }

  @Override public CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
    boolean atFrontOfQueue = false;
    Annotation[] nextAnnotations = null;
    for (int i = 0, length = annotations.length; i < length; i++) {
      if (annotations[i] instanceof UseHandlerPost) {
        atFrontOfQueue = ((UseHandlerPost) annotations[i]).atFrontOfQueue();
        nextAnnotations = new Annotation[length - 1];
        System.arraycopy(annotations, 0, nextAnnotations, 0, i);
        System.arraycopy(annotations, i + 1, nextAnnotations, i, length - i - 1);
        break;
      }
    }
    if (nextAnnotations == null) {
      return null;
    }
    if (getRawType(returnType) != Call.class) {
      throw new IllegalStateException("UseHandlerPost must be used with Call return type.");
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalArgumentException(
          "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
    }
    final Type responseType = getParameterUpperBound(0, (ParameterizedType) returnType);
    final boolean atFrontOfQueueResult = atFrontOfQueue;
    return new CallAdapter<Object, Call<?>>() {
      @Override public Type responseType() {
        return responseType;
      }

      @Override public Call<Object> adapt(Call<Object> call) {
        return new HandlerCallbackCall<>(callbackHandler, atFrontOfQueueResult, call);
      }
    };
  }

  static final class HandlerCallbackCall<T> implements Call<T> {
    final Handler callbackHandler;
    final boolean atFrontOfQueue;
    final Call<T> delegate;

    HandlerCallbackCall(Handler callbackHandler, boolean atFrontOfQueue, Call<T> delegate) {
      this.callbackHandler = callbackHandler;
      this.atFrontOfQueue = atFrontOfQueue;
      this.delegate = delegate;
    }

    @Override public void enqueue(final Callback<T> callback) {
      if (callback == null) {
        throw new NullPointerException("callback == null");
      }

      delegate.enqueue(new Callback<T>() {
        @Override public void onResponse(Call<T> call, final Response<T> response) {
          Runnable callbackRunnable = new Runnable() {
            @Override public void run() {
              if (delegate.isCanceled()) {
                // Emulate OkHttp's behavior of throwing/delivering an IOException on cancellation.
                callback.onFailure(HandlerCallbackCall.this,
                    new IOException("Canceled"));
              } else {
                callback.onResponse(HandlerCallbackCall.this, response);
              }
            }
          };
          if (atFrontOfQueue) {
            callbackHandler.postAtFrontOfQueue(callbackRunnable);
          } else {
            callbackHandler.post(callbackRunnable);
          }
        }

        @Override public void onFailure(Call<T> call, final Throwable t) {
          Runnable callbackRunnable = new Runnable() {
            @Override public void run() {
              callback.onFailure(HandlerCallbackCall.this, t);
            }
          };
          if (atFrontOfQueue) {
            callbackHandler.postAtFrontOfQueue(callbackRunnable);
          } else {
            callbackHandler.post(callbackRunnable);
          }
        }
      });
    }

    @Override public boolean isExecuted() {
      return delegate.isExecuted();
    }

    @Override public Response<T> execute() throws IOException {
      return delegate.execute();
    }

    @Override public void cancel() {
      delegate.cancel();
    }

    @Override public boolean isCanceled() {
      return delegate.isCanceled();
    }

    @SuppressWarnings("CloneDoesntCallSuperClone") // Performing deep clone.
    @Override public Call<T> clone() {
      return new HandlerCallbackCall<>(callbackHandler, atFrontOfQueue, delegate.clone());
    }

    @Override public Request request() {
      return delegate.request();
    }
  }
}
