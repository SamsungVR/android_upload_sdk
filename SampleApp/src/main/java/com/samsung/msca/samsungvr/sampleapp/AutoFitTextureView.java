/**
 * Created by venky on 1/19/17.
 */

/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsung.msca.samsungvr.sampleapp;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitTextureView extends TextureView {

    private static final boolean DEBUG = Util.DEBUG;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Size mSize = null;

    public void setSizes(Size[] allowedSizes) {
        Size result = null;

        if (null == allowedSizes) {
            result = null;
        } else {
            int len = allowedSizes.length;
            for (int i = 0; i < len; i += 1) {
                Size size = allowedSizes[i];
                if (DEBUG) {
                    Log.d(TAG, "Size at " + i + " = " + size);
                }
                if (null == result) {
                    result = size;
                } else {
                    if ((size.getHeight() * size.getWidth()) >
                            result.getHeight() * result.getWidth()) {
                        result = size;
                    }
                }
            }
        }
        Log.d(TAG, "Final size: " + result + " prev size: " + mSize);
        if (mSize == result) {
            return;
        }
        if (mSize == result || null != mSize && mSize.equals(result) || null != result && result.equals(mSize)) {
            return;
        }
        mSize = result;
        requestLayout();
    }

    static final String TAG = Util.getLogTag(AutoFitTextureView.class);

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.d(TAG, "Provided size: " + width + "x" + height);
        if (null == mSize) {
            setMeasuredDimension(width, height);
        } else {
            setMeasuredDimension(mSize.getWidth(), mSize.getHeight());
        }
        Log.d(TAG, "Measured size: " + getMeasuredWidth() + "x" + getMeasuredHeight());
    }

    private Surface mSurface = null;

    public void destroy() {
        SurfaceTexture texture = getSurfaceTexture();
        texture.release();
    }

    public Surface getSurface() {
        if (null == mSurface) {
            SurfaceTexture texture = getSurfaceTexture();
            mSurface = new Surface(texture);
        }
        return mSurface;
    }

}