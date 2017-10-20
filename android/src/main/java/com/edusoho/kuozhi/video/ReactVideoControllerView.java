package com.edusoho.kuozhi.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.edusoho.videoplayer.view.VideoControllerView;

/**
 * Created by suju on 2017/10/19.
 */

public class ReactVideoControllerView extends VideoControllerView {


    public ReactVideoControllerView(Context context) {
        super(context, (AttributeSet)null);
        this.initView();
    }

    public ReactVideoControllerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        this.initView();
    }

    @Override
    protected void initView() {
        super.initView();
        findViewById(com.edusoho.videoplayer.R.id.ll_controller_tools).setVisibility(INVISIBLE);
    }
}
