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

import org.json.JSONObject;

import java.util.List;

final class ContainedContainer {

    abstract static class BaseImpl<CONTAINER extends Container.Spec, OBSERVER> extends Observable.BaseImpl<OBSERVER>
            implements Contained.Spec<CONTAINER>, Container.Spec {

        protected final Contained.BaseImpl<CONTAINER> mContainedImpl;
        protected final Container.BaseImpl mContainerImpl;

        protected BaseImpl(Contained.Type type, CONTAINER container, JSONObject jsonObject) {

            mContainedImpl = new Contained.BaseImpl(type, BaseImpl.this, container, jsonObject) {

                @Override
                public boolean containedOnQueryFromServiceLocked(JSONObject jsonObject) {
                    throw new RuntimeException();
                }

                @Override
                public void containedOnCreateInServiceLocked() {
                    throw new RuntimeException();
                }

                @Override
                public void containedOnDeleteFromServiceLocked() {
                    throw new RuntimeException();
                }

                @Override
                public void containedOnUpdateToServiceLocked() {
                    throw new RuntimeException();
                }

                @Override
                public Object containedGetIdLocked() {
                    throw new RuntimeException();
                }

            };

            mContainerImpl = new Container.BaseImpl(BaseImpl.this) {

                @Override
                public  <CONTAINED extends Contained.Spec> List<CONTAINED> containerOnQueryListOfContainedFromServiceLocked(Contained.Type type, JSONObject jsonObject) {
                    throw new RuntimeException();
                }

                @Override
                public <CONTAINED extends Contained.Spec> CONTAINED containerOnQueryOfContainedFromServiceLocked(Contained.Type type, CONTAINED contained, JSONObject jsonObject) {
                    throw new RuntimeException();
                }

                @Override
                public <CONTAINED extends Contained.Spec> CONTAINED containerOnCreateOfContainedInServiceLocked(Contained.Type type, JSONObject jsonObject) {
                    throw new RuntimeException();
                }

                @Override
                public <CONTAINED extends Contained.Spec> CONTAINED containerOnUpdateOfContainedToServiceLocked(Contained.Type type, CONTAINED contained) {
                    throw new RuntimeException();
                }

                @Override
                public <CONTAINED extends Contained.Spec> CONTAINED containerOnDeleteOfContainedFromServiceLocked(Contained.Type type, CONTAINED contained) {
                    throw new RuntimeException();
                }

            };

        }

        @Override
        public CONTAINER getContainer() {
            return mContainedImpl.getContainer();
        }

        Container.BaseImpl getContainerImpl() {
            return mContainerImpl;
        }

        Contained.BaseImpl getContainedImpl() {
            return mContainedImpl;
        }

        protected boolean setNoLock(Enum attr, Object newValue) {
            return mContainedImpl.setNoLock(attr, newValue);
        }

        Object getNoLock(Enum attr) {
            return mContainedImpl.getNoLock(attr);
        }

        Object getLocked(Enum attr) {
            return mContainedImpl.getLocked(attr);
        }

        protected boolean processQueryFromServiceNoLock(JSONObject jsonObject) {
            return mContainedImpl.processQueryFromServiceLocked(jsonObject);
        }

        protected boolean processQueryFromServiceLocked(JSONObject jsonObject) {
            return mContainedImpl.processQueryFromServiceLocked(jsonObject);
        }

        protected void registerType(Contained.Type type, boolean rememberContained) {
            mContainerImpl.registerType(type, rememberContained);
        }

    }



}
