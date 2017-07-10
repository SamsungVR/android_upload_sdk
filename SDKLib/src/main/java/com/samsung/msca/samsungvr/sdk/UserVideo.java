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

package com.samsung.msca.samsungvr.sdk;

import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.util.Collection;
import java.util.Locale;

public interface UserVideo {

    enum VideoStereoscopyType {
        DEFAULT,
        MONOSCOPIC,
        TOP_BOTTOM_STEREOSCOPIC,
        LEFT_RIGHT_STEREOSCOPIC,
        DUAL_FISHEYE,
        EXPERIMENTAL
    }


    enum Permission {

        PRIVATE {
            @Override
            String getStringValue() {
                return "Private";
            }
        },
        UNLISTED {
            @Override
            String getStringValue() {
                return "Unlisted";
            }
        },
        PUBLIC {
            @Override
            String getStringValue() {
                return "Public";
            }
        },
        VR_ONLY {
            @Override
            String getStringValue() {
                return "VR Only";
            }
        },
        WEB_ONLY {
            @Override
            String getStringValue() {
                return "Web Only";
            }
        };

        abstract String getStringValue();

        private static final String TAG = Util.getLogTag(UserVideo.class);
        private static final boolean DEBUG = Util.DEBUG;

        private static final Permission[] sPermissions = Permission.values();

        /**
         * Given a permission string, return the corresponding enum value. This method
         * does a lower case compare using the en-US Locale.
         *
         * @param perm The permission string. Must be one of private, public, unlisted, vr only, web only
         * @return A matching value from the Permission Enumeration or NULL for no match
         */

        public static Permission fromString(String perm) {
            Permission result = null;
            Locale locale = Locale.US;
            if (null != perm) {
                String permInLower = perm.toLowerCase(locale);
                for (int i = sPermissions.length - 1; i >= 0; i -= 1) {
                    String mineInLower = sPermissions[i].getStringValue().toLowerCase(locale);
                    if (mineInLower.equals(permInLower)) {
                        result = sPermissions[i];
                        break;
                    }
                }

            }
            if (DEBUG) {
                Log.d(TAG, "permissionFromString str: " + perm + " result: " + result);
            }
            return result;
        }

    }

    public interface Reactions {

        long getScared();//  "scared": 0,
        long getWow();   //  "wow": 0,
        long getSad();   //  "sad": 0,
        long getSick();  //  "sick": 0,
        long getAngry(); //  "angry": 0,
        long getHappy(); //  "happy": 0

    }

    /**
     * Get this video's unique id
     *
     * @return A non null video id
     */

    String getVideoId();

    /*
     * Get this video's tags. The returned collection is read only.
     */

    Collection<String> getTags();

    /**
     * Cancel an ongoing upload. This yields the same result as calling User.cancelVideoUpload()
     *
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if a cancel was scheduled, false if the upload could not be cancelled, which
     * can happen because the upload failed or completed even before the cancel could be reqeusted.
     */

    boolean cancelUpload(Object closure);

    /**
     * Retry a failed upload. The params are similar to those of User.uploadVideo. No check is
     * made to ensure that the parcel file descriptor points to the same file as the failed
     * upload.
     *
     *
     * @return true if a retry was scheduled, false if the upload is already in progress or cannot
     * be retried. An upload cannot be retried if it already completed successfully.
     */

    boolean retryUpload(ParcelFileDescriptor source, User.Result.UploadVideo callback,
                        Handler handler, Object closure);

}
