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

import org.json.JSONObject;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;


final class Contained {

    private static final boolean DEBUG = Util.DEBUG;
    private static final String TAG = Util.getLogTag(Contained.class);

    interface Spec<CONTAINER> {

        boolean containedOnQueryFromServiceLocked(JSONObject jsonObject);
        void containedOnCreateInServiceLocked();
        void containedOnDeleteFromServiceLocked();
        void containedOnUpdateToServiceLocked();

        Object containedGetIdLocked();
        CONTAINER getContainer();

    }

    abstract static class Type<CONTAINER extends Container.Spec, CONTAINED extends Contained.Spec> {

        abstract CONTAINED newInstance(CONTAINER container, JSONObject jsonObject) throws IllegalArgumentException;
        abstract Object getContainedId(JSONObject jsonObject);

        abstract void notifyCreate(Object callback, CONTAINER container, CONTAINED contained);
        abstract void notifyUpdate(Object callback, CONTAINER container, CONTAINED contained);
        abstract void notifyDelete(Object callback, CONTAINER container, CONTAINED contained);
        abstract void notifyQueried(Object callback, CONTAINER container, CONTAINED contained);
        abstract void notifyListQueried(Object callback, CONTAINER container, List<CONTAINED> contained);

        private final Class<Enum> mPropertiesClass;

        protected Type(Class propertiesClass) {
            mPropertiesClass = propertiesClass;
        }

        Class<Enum> getPropertiesClass() {
            return mPropertiesClass;
        }

        Enum getEnum(String key) {
            String enumName = getEnumName(key);
            if (null == enumName) {
                return null;
            }
            try {
                return Enum.valueOf(mPropertiesClass, enumName);
            } catch (Exception ex) {
                return null;
            }
        }

        String getEnumName(String key) {
            return key;
        }

        Object validateValue(Enum<?> key, Object newValue) {
            return newValue;
        }
    }


    abstract static class BaseImpl<CONTAINER extends Container.Spec> implements Spec<CONTAINER> {

        private final EnumMap mValues;
        protected final CONTAINER mContainer;
        private final Contained.Spec mSelf;
        private final Contained.Type mType;

        protected BaseImpl(Contained.Type type, Contained.Spec self, CONTAINER container,
                           JSONObject jsonObject) throws IllegalArgumentException {
            mContainer = container;
            Class<Enum> propertiesClass = type.getPropertiesClass();
            mValues = new EnumMap(propertiesClass);
            mSelf = null == self ? this : self;
            mType = type;
            if (null != jsonObject && !processQueryFromServiceLocked(jsonObject)) {
                throw new IllegalArgumentException();
            }
        }

        protected BaseImpl(Contained.Type type, CONTAINER container, JSONObject jsonObject) throws IllegalArgumentException {
            this(type, null, container, jsonObject);
        }

        protected boolean processQueryFromServiceNoLock(JSONObject jsonObject) {
            Iterator<String> keys = jsonObject.keys();
            boolean changed = false;
            while (keys.hasNext()) {
                String key = keys.next();
                if (DEBUG) {
                    Log.d(TAG, "key : " + key);
                }
                Enum eKey = mType.getEnum(key);
                if (null == eKey) {
                    continue;
                }
                Object newValue = jsonObject.opt(key);
                changed |= setNoLock(eKey, newValue);
            }
            return changed;
        }

        protected boolean processQueryFromServiceLocked(JSONObject jsonObject) {
            if (DEBUG) {
                Log.d(TAG, "processQueryFromServiceLocked : " + jsonObject);
            }
            synchronized (mValues) {
                return processQueryFromServiceNoLock(jsonObject);
            }
        }

        private boolean isSameNoLock(Object oldValue, Object newValue) {
            return (oldValue == newValue || null != oldValue && oldValue.equals(newValue) ||
                    null != newValue && newValue.equals(oldValue));
        }

        private boolean deleteNoLock(Enum<?> key) {
            mValues.remove(key);
            return true;
        }

        private boolean addNoLock(Enum<?> key, Object newValue) {
            mValues.put(key, newValue);
            return true;
        }

        private boolean changeNoLock(Enum<?> key, Object oldValue, Object newValue) {
            mValues.put(key, newValue);
            return true;
        }

        protected boolean setNoLock(Enum attr, Object newValue, boolean validate) {
            Object oldValue = mValues.get(attr);
            Object newValueMapped = validate ? mType.validateValue(attr, newValue) : newValue;
            if (isSameNoLock(oldValue, newValueMapped)) {
                return false;
            }
            if (null == newValueMapped) {
                return deleteNoLock(attr);
            }
            if (null == oldValue) {
                return addNoLock(attr, newValueMapped);
            }
            return changeNoLock(attr, oldValue, newValueMapped);
        }

        protected boolean setNoLock(Enum attr, Object newValue) {
            return setNoLock(attr, newValue, true);
        }

        Object getNoLock(Enum attr) {
            return mValues.get(attr);
        }

        Object getLocked(Enum attr) {
            synchronized (mValues) {
                return getNoLock(attr);
            }
        }

        @Override
        public CONTAINER getContainer() {
            return mContainer;
        }
    }

}
