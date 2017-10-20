package com.edusoho.kuozhi.video;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.edusoho.videoplayer.helper.LibUpdateHelper;
import com.edusoho.videoplayer.media.IPlayerStateListener;
import com.edusoho.videoplayer.media.IVideoPlayer;
import com.edusoho.videoplayer.media.M3U8Stream;
import com.edusoho.videoplayer.media.VideoPlayerFactory;
import com.edusoho.videoplayer.util.FileUtils;
import com.edusoho.videoplayer.util.M3U8Util;
import com.edusoho.videoplayer.util.VLCInstance;
import com.edusoho.videoplayer.util.VLCOptions;
import com.edusoho.videoplayer.view.VideoControllerView;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.fragment.R;

import org.videolan.libvlc.util.VLCUtil;

import java.util.List;

public class ReactVideoManager extends SimpleViewManager<View> {

    public static final String REACT_CLASS = "RCTVideoPlayer";
    private IVideoPlayer mVideoPlayer;
    private VideoControllerView mVideoControllerView;
    private LibUpdateHelper mLibUpdateHelper;
    private AsyncTask<String, String, String> mDownloadTask;
    private String mPlayUri;
    private ThemedReactContext mContext;
    private View mView;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected View createViewInstance(final ThemedReactContext reactContext) {
        Log.d("setSrc", "createViewInstance");
        mContext = reactContext;
        if(!VLCInstance.testCompatibleCPU(reactContext)) {
            Log.d("VideoPlayerFragment", "no match lib");
            this.mLibUpdateHelper = new LibUpdateHelper(reactContext.getCurrentActivity());
            this.mLibUpdateHelper.update(VLCUtil.getMachineType(), new LibUpdateHelper.LibUpdateListener() {
                public void onInstalled() {
                    showAlert(reactContext.getCurrentActivity(), "解码库更新完成，请重新打开视频播放", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            reactContext.getCurrentActivity().finish();
                        }
                    });
                }

                public void onFail() {
                    reactContext.getCurrentActivity().finish();
                }
            });
        } else {
            //
        }

        mVideoPlayer = VideoPlayerFactory.getInstance().createPlayer(reactContext.getCurrentActivity(), VLCOptions.SUPPORT_RATE);
        this.mVideoPlayer.setDigestKey("");

        mVideoControllerView = new ReactVideoControllerView(reactContext.getCurrentActivity());
        mVideoControllerView.setControllerListener(getControllerListener());
        this.mVideoPlayer.addVideoController(mVideoControllerView);
        this.mVideoPlayer.onStart();

        FrameLayout container = new FrameLayout(reactContext);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(mVideoPlayer.getView(), lp);
        container.addView(mVideoControllerView, lp);
        mView = container;
        return container;
    }

    private void showAlert(Activity activity, String message, DialogInterface.OnClickListener cancelClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("提醒").setMessage(message).setPositiveButton("确认", cancelClickListener).create().show();
    }

    @ReactProp(name = "src")
    public void setSrc(View view, @Nullable String src) {
        Log.d("setSrc", src);
        this.mPlayUri = src;
        parseMediaWrapper(Uri.parse(mPlayUri));
    }

    @ReactProp(name = "portrait")
    public void setPortrait(View view, @Nullable Boolean portrait) {
        if(portrait){
            ReactVideoManager.this.changeScreenLayout(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onChangeScreen", "portrait: " + ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void playWithMedia(String mediaUrl) {
        mVideoPlayer.setMediaSourse(mediaUrl);
        mVideoPlayer.addPlayerStateListener(getIPlayerStateListener());
    }

    private void parseMediaWrapper(final Uri uri) {

        if (TextUtils.isEmpty(uri.getPath())
                || "file".equals(uri.getScheme())
                || !uri.getPath().endsWith(".m3u8")) {
            playWithMedia(uri.toString());
            return;
        }

        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
        mDownloadTask = new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                if (isCancelled()) {
                    return null;
                }
                return M3U8Util.downloadM3U8File(mContext, params[0].toString());
            }

            @Override
            protected void onPostExecute(String url) {
                if (TextUtils.isEmpty(url)) {
                    return;
                }
                List<M3U8Stream> m3U8StreamList = M3U8Util.getM3U8StreamListFromPath(FileUtils.getParent(mPlayUri.toString()), url);
                if (m3U8StreamList == null || m3U8StreamList.isEmpty()) {
                    playWithMedia(uri.toString());
                    return;
                }
                mVideoControllerView.setM3U8StreamList(m3U8StreamList);
                playWithMedia(m3U8StreamList.get(0).getUrl());
            }
        };
        mDownloadTask.execute(uri.toString());
    }

    @Override
    public void onDropViewInstance(View view) {
        super.onDropViewInstance(view);
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }

        mVideoPlayer.pause();
        mVideoPlayer.onStop();
    }

    protected VideoControllerView.ControllerListener getControllerListener() {
        return new VideoControllerView.ControllerListener() {
            @Override
            public void onSeek(int i) {
            }

            @Override
            public void onChangeScreen(int i) {
                ReactVideoManager.this.changeScreenLayout(i);
                mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onChangeScreen", ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onPlayStatusChange(boolean b) {
            }

            @Override
            public void onChangeRate(float v) {
            }

            @Override
            public void onChangePlaySource(String s) {
            }

            @Override
            public void onChangeOverlay(boolean b) {
                Log.d("onChangeOverlay", ""+ b);
                mVideoControllerView.findViewById(R.id.ll_controller_tools).setVisibility(b ? View.VISIBLE : View.INVISIBLE);
            }
        };
    }

    protected IPlayerStateListener getIPlayerStateListener() {
        return new IPlayerStateListener() {
            @Override
            public void onPrepare() {
                mVideoPlayer.start();
            }

            @Override
            public void onFinish() {
            }

            @Override
            public void onPlaying() {
            }
        };
    }

    protected void changeScreenLayout(int orientation) {
        Log.d("changeScreen", "onChangeScreen : " + orientation + " mContext: " + mContext.getResources().getConfiguration().orientation);
        if(orientation != mContext.getResources().getConfiguration().orientation) {
            ViewParent viewParent = mView.getParent();
            if(viewParent != null) {
                ViewGroup parent = (ViewGroup)viewParent;
                ViewGroup.LayoutParams lp = parent.getLayoutParams();
                lp.height = orientation == 2?-1:mContext.getResources().getDimensionPixelOffset(com.edusoho.videoplayer.R.dimen.video_height);
                lp.width = -1;
                Log.d("changeScreen", "height : " + lp.height + " width: " + lp.width);
                parent.setLayoutParams(lp);
                int requestedOrientarion = orientation == 2?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                mContext.getCurrentActivity().setRequestedOrientation(requestedOrientarion);
            }
        }
    }
}
