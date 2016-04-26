/*
 * Copyright (c) 2016 Samsung Electronics America
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.samsung.msca.samsungvr.sampleapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executor;

public class SimpleNetworkImageView extends ImageView {

    public SimpleNetworkImageView(Context context) {
        this(context, null);
    }

    public SimpleNetworkImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleNetworkImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        protected Bitmap doInBackground(String ... urls) {
            String url = urls[0];
            Bitmap image = null;
            try (InputStream in = new URL(url).openStream()) {
                image = BitmapFactory.decodeStream(in);
            } catch (IOException e) {
            }
            return image;
        }

        protected void onPostExecute(Bitmap result) {
            if (null != result) {
                setImageBitmap(result);
            }
        }
    }

    private final Executor mExecutor = AsyncTask.SERIAL_EXECUTOR;

    public void setImageUrl(String url) {
        if (null == url) {
            return;
        }
        new DownloadImageTask().executeOnExecutor(mExecutor, url);
    }
}
