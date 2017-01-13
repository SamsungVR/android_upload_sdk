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

import java.util.List;

public interface User extends Observable.Spec<User.Observer> {

    interface Observer {

        void onUserLiveEventsQueried(User user, List<UserLiveEvent> events);

        void onUserLiveEventCreated(UserLiveEvent userLiveEvent);
        void onUserLiveEventQueried(UserLiveEvent userLiveEvent);
        void onUserLiveEventUpdated(UserLiveEvent userLiveEvent);
        void onUserLiveEventDeleted(UserLiveEvent userLiveEvent);

    }

    /**
     * A grouper class holding result callback interfaces
     */

    final class Result {

        private Result() {
        }

        /**
         * Callback delivering results of queryLiveEvents.  The result object that comes back
         * is of type UserLiveEvent
         */

        public interface QueryLiveEvents extends VR.Result.BaseCallback,
            VR.Result.SuccessWithResultCallback<List<UserLiveEvent>> {
        }

        /**
         * Callback delivering results of uploadVideo. Most status codes
         * are self explanatory. The non-obvious ones are documented.
         */

        public interface UploadVideo extends VR.Result.BaseCallback,
                VR.Result.ProgressCallback, VR.Result.SuccessCallback {

            public static final int STATUS_OUT_OF_UPLOAD_QUOTA = 1;
            public static final int STATUS_BAD_FILE_LENGTH = 3;
            public static final int STATUS_FILE_LENGTH_TOO_LONG = 4;
            public static final int STATUS_INVALID_STEREOSCOPIC_TYPE = 5;
            public static final int STATUS_INVALID_AUDIO_TYPE = 6;

            public static final int STATUS_CHUNK_UPLOAD_FAILED = 101;

            /**
             * An attempt to query the url for the next chunk upload failed.
             */
            public static final int STATUS_SIGNED_URL_QUERY_FAILED = 102;

            /**
             * An attempt to schedule the content upload onto a background
             * thread failed. This may indicate that the system is low on resources.
             * The user could attempt to kill unwanted services/processess and retry
             * the upload operation
             */

            public static final int STATUS_CONTENT_UPLOAD_SCHEDULING_FAILED = 103;

            /**
             * The file has been modified while the upload was in progress. This could
             * be a checksum mismatch or file length mismatch.
             */

            public static final int STATUS_FILE_MODIFIED_AFTER_UPLOAD_REQUEST = 104;

            /**
             * The server issued a video id for this upload.  The contents
             * of the video may not have been uploaded yet.
             */
            void onVideoIdAvailable(Object closure, UserVideo video);

        }

        /**
         * Callback delivering results of createLiveEvent. Failure status codes are self
         * explanatory.
         */

        public interface CreateLiveEvent extends VR.Result.BaseCallback,
                VR.Result.SuccessWithResultCallback<UserLiveEvent> {

            public static final int STATUS_MISSING_STREAMING_PROTOCOL = 1;
            public static final int STATUS_INVALID_STREAMING_PROTOCOL = 2;
            public static final int STATUS_MISSING_DURATION = 3;
            public static final int STATUS_INVALID_DURATION = 4;
            public static final int STATUS_INVALID_STEREOSCOPIC_TYPE = 5;
            public static final int STATUS_INVALID_AUDIO_TYPE = 6;
            public static final int STATUS_MISSING_START_TIME = 7;
            public static final int STATUS_INVALID_START_TIME_FORMAT = 8;
            public static final int STATUS_START_TIME_IN_PAST = 9;
            public static final int STATUS_START_TIME_TOO_FAR_IN_FUTURE = 10;
            public static final int STATUS_MISSING_INGEST_BITRATE = 11;
            public static final int STATUS_INGEST_BITRATE_TOO_LOW = 12;
            public static final int STATUS_INGEST_BITRATE_TOO_HIGH = 13;

        }
    }

    String getProfilePicUrl();
    String getName();
    String getEmail();
    String getUserId();
    String getSessionToken();

    /**
     * Creates a new live event
     *
     * @param title Short description of the live event. This field is shown by all the different players.
     * @param description Detailed description of the live event.
     * @param permission See UserVideo.Permission enum. This is how the privacy settings of the new
     *                   live event can be set
     * @param protocol See UserLiveEvent.Protocol enum. Use this parameter to control how the inbond
     *                 stream should be ingested
     * @param videoStereoscopyType See UserLiveEvent.VideoStereoscopyType enum.
     *                             Describes the video projection used in the inbound stream.
     * @param callback This may be NULL. SDK does not close the source parcel file descriptor.
     *                 SDK transfers back ownership of the FD only on the callback.  Consider
     *                 providing a Non Null callback so that the application can close the FD.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the live event got created, false otherwise
     */

    boolean createLiveEvent(String title,
                            String description,
                            UserVideo.Permission permission,
                            UserLiveEvent.Protocol protocol,
                            UserLiveEvent.VideoStereoscopyType videoStereoscopyType,
                            User.Result.CreateLiveEvent callback,
                            Handler handler,
                            Object closure);


    /**
     * Queries the live events of the user
     *
     * @param callback This may be NULL. SDK does not close the source parcel file descriptor.
     *                 SDK transfers back ownership of the FD only on the callback.  Consider
     *                 providing a Non Null callback so that the application can close the FD.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the upload was schedule, false otherwise
     */


    boolean queryLiveEvents(Result.QueryLiveEvents callback, Handler handler, Object closure);

    /**
     * Upload a video
     *
     * @param source Ownership of this FD passes onto the SDK from this point onwards till the
     *               results are delivered via callback. The SDK may use a FileChannel to change the
     *               file pointer position.  The SDK will not close the FD. It is the application's
     *               responsibility to close the FD on success, failure, cancel or exception.
     * @param title Short description of the video. This field is shown by all the different players.
     * @param description Detailed description of the video.
     * @param permission See UserVideo.Permission enum. This is how the privacy settings of the new
     *                   video can be set
     * @param callback This may be NULL. SDK does not close the source parcel file descriptor.
     *                 SDK transfers back ownership of the FD only on the callback.  Consider
     *                 providing a Non Null callback so that the application can close the FD.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the upload was started, false otherwise
     */

    boolean uploadVideo(ParcelFileDescriptor source, String title, String description,
                UserVideo.Permission permission, Result.UploadVideo callback, Handler handler, Object closure);


    /**
     * Cancels an already started video upload
     *
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the upload was successful, false otherwise
     */

    boolean cancelUploadVideo(Object closure);

}