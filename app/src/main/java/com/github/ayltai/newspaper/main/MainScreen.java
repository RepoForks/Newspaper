package com.github.ayltai.newspaper.main;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.Transition;
import com.github.ayltai.newspaper.BuildConfig;
import com.github.ayltai.newspaper.Constants;
import com.github.ayltai.newspaper.R;
import com.github.ayltai.newspaper.graphics.DaggerGraphicsComponent;
import com.github.ayltai.newspaper.graphics.GraphicsModule;
import com.github.ayltai.newspaper.graphics.ImageLoaderCallback;
import com.github.ayltai.newspaper.setting.Settings;
import com.github.ayltai.newspaper.setting.SettingsActivity;
import com.github.ayltai.newspaper.util.ContextUtils;
import com.github.ayltai.newspaper.util.Irrelevant;
import com.github.ayltai.newspaper.util.TestUtils;
import com.github.piasy.biv.loader.ImageLoader;
import com.jakewharton.rxbinding2.view.RxView;
import com.yalantis.guillotine.animation.GuillotineAnimation;
import com.yalantis.guillotine.interfaces.GuillotineListener;

import flow.ClassKey;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.PublishProcessor;

@SuppressLint("ViewConstructor")
public final class MainScreen extends FrameLayout implements MainPresenter.View, KenBurnsView.TransitionListener {
    public static final class Key extends ClassKey implements Parcelable {
        public Key() {
        }

        //region Parcelable

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        }

        protected Key(@NonNull final Parcel in) {
        }

        public static final Parcelable.Creator<MainScreen.Key> CREATOR = new Parcelable.Creator<MainScreen.Key>() {
            @NonNull
            @Override
            public MainScreen.Key createFromParcel(@NonNull final Parcel source) {
                return new MainScreen.Key(source);
            }

            @NonNull
            @Override
            public MainScreen.Key[] newArray(final int size) {
                return new MainScreen.Key[size];
            }
        };

