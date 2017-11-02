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

package com.bilibili.socialize.share.core.handler.wx;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bilibili.socialize.share.R;
import com.bilibili.socialize.share.core.BiliShareConfiguration;
import com.bilibili.socialize.share.core.SharePlatformConfig;
import com.bilibili.socialize.share.core.SocializeMedia;
import com.bilibili.socialize.share.core.error.BiliShareStatusCode;
import com.bilibili.socialize.share.core.error.InvalidParamException;
import com.bilibili.socialize.share.core.error.ShareConfigException;
import com.bilibili.socialize.share.core.error.ShareException;
import com.bilibili.socialize.share.core.handler.BaseShareHandler;
import com.bilibili.socialize.share.core.shareparam.ShareImage;
import com.bilibili.socialize.share.core.shareparam.ShareParamAudio;
import com.bilibili.socialize.share.core.shareparam.ShareParamImage;
import com.bilibili.socialize.share.core.shareparam.ShareParamText;
import com.bilibili.socialize.share.core.shareparam.ShareParamVideo;
import com.bilibili.socialize.share.core.shareparam.ShareParamWebPage;
import com.bilibili.socialize.share.core.shareparam.ShareVideo;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.util.Map;

/**
 * @author Jungly
 * @email jungly.ik@gmail.com
 * @since 2015/9/31 18:43
 */
public abstract class BaseWxShareHandler extends BaseShareHandler {
    private static final String TAG = "BShare.wx.handler";

    private static final int IMAGE_MAX = 32 * 1024;
    private static final int IMAGE_WIDTH = 600;
    private static final int IMAGE_HEIGHT = 800;

    private String mAppId;
    private IWXAPI mIWXAPI;

    public BaseWxShareHandler(Activity context, BiliShareConfiguration configuration) {
        super(context, configuration);
    }

    private Map<String, String> getAppConfig() {
        SharePlatformConfig platformConfig = mShareConfiguration.getPlatformConfig();
        return platformConfig.getPlatformDevInfo(SocializeMedia.WEIXIN);
    }

    @Override
    public void checkConfig() throws Exception {
        if (!TextUtils.isEmpty(mAppId)) {
            return;
        }

        Map<String, String> appConfig = getAppConfig();
        if (appConfig == null || TextUtils.isEmpty(mAppId = appConfig.get(SharePlatformConfig.APP_ID))) {
            throw new ShareConfigException("Please set WeChat platform dev info.");
        }
    }

    @Override
    public void init() throws Exception {
        if (mIWXAPI == null) {
            mIWXAPI = WXAPIFactory.createWXAPI(getContext(), mAppId, true);
            if (mIWXAPI.isWXAppInstalled()) {
                mIWXAPI.registerApp(mAppId);
            }
        }

        if (!mIWXAPI.isWXAppInstalled()) {
            String msg = getContext().getString(R.string.bili_share_sdk_not_install_wechat);
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            throw new ShareException(msg, BiliShareStatusCode.ST_CODE_SHARE_ERROR_NOT_INSTALL);
        }
    }

    @Override
    protected void shareText(final ShareParamText params) throws ShareException {
        String text = params.getContent();
        if (TextUtils.isEmpty(text)) {
            throw new InvalidParamException("Content is empty or illegal");
        }

        WXTextObject textObj = new WXTextObject();
        textObj.text = text;

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObj;
        msg.description = text;

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("textshare");
        req.message = msg;
        req.scene = getShareType();
        Log.d(TAG, "start share text");
        shareOnMainThread(req);
    }

    @Override
    protected void shareImage(final ShareParamImage params) throws ShareException {
        mImageHelper.downloadImageIfNeed(params, new Runnable() {
            @Override
            public void run() {
                WXImageObject imgObj = buildWXImageObject(params.getImage());

                WXMediaMessage msg = new WXMediaMessage();
                msg.mediaObject = imgObj;
                msg.thumbData = mImageHelper.buildThumbData(params.getImage());
                msg.title = params.getTitle();
                msg.description = params.getContent();

                final SendMessageToWX.Req req = new SendMessageToWX.Req();
                req.transaction = buildTransaction("imgshareappdata");
                req.message = msg;
                req.scene = getShareType();
                Log.d(TAG, "start share image");
                shareOnMainThread(req);
            }
        });
    }

