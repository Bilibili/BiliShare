/*
 * Copyright (C) 2015 Bilibili <jungly.ik@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bilibili.socialize.share.utils.selector;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.bilibili.socialize.share.core.SocializeMedia;
import com.bilibili.socialize.share.utils.R;

/**
 * @author Jungly
 * @email jungly.ik@gmail.com
 * @since 2016/1/4.
 */
public abstract class BaseSharePlatformSelector {

    private FragmentActivity mContext;
    private OnShareSelectorDismissListener mDismissListener;
    private AdapterView.OnItemClickListener mItemClickListener;

    private static ShareTarget[] shareTargets = {
            new ShareTarget(SocializeMedia.SINA, R.string.bili_socialize_text_sina_key, R.drawable.bili_socialize_sina_on),
            new ShareTarget(SocializeMedia.WEIXIN, R.string.bili_socialize_text_weixin_key, R.drawable.bili_socialize_wechat),
            new ShareTarget(SocializeMedia.WEIXIN_MONMENT, R.string.bili_socialize_text_weixin_circle_key, R.drawable.bili_socialize_wxcircle),
            new ShareTarget(SocializeMedia.QQ, R.string.bili_socialize_text_qq_key, R.drawable.bili_socialize_qq_on),
            new ShareTarget(SocializeMedia.QZONE, R.string.bili_socialize_text_qq_zone_key, R.drawable.bili_socialize_qzone_on),
            new ShareTarget(SocializeMedia.GENERIC, R.string.bili_share_sdk_others, R.drawable.bili_socialize_sms_on),
            new ShareTarget(SocializeMedia.COPY, R.string.bili_socialize_text_copy_url, R.drawable.bili_socialize_copy_url)
    };

    public BaseSharePlatformSelector(FragmentActivity context, OnShareSelectorDismissListener dismissListener, AdapterView.OnItemClickListener itemClickListener) {
        mContext = context;
        mDismissListener = dismissListener;
        mItemClickListener = itemClickListener;
    }

    public abstract void show();

    public abstract void dismiss();
    
    public void setShareTargets(ShareTarget[] shareTargetList){
        shareTargets=shareTargetList;
    }

    public void release() {
        mContext = null;
        mDismissListener = null;
        mItemClickListener = null;
    }

    protected static GridView createShareGridView(final Context context, AdapterView.OnItemClickListener onItemClickListener) {
        GridView grid = new GridView(context);
        ListAdapter adapter = new ArrayAdapter<ShareTarget>(context, 0, shareTargets) {
            // no need scroll
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bili_socialize_shareboard_item, parent, false);
                view.setBackgroundDrawable(null);
                ImageView image = (ImageView) view.findViewById(R.id.bili_socialize_shareboard_image);
                TextView platform = (TextView) view.findViewById(R.id.bili_socialize_shareboard_pltform_name);

                ShareTarget target = getItem(position);
                image.setImageResource(target.iconId);
                platform.setText(target.titleId);
                return view;
            }
        };
        grid.setNumColumns(-1);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setColumnWidth(context.getResources().getDimensionPixelSize(R.dimen.bili_socialize_shareboard_size));
        grid.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
        grid.setSelector(R.drawable.bili_socialize_selector_item_background);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(onItemClickListener);
        return grid;
    }

    public FragmentActivity getContext() {
        return mContext;
    }

    public AdapterView.OnItemClickListener getItemClickListener() {
        return mItemClickListener;
    }

    public OnShareSelectorDismissListener getDismissListener() {
        return mDismissListener;
    }

    public static class ShareTarget {
        public int titleId;
        public int iconId;
        public SocializeMedia media;

        public ShareTarget(SocializeMedia media, int titleId, int iconId) {
            this.media = media;
            this.iconId = iconId;
            this.titleId = titleId;
        }
    }

    public interface OnShareSelectorDismissListener {
        void onDismiss();
    }

}
