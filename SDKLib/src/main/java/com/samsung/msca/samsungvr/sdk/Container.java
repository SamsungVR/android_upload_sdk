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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Container {

    interface Spec {
        <CONTAINED extends Contained.Spec> List<CONTAINED> containerOnQueryListOfContainedFromServiceLocked(Contained.Type type, JSONObject jsonObject);
        <CONTAINED extends Contained.Spec> boolean containerOnQueryOfContainedFromServiceLocked(Contained.Type type, CONTAINED contained, JSONObject jsonObject);
        <CONTAINED extends Contained.Spec> CONTAINED containerOnCreateOfContainedInServiceLocked(Contained.Type type, JSONObject jsonObject);
        <CONTAINED extends Contained.Spec> CONTAINED containerOnUpdateOfContainedToServiceLocked(Contained.Type type, CONTAINED contained);
        <CONTAINED extends Contained.Spec> CONTAINED containerOnDeleteOfContainedFromServiceLocked(Contained.Type type, CONTAINED contained);
    }

    abstract static class BaseImpl extends Observable.BaseImpl implements Container.Spec {

        private static class TypeData<CONTAINED extends Contained.Spec> {
            Map<Object, CONTAINED> mContainedItems;
            boolean mRememberContained;

            private void setRememberContained(boolean rememberContained) {
                if (rememberContained == mRememberContained) {
                    return;
                }
                mRememberContained = rememberContained;
                if (mRememberContained) {
                    mContainedItems = new HashMap<>();
                } else {
                    mContainedItems = null;
                }
            }
        }

        private final Map<Contained.Type, TypeData<?>> mContainedMap = new HashMap<>();
        private final Container.Spec mSelf;

        protected BaseImpl(Container.Spec self) {
            mSelf = null == self ? this : self;
        }

        protected BaseImpl() {
            this(null);
        }

        private <CONTAINED extends Contained.Spec> TypeData<CONTAINED>
                updateTypeNoLock(Contained.Type type, boolean rememberContained) {
            TypeData typeData = mContainedMap.get(type);
            if (null == typeData) {
                typeData = new TypeData();
                mContainedMap.put(type, typeData);
            }
            typeData.setRememberContained(rememberContained);
            return typeData;
        }

        private <CONTAINED extends Contained.Spec> TypeData<CONTAINED>
                updateTypeLocked(Contained.Type type, boolean rememberContained) {
            synchronized (mContainedMap) {
                return updateTypeNoLock(type, rememberContained);
            }
        }

        protected void registerType(Contained.Type type, boolean rememberContained) {
            updateTypeLocked(type, rememberContained);
        }

        private <CONTAINED extends Contained.Spec> TypeData<CONTAINED>
            getContainedTypeDataNoLock(Contained.Type type) {
            TypeData<CONTAINED> typeData = (TypeData<CONTAINED>)mContainedMap.get(type);
            if (null == typeData) {
                typeData = updateTypeNoLock(type, false);
            }
            return typeData;
        }

        private <CONTAINED extends Contained.Spec> TypeData<CONTAINED>
            getContainedTypeDataLocked(Contained.Type type) {
            synchronized (mContainedMap) {
                return getContainedTypeDataNoLock(type);
            }
        }

        /*
         * Contained List
         */

        private class NotifyListQueried<CONTAINED extends Contained.Spec> implements Observable.IterationObserver<Object> {

            @Override
            public boolean onIterate(final Observable.Block<Object> block, Object... closure) {
                final Contained.Type type = (Contained.Type)closure[0];
                final List<CONTAINED> containedList = (List<CONTAINED>)closure[1];
                final Container.Spec container = mSelf;

                block.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (hasObserver(block.mCallback)) {
                            type.notifyListQueried(block.mCallback, container, containedList);
                        }
                    }
                });
                return true;

            }
        }

        private final NotifyListQueried<?> mNotifyListQueried = new NotifyListQueried<>();

        private <CONTAINED extends Contained.Spec> List<CONTAINED> onQueryListOfContainedFromServiceNoLock(
            Contained.Type type, TypeData<CONTAINED> typeData, JSONArray jsonItems, List<CONTAINED> result) {

            if (null == jsonItems) {
                return null;
            }

            if (null == result) {
                result = new ArrayList<>();
            } else {
                result.clear();
            }

            List<CONTAINED> toRemove = new ArrayList<>();
            Map<Object, CONTAINED> containedItems = typeData.mContainedItems;
            if (null != containedItems) {
                toRemove.addAll(containedItems.values());
            }
            int len = jsonItems.length();
            for (int i = 0; i < len; i += 1) {
                JSONObject jsonObject;
                try {
                    jsonObject = jsonItems.getJSONObject(i);
                } catch (JSONException ex1) {
                    continue;
                }
                Object id = type.getContainedId(jsonObject);
                if (null == id) {
                    continue;
                }
                if (null == containedItems) {
                    CONTAINED contained = newInstance(type, jsonObject);
                    if (null != contained) {
                        result.add(contained);
                    }
                } else {
                    CONTAINED contained = containedItems.get(id);
                    if (null == contained) {
                        contained = newInstance(type, jsonObject);
                        if (null == contained) {
                            continue;
                        }
                        containedItems.put(id, contained);
                    } else {
                        contained.containedOnQueryFromServiceLocked(jsonObject);
                        toRemove.remove(contained);
                    }

                }
            }
            if (null != containedItems) {
                for (CONTAINED contained : toRemove) {
                    CONTAINED removedItem = containedItems.remove(contained.containedGetIdLocked());
                    removedItem.containedOnDeleteFromServiceLocked();
                }
                result.addAll(containedItems.values());
            }
            result = Collections.unmodifiableList(result);
            iterate(mNotifyListQueried, type, result);
            return result;
        }

        protected <CONTAINED extends Contained.Spec> List<CONTAINED> processQueryListOfContainedFromServiceLocked(
                Contained.Type type, JSONArray jsonItems, List<CONTAINED> result) {
            TypeData<CONTAINED> typeData = getContainedTypeDataLocked(type);
            synchronized (typeData) {
                return onQueryListOfContainedFromServiceNoLock(type, typeData, jsonItems, result);
            }
        }

        private <CONTAINED extends Contained.Spec> CONTAINED getContainedByIdNoLock(Contained.Type type,
            TypeData<CONTAINED> typeData, Object id) {
            Map<Object, CONTAINED> containedItems = typeData.mContainedItems;
            if (null == containedItems) {
                return null;
            }
            CONTAINED contained = containedItems.get(id);
            return contained;
        }

        protected <CONTAINED extends Contained.Spec> CONTAINED getContainedByIdLocked(Contained.Type type, Object id) {
            TypeData<CONTAINED> typeData = getContainedTypeDataLocked(type);
            synchronized (typeData) {
                return getContainedByIdNoLock(type, typeData, id);
            }
        }

        /*
         * Contained Delete
         */

        private class NotifyDeleted<CONTAINED extends Contained.Spec> implements Observable.IterationObserver<Object> {

            @Override
            public boolean onIterate(final Observable.Block<Object> block, Object... closure) {
                final Contained.Type type = (Contained.Type)closure[0];
                final CONTAINED contained = (CONTAINED)closure[1];
                final Container.Spec container = mSelf;

                block.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (hasObserver(block.mCallback)) {
                            type.notifyDelete(block.mCallback, container, contained);
                        }
                    }
                });
                return true;

            }
        }

        private final NotifyDeleted<?> mNotifyDeleted = new NotifyDeleted<>();

        private <CONTAINED extends Contained.Spec> CONTAINED onDeleteOfContainedFromServiceNoLock (
                Contained.Type type, TypeData typeData, CONTAINED contained) {
            Map<Object, CONTAINED> containedItems = typeData.mContainedItems;
            if (null != containedItems) {
                contained = containedItems.remove(contained.containedGetIdLocked());
            }
            if (null != contained) {
                contained.containedOnDeleteFromServiceLocked();
                iterate(mNotifyDeleted, type, contained);
            }
            return contained;
        }

        protected <CONTAINED extends Contained.Spec> CONTAINED processDeleteOfContainedFromServiceLocked(
                Contained.Type type, CONTAINED contained) {
            TypeData<CONTAINED> typeData = getContainedTypeDataLocked(type);
            synchronized (typeData) {
                return onDeleteOfContainedFromServiceNoLock(type, typeData, contained);
            }
        }

        /*
         * Contained Create
         */

        private <CONTAINED extends Contained.Spec> CONTAINED newInstance(Contained.Type type, JSONObject jsonObject) {
            CONTAINED result = null;
            try {
                result = (CONTAINED)type.newInstance(mSelf, jsonObject);
            } catch (IllegalArgumentException ex) {
                result = null;
            }
            if (null != result) {
                result.containedOnCreateInServiceLocked();
                iterate(mNotifyCreated, type, result);
            }
            return result;
        }

        private class NotifyCreated<CONTAINED extends Contained.Spec> implements Observable.IterationObserver<Object> {

            @Override
            public boolean onIterate(final Observable.Block<Object> block, Object... closure) {
                final Contained.Type type = (Contained.Type)closure[0];
                final CONTAINED contained = (CONTAINED)closure[1];
                final Container.Spec container = mSelf;

                block.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (hasObserver(block.mCallback)) {
                            type.notifyCreate(block.mCallback, container, contained);
                        }
                    }
                });
                return true;

            }
        }

        private final NotifyCreated<?> mNotifyCreated = new NotifyCreated<>();

        private <CONTAINED extends Contained.Spec> CONTAINED onCreateOfContainedInServiceNoLock(
                Contained.Type type, TypeData<CONTAINED> typeData, JSONObject jsonObject,
                boolean updateIfExists) {

            Object id = type.getContainedId(jsonObject);
            if (null == id) {
                return null;
            }
            Map<Object, CONTAINED> containedItems = typeData.mContainedItems;
            if (null != containedItems) {
                CONTAINED contained = containedItems.get(id);
                if (null != contained) {
                    if (!updateIfExists) {
                        return null;
                    }
                    contained.containedOnQueryFromServiceLocked(jsonObject);
                    return contained;
                }
            }
            CONTAINED contained = newInstance(type, jsonObject);
            if (null != contained && null != containedItems) {
                containedItems.put(id, contained);
            }
            return contained;
        }

        protected <CONTAINED extends Contained.Spec> CONTAINED processCreateOfContainedInServiceLocked(
                Contained.Type type, JSONObject jsonObject, boolean updateIfExists) {
            TypeData<CONTAINED> typeData = getContainedTypeDataLocked(type);
            synchronized (typeData) {
                return onCreateOfContainedInServiceNoLock(type, typeData, jsonObject, updateIfExists);
            }
        }

        /*
         * Contained Update
         */

        private class NotifyUpdated<CONTAINED extends Contained.Spec> implements Observable.IterationObserver<Object> {

            @Override
            public boolean onIterate(final Observable.Block<Object> block, Object... closure) {
                final Contained.Type type = (Contained.Type)closure[0];
                final CONTAINED contained = (CONTAINED)closure[1];
                final Container.Spec container = mSelf;

                block.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (hasObserver(block.mCallback)) {
                            type.notifyUpdate(block.mCallback, container, contained);
                        }
                    }
                });
                return true;

            }
        }

        private final NotifyUpdated<?> mNotifyUpdated = new NotifyUpdated<>();

        private <CONTAINED extends Contained.Spec> CONTAINED onUpdateOfContainedToServiceNoLock(
                Contained.Type type, TypeData<CONTAINED> typeData, CONTAINED contained) {
            Map<Object, CONTAINED> containedItems = typeData.mContainedItems;
            if (null != containedItems) {
                contained = containedItems.get(contained.containedGetIdLocked());
            }
            if (null != contained) {
                contained.containedOnUpdateToServiceLocked();
                iterate(mNotifyUpdated, type, contained);
            }
            return contained;
        }

        protected <CONTAINED extends Contained.Spec> CONTAINED processUpdateOfContainedToServiceLocked(
                Contained.Type type, CONTAINED contained) {
            TypeData<CONTAINED> typeData = getContainedTypeDataLocked(type);

            synchronized (typeData) {
                return onUpdateOfContainedToServiceNoLock(type, typeData, contained);
            }
        }

        /*
         * Contained Query
         */

        private class NotifyQueried<CONTAINED extends Contained.Spec> implements Observable.IterationObserver<Object> {

            @Override
            public boolean onIterate(final Observable.Block<Object> block, Object... closure) {
                final Contained.Type type = (Contained.Type)closure[0];
                final CONTAINED contained = (CONTAINED)closure[1];
                final Container.Spec container = mSelf;

                block.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (hasObserver(block.mCallback)) {
                            type.notifyQueried(block.mCallback, container, contained);
                        }
                    }
                });
                return true;

            }
        }

        private final NotifyQueried<?> mNotifyQueried = new NotifyQueried<>();

        private <CONTAINED extends Contained.Spec> boolean onQueryOfContainedFromServiceNoLock(
                Contained.Type type, TypeData<CONTAINED> typeData, CONTAINED contained,
                JSONObject jsonObject, boolean addIfMissing) {

            Map<Object, CONTAINED> containedItems = typeData.mContainedItems;
            if (null != containedItems) {
                Object id = contained.containedGetIdLocked();
                CONTAINED existing = containedItems.get(id);
                if (contained != existing) {
                    if (!addIfMissing) {
                        return false;
                    }
                    containedItems.put(id, contained);
                }
            }
            if (contained.containedOnQueryFromServiceLocked(jsonObject)) {
                iterate(mNotifyQueried, type, contained);
                return true;
            }
            return false;
        }

        protected <CONTAINED extends Contained.Spec> boolean processQueryOfContainedFromServiceLocked(
                Contained.Type type, CONTAINED contained, JSONObject jsonObject, boolean addIfMissing) {
            TypeData<CONTAINED> typeData = getContainedTypeDataLocked(type);
            synchronized (typeData) {
                return onQueryOfContainedFromServiceNoLock(type, typeData, contained,
                        jsonObject, addIfMissing);
            }
        }


    }
}