    protected WXImageObject buildWXImageObject(final ShareImage image) {
        WXImageObject imgObj = new WXImageObject();

        if (image == null) {
            return imgObj;
        }

        if (image.isLocalImage()) {
            imgObj.setImagePath(image.getLocalPath());
        } else if (!image.isUnknowImage()) {
            imgObj.imageData = mImageHelper.buildThumbData(image, IMAGE_MAX, IMAGE_WIDTH, IMAGE_HEIGHT, false);
        }

        return imgObj; 
    }

    @Override
    protected void shareWebPage(final ShareParamWebPage params) throws ShareException {
        if (TextUtils.isEmpty(params.getTargetUrl())) {
            throw new InvalidParamException("Target url is empty or illegal");
        }

        mImageHelper.downloadImageIfNeed(params, new Runnable() {
            @Override
            public void run() {

                WXWebpageObject webpage = new WXWebpageObject();
                webpage.webpageUrl = params.getTargetUrl();

                WXMediaMessage msg = new WXMediaMessage(webpage);
                msg.title = params.getTitle();
                msg.description = params.getContent();
                msg.thumbData = mImageHelper.buildThumbData(params.getThumb());

                SendMessageToWX.Req req = new SendMessageToWX.Req();
                req.transaction = buildTransaction("webpage");
                req.message = msg;
                req.scene = getShareType();
                Log.d(TAG, "start share webpage");
                shareOnMainThread(req);
            }
        });
    }

    @Override
    protected void shareAudio(final ShareParamAudio params) throws ShareException {
        if (TextUtils.isEmpty(params.getTargetUrl()) && TextUtils.isEmpty(params.getAudioUrl())) {
            throw new InvalidParamException("Target url or audio url is empty or illegal");
        }

        mImageHelper.downloadImageIfNeed(params, new Runnable() {
            @Override
            public void run() {
                WXMusicObject audio = new WXMusicObject();

                if (!TextUtils.isEmpty(params.getAudioUrl())) {
                    audio.musicUrl = params.getAudioUrl();
                } else {
                    audio.musicUrl = params.getTargetUrl();
                }

                WXMediaMessage msg = new WXMediaMessage(audio);
                msg.title = params.getTitle();
                msg.description = params.getContent();
                msg.thumbData = mImageHelper.buildThumbData(params.getThumb());

                SendMessageToWX.Req req = new SendMessageToWX.Req();
                req.transaction = buildTransaction("music");
                req.message = msg;
                req.scene = getShareType();
                Log.d(TAG, "start share audio");
                shareOnMainThread(req);
            }
        });
    }

    @Override
    protected void shareVideo(final ShareParamVideo params) throws ShareException {
        if (TextUtils.isEmpty(params.getTargetUrl()) && (params.getVideo() == null || TextUtils.isEmpty(params.getVideo().getVideoH5Url()))) {
            throw new InvalidParamException("Target url or video url is empty or illegal");
        }

        mImageHelper.downloadImageIfNeed(params, new Runnable() {
            @Override
            public void run() {
                WXVideoObject video = new WXVideoObject();
                ShareVideo sv = params.getVideo();
                if (!TextUtils.isEmpty(sv.getVideoH5Url())) {
                    video.videoUrl = sv.getVideoH5Url();
                } else {
                    video.videoUrl = params.getTargetUrl();
                }

                WXMediaMessage msg = new WXMediaMessage(video);
                msg.title = params.getTitle();
                msg.description = params.getContent();
                msg.thumbData = mImageHelper.buildThumbData(params.getThumb());

                SendMessageToWX.Req req = new SendMessageToWX.Req();
                req.transaction = buildTransaction("video");
                req.message = msg;
                req.scene = getShareType();
                Log.d(TAG, "start share video");
                shareOnMainThread(req);
            }
        });
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    private void shareOnMainThread(final SendMessageToWX.Req req) {
        doOnMainThread(new Runnable() {
            @Override
            public void run() {
                postProgressStart();
                boolean result = mIWXAPI.sendReq(req);
                if (!result && getShareListener() != null) {
                    getShareListener().onError(getShareMedia(), BiliShareStatusCode.ST_CODE_SHARE_ERROR_SHARE_FAILED, new ShareException("sendReq failed"));
                }
            }
        });
    }

    @Override
    public void release() {
        Log.d(TAG, "release");
        super.release();
        if (mIWXAPI != null) {
            mIWXAPI.detach();
        }
    }

    @Override
    protected boolean isNeedActivityContext() {
        return true;
    }

    abstract int getShareType();

}
