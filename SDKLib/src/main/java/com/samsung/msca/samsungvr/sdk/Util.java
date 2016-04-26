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

import android.os.ParcelFileDescriptor;

class Util {

    static final boolean DEBUG = BuildConfig.DEBUG;

    static String getLogTag(Object obj) {
        String result = "VRSDK.";
        if (obj instanceof Class<?>) {
            Class<?> cls = (Class<?>)obj;
            result += cls.getSimpleName();
        } else {
            if (null == obj) {
                result += "NULL";
            } else {
                Class<?> cls = obj.getClass();
                result += cls.getSimpleName() + " [" + getHashCode(obj) + "]";
            }
        }
        return result;
    }

    static String getHashCode(Object obj) {
        return "0x" + Integer.toHexString(System.identityHashCode(obj));
    }

    static Enum enumFromString(Class enumClass, String str) {
        if (null == str) {
            return null;
        }
        Enum[] items = ((Class<Enum>)enumClass).getEnumConstants();
        if (null == items) {
            return null;
        }
        for (int i = items.length - 1; i >= 0; i -= 1) {
            Enum item = items[i];
            if (str.equalsIgnoreCase(item.name())) {
                return item;
            }
        }
        return null;
    }

    static boolean checkEquals(Object a, Object b) {
        return (a == b) ||
                ((null != a) && a.equals(b)) ||
                ((null != b) && b.equals(a));
    }

}
