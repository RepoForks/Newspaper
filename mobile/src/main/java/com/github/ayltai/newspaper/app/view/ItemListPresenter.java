package com.github.ayltai.newspaper.app.view;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.github.ayltai.newspaper.config.UserConfig;
import com.github.ayltai.newspaper.data.ItemListLoader;
import com.github.ayltai.newspaper.data.model.Item;
import com.github.ayltai.newspaper.view.ListPresenter;

import io.reactivex.Flowable;

public class ItemListPresenter extends ListPresenter<Item, ItemListPresenter.View> {
    public interface View extends ListPresenter.View<Item> {
    }

    private final String category;

    public ItemListPresenter(@NonNull final String category) {
        this.category = category;
    }

    @Override
    public Flowable<List<Item>> load() {
        if (this.getView() == null) return Flowable.just(Collections.emptyList());

        final Activity activity = this.getView().getActivity();
        if (activity == null) return Flowable.just(Collections.emptyList());

        final ItemListLoader.Builder builder = new ItemListLoader.Builder((AppCompatActivity)activity).setCategory(this.category);
        for (final String source : UserConfig.getSources(this.getView().getContext())) builder.addSource(source);

        return builder.build();
    }
}
