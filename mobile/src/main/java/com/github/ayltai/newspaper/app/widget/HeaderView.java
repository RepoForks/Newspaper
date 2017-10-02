package com.github.ayltai.newspaper.app.widget;

import java.util.Date;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.ayltai.newspaper.R;
import com.github.ayltai.newspaper.util.DateUtils;

public final class HeaderView extends ItemView {
    public static final int VIEW_TYPE = R.id.view_type_item_header;

    //region Components

    private final SimpleDraweeView avatar;
    private final TextView         source;
    private final TextView         publishDate;

    //endregion

    public HeaderView(@NonNull final Context context) {
        super(context);

        final View view = LayoutInflater.from(context).inflate(R.layout.view_news_cozy_header, this, true);

        this.container   = view.findViewById(R.id.container);
        this.avatar      = view.findViewById(R.id.avatar);
        this.source      = view.findViewById(R.id.source);
        this.publishDate = view.findViewById(R.id.publish_date);
    }

    //region Properties

    @Override
    public void setAvatar(@DrawableRes final int avatar) {
        this.avatar.setImageResource(avatar);
    }

    @Override
    public void setSource(@Nullable final CharSequence source) {
        if (TextUtils.isEmpty(source)) {
            this.source.setVisibility(View.GONE);
        } else {
            this.source.setVisibility(View.VISIBLE);
            this.source.setText(source);
        }
    }

    @Override
    public void setPublishDate(@Nullable final Date date) {
        if (date == null) {
            this.publishDate.setVisibility(View.GONE);
        } else {
            this.publishDate.setVisibility(View.VISIBLE);
            this.publishDate.setText(DateUtils.getTimeAgo(this.getContext(), date.getTime()));
        }
    }

    //endregion
}