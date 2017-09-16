package com.github.ayltai.newspaper.data;

import java.util.Collection;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.ayltai.newspaper.util.Irrelevant;
import com.github.ayltai.newspaper.util.RxUtils;
import com.github.ayltai.newspaper.util.TestUtils;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.realm.Realm;

public abstract class RealmLoader<D> extends RxLoader<D> {
    private Realm realm;

    protected RealmLoader(@NonNull final Context context) {
        super(context);
    }

    protected RealmLoader(@NonNull final Context context, @Nullable final Bundle args) {
        super(context, args);
    }

    @NonNull
    protected Scheduler getScheduler() {
        return DataManager.SCHEDULER;
    }

    @Nullable
    protected Realm getRealm() {
        return this.realm;
    }

    protected boolean isValid() {
        if (this.realm == null || this.realm.isClosed()) {
            if (TestUtils.isLoggable()) throw new IllegalStateException("No Realm instance is available");

            return false;
        }

        return true;
    }

    @NonNull
    @Override
    protected Observable<D> load(@NonNull final Context context, @Nullable final Bundle args) {
        if (this.isValid()) {
            return Observable.create(emitter -> this.loadFromLocalSource(context, args)
                .compose(RxUtils.applyObservableSchedulers(this.getScheduler()))
                .subscribe(
                    data -> {
                        if (data == null || (data instanceof Collection && ((Collection)data).isEmpty())) {
                            this.loadFromRemoteSource(context, args).subscribe(emitter::onNext);
                        } else {
                            emitter.onNext(data);
                        }
                    },
                    error -> {
                        if (TestUtils.isLoggable()) Log.e(this.getClass().getSimpleName(), error.getMessage(), error);
                    }));
        }

        return this.loadFromRemoteSource(context, args);
    }

    @NonNull
    protected abstract Observable<D> loadFromLocalSource(@NonNull Context context, @Nullable Bundle args);

    @NonNull
    protected abstract Observable<D> loadFromRemoteSource(@NonNull Context context, @Nullable Bundle args);

    @CallSuper
    @Override
    protected void onForceLoad() {
        Single.<Realm>create(emitter -> emitter.onSuccess(DaggerDataComponent.builder()
                .dataModule(new DataModule(this.getContext()))
                .build()
                .realm()))
            .compose(RxUtils.applySingleSchedulers(this.getScheduler()))
            .subscribe(
                realm -> {
                    this.realm = realm;

                    if (TestUtils.isLoggable()) Log.d(this.getClass().getSimpleName(), "A Realm instance is created");

                    super.onForceLoad();
                },
                error -> {
                    if (TestUtils.isLoggable()) Log.e(this.getClass().getSimpleName(), error.getMessage(), error);
                });
    }

    @CallSuper
    @Override
    protected boolean onCancelLoad() {
        final boolean result = super.onCancelLoad();

        Single.<Irrelevant>create(
            emitter -> {
                if (this.realm != null) {
                    this.realm.close();
                    this.realm = null;
                }

                emitter.onSuccess(Irrelevant.INSTANCE);
            })
            .compose(RxUtils.applySingleSchedulers(this.getScheduler()))
            .subscribe(
                irrelevant -> {
                    if (TestUtils.isLoggable()) Log.d(this.getClass().getSimpleName(), "A Realm instance is closed");
                },
                error -> {
                    if (TestUtils.isLoggable()) Log.e(this.getClass().getSimpleName(), error.getMessage(), error);
                });

        return result;
    }
}