        //endregion
    }

    //region Events

    private final BehaviorProcessor<Object>  attachedToWindow   = BehaviorProcessor.create();
    private final BehaviorProcessor<Object>  detachedFromWindow = BehaviorProcessor.create();
    private final BehaviorProcessor<Integer> pageChanges        = BehaviorProcessor.create();
    private final PublishProcessor<Object>   previousClicks     = PublishProcessor.create();
    private final PublishProcessor<Object>   nextClicks         = PublishProcessor.create();

    //endregion

    @Inject
    ImageLoader imageLoader;

    private final CompositeDisposable disposable = new CompositeDisposable();

    //region Components

    private CollapsingToolbarLayout toolbar;
    private ViewPager               viewPager;
    private View                    headerContainer;
    private KenBurnsView            headerImage0;
    private KenBurnsView            headerImage1;
    private ViewSwitcher            viewSwitcher;
    private View                    previousButton;
    private View                    nextButton;

    //endregion

    //region Variables

    private MainAdapter         adapter;
    private boolean             hasAttached;
    private boolean             isBound;
    private boolean             isDrawerOpened;
    private GuillotineAnimation animation;
    private List<String>        images;
    private int                 imageIndex;
    private boolean             imageToggle;

    //endregion

    @Inject
    public MainScreen(@NonNull final Context context) {
        super(context);

        DaggerGraphicsComponent.builder()
            .graphicsModule(new GraphicsModule())
            .build()
            .inject(this);
    }

    @Override
    public void bind(@NonNull final MainAdapter adapter) {
        if (!this.isBound) {
            this.isBound = true;

            this.viewPager.setAdapter(this.adapter = adapter);

            this.pageChanges.onNext(0);
        }
    }

    @Override
    public void updateHeaderTitle(@NonNull final CharSequence title) {
        this.toolbar.setTitle(title);
    }

    @Override
    public void updateHeaderImages(@Nullable final List<String> images) {
        this.images     = images;
        this.imageIndex = 0;

        if (this.images != null && !this.images.isEmpty()) {
            Collections.shuffle(this.images);

            if (this.imageToggle) {
                this.updateHeaderImage(this.headerImage1);
            } else {
                this.updateHeaderImage(this.headerImage0);
            }
        }
    }

    private void updateHeaderImage(@NonNull final ImageView imageView) {
        if (this.images == null || this.images.isEmpty()) {
            imageView.post(() -> imageView.setImageBitmap(null));
        } else if (!TestUtils.isRunningInstrumentedTest()) {
            if (this.imageIndex == this.images.size()) {
                this.imageIndex = 0;

                Collections.shuffle(this.images);
            }

            this.imageLoader.loadImage(Uri.parse(this.images.get(this.imageIndex++)), new ImageLoaderCallback(imageView));

            if (this.imageIndex < this.images.size()) this.imageLoader.prefetch(Uri.parse(this.images.get(this.imageIndex)));
        }
    }

    //region Navigation

    @Override
    public void enablePrevious(final boolean enabled) {
        this.previousButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    @Override
    public void enableNext(final boolean enabled) {
        this.nextButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    @Override
    public Flowable<Object> previousClicks() {
        return this.previousClicks;
    }

    @Override
    public Flowable<Object> nextClicks() {
        return this.nextClicks;
    }

    @Override
    public void navigatePrevious() {
        this.viewPager.setCurrentItem(this.viewPager.getCurrentItem() - 1, true);
    }

    @Override
    public void navigateNext() {
        this.viewPager.setCurrentItem(this.viewPager.getCurrentItem() + 1, true);
    }

    //endregion

    @Override
    public boolean goBack() {
        if (this.isDrawerOpened) {
            this.animation.close();

            return true;
        }

        return false;
    }

    @NonNull
    @Override
    public Flowable<Integer> pageChanges() {
        return this.pageChanges;
    }

    @Override
    public void onTransitionStart(final Transition transition) {
    }

    @Override
    public void onTransitionEnd(final Transition transition) {
        if (this.imageToggle) {
            this.updateHeaderImage(this.headerImage0);
            this.viewSwitcher.showPrevious();
        } else {
            this.updateHeaderImage(this.headerImage1);
            this.viewSwitcher.showNext();
        }

        this.imageToggle = !this.imageToggle;
    }

    //region Lifecycle

    @NonNull
    @Override
    public Flowable<Object> attachments() {
        return this.attachedToWindow;
    }

    @NonNull
    @Override
    public Flowable<Object> detachments() {
        return this.detachedFromWindow;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!this.hasAttached) {
            this.hasAttached = true;

            final ViewGroup view = (ViewGroup)LayoutInflater.from(this.getContext()).inflate(R.layout.screen_main, this, false);
            if (BuildConfig.DEBUG) view.removeView(view.findViewById(R.id.statusBarPadding));

            this.toolbar = view.findViewById(R.id.collapsingToolbarLayout);

            // Sets up header
            this.headerContainer = view.findViewById(R.id.headerContainer);
            this.viewSwitcher    = view.findViewById(R.id.viewSwitcher);
            this.headerImage0    = view.findViewById(R.id.headerImage0);
            this.headerImage1    = view.findViewById(R.id.headerImage1);

            this.headerImage0.setTransitionListener(this);
            this.headerImage1.setTransitionListener(this);

            // Sets up navigation
            this.previousButton = view.findViewById(R.id.navigate_previous);
            this.nextButton     = view.findViewById(R.id.navigate_next);

            this.disposable.add(RxView.clicks(this.previousButton).subscribe(dummy -> this.previousClicks.onNext(Irrelevant.INSTANCE)));
            this.disposable.add(RxView.clicks(this.nextButton).subscribe(dummy -> this.nextClicks.onNext(Irrelevant.INSTANCE)));

            // Sets up ViewPager
            this.viewPager = view.findViewById(R.id.viewPager);
            this.viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(final int position) {
                    MainScreen.this.pageChanges.onNext(position);
                }
            });

            ((TabLayout)view.findViewById(R.id.tabLayout)).setupWithViewPager(this.viewPager);

            this.addView(view);

            this.setUpDrawerMenu(view);

            this.hasAttached = true;
        }

        this.headerContainer.setVisibility(Settings.isHeaderImageEnabled(this.getContext()) ? View.VISIBLE : View.GONE);

        this.attachedToWindow.onNext(Irrelevant.INSTANCE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        this.detachedFromWindow.onNext(Irrelevant.INSTANCE);
    }

    @Override
    public void close() {
        if (this.adapter != null) {
            this.adapter.close();
            this.adapter = null;
        }

        if (!this.disposable.isDisposed()) this.disposable.dispose();
    }

    //endregion

    private void setUpDrawerMenu(@NonNull final View view) {
        final ViewGroup drawerMenu = (ViewGroup)LayoutInflater.from(this.getContext()).inflate(R.layout.view_drawer_menu, this, false);
        if (BuildConfig.DEBUG) drawerMenu.removeView(drawerMenu.findViewById(R.id.statusBarPadding));

        this.disposable.add(RxView.clicks(drawerMenu).subscribe(dummy -> {
            // Prevent click-through
        }));

        this.disposable.add(RxView.clicks(drawerMenu.findViewById(R.id.action_settings)).subscribe(dummy -> {
            this.animation.close();
            ((Activity)this.getContext()).startActivityForResult(new Intent(this.getContext(), SettingsActivity.class), Constants.REQUEST_SETTINGS);
        }));

        this.disposable.add(RxView.clicks(drawerMenu.findViewById(R.id.action_about)).subscribe(dummy -> {
            this.animation.close();
            ContextUtils.showAbout(this.getContext());
        }));

        this.addView(drawerMenu);

        this.animation = new GuillotineAnimation.GuillotineBuilder(drawerMenu, drawerMenu.findViewById(R.id.drawer_close), this.findViewById(R.id.drawer_open))
            .setStartDelay(Constants.DRAWER_MENU_ANIMATION_DELAY)
            .setActionBarViewForAnimation(view.findViewById(R.id.toolbar))
            .setClosedOnStart(true)
            .setGuillotineListener(new GuillotineListener() {
                @Override
                public void onGuillotineOpened() {
                    MainScreen.this.isDrawerOpened = true;
                }

                @Override
                public void onGuillotineClosed() {
                    MainScreen.this.isDrawerOpened = false;
                }
            })
            .build();
    }
}
