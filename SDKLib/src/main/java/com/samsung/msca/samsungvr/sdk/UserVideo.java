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

import android.util.Log;

import java.util.Locale;

public abstract class UserVideo {

    UserVideo() {
    }

    private static final String TAG = Util.getLogTag(UserVideo.class);
    private static final boolean DEBUG = Util.DEBUG;

    /**
     * Given a permission string, return the corresponding enum value. This method
     * does a lower case compare using the en-US Locale.
     *
     * @param perm The permission string. Must be one of private, public, unlisted, vr only, web only
     * @return A matching value from the Permission Enumeration or NULL for no match
     */

    public static Permission permissionFromString(String perm) {
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

    private static final Permission[] sPermissions = Permission.values();

    public enum Permission {

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
    }

}
