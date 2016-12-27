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

public interface UserLiveEvent {

    final class Result {

        private Result() {
        }

        /**
         * This callback is used to provide status update for querying the details of a live event.
         */

        public interface QueryLiveEvent extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

            int INVALID_LIVE_EVENT_ID = 1;
        }

        /**
         * This callback is used to provide status update for deleting a live event.
         */

        public interface DeleteLiveEvent extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

            int INVALID_LIVE_EVENT_ID = 1;
        }


        /**
         * This callback is used to provide status update for updating a live event.
         */

        public interface Finish extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

            int INVALID_LIVE_EVENT_ID = 1;

        }
        /**
         * This callback is used to provide status update for uploading a thumbnail for a live event.
         */

        public interface UploadThumbnail extends
                VR.Result.BaseCallback, VR.Result.SuccessCallback,
                VR.Result.ProgressCallback {

            int INVALID_LIVE_EVENT_ID = 1;
        }

    }

    enum State {
        UNKNOWN,
        LIVE_CREATED,
        LIVE_CONNECTED ,
        LIVE_DISCONNECTED,
        LIVE_FINISHED_ARCHIVED
    }

    enum FinishAction {
        ARCHIVE,
        DELETE
    }

    enum Protocol {
        RTMP,
        SEGMENTED_TS
    }

    enum VideoStereoscopyType {
        DEFAULT,
        MONOSCOPIC,
        TOP_BOTTOM_STEREOSCOPIC,
        LEFT_RIGHT_STEREOSCOPIC,
        DUAL_FISHEYE
    }

    /**
     * Queries the the details if the specific live event
     *
     * @param callback This may be NULL. SDK does not close the source parcel file descriptor.
     *                 SDK transfers back ownership of the FD only on the callback.  Consider
     *                 providing a Non Null callback so that the application can close the FD.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the query succeeded, false otherwise
     */

    boolean query(Result.QueryLiveEvent callback, Handler handler, Object closure);



    /**
     * Deletes a new live event
     *
     * @param callback This may be NULL. SDK does not close the source parcel file descriptor.
     *                 SDK transfers back ownership of the FD only on the callback.  Consider
     *                 providing a Non Null callback so that the application can close the FD.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the live event got deleted, false otherwise
     */

    boolean delete(Result.DeleteLiveEvent callback, Handler handler, Object closure);

    /**
     * Sets the state of the live event FINISHED.
     *
     * @param action  The action the server should take after marking the live event finished.
     *                Currently FINISHED_ARCHIVED is the only accepted value, which instructs
     *                the server to make the entire finished live event playble as streaming VOD
     * @param callback This may be NULL. SDK does not close the source parcel file descriptor.
     *                 SDK transfers back ownership of the FD only on the callback.  Consider
     *                 providing a Non Null callback so that the application can close the FD.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the live event got deleted, false otherwise
     */

    boolean finish(FinishAction action, Result.Finish callback, Handler handler, Object closure);

    String getId();
    String getTitle();
    String getDescription();
    String getProducerUrl();
    State getState();

    /**
     * Current number of viewers
     *
     * @return Long the current number of viewers
     */

    Long getViewerCount();

    /**
     * The inbound protocol of the stream. RTMP or SEGMENTED_TS
     *
     * @return Protocol The time the stream finished.
     */

    Protocol getProtocol();
    VideoStereoscopyType getVideoStereoscopyType();
    String getThumbnailUrl();
    UserVideo.Permission getPermission();

    /**
     * The time the stream started,  UTC, seconds since EPOCH, 0 of the stream has not started yet
     *
     * @return Long The time the stream started,
     */
    Long getStartedTime();


    /**
     * The time the stream finished, UTC, seconds since EPOCH0 of the stream has not finished yet
     *
     * @return Long The time the stream finished
     */
    Long getFinishedTime();
    User getUser();


}
