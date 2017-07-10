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

            /**
             * The user exhausted his / her upload quota.
             * SamsungVR has a quota, normally about 20 videos
             * or live streams per user.
             */
            public static final int STATUS_OUT_OF_UPLOAD_QUOTA = 1;

            /**
             * The server is unable to determine the length of the uploaded file
             */
            public static final int STATUS_BAD_FILE_LENGTH = 3;

            /**
             * File too long. Videos must be < 25 MB long
             */
            public static final int STATUS_FILE_LENGTH_TOO_LONG = 4;

            /**
             * Unexpected stereoscopy type
             */
            public static final int STATUS_INVALID_STEREOSCOPIC_TYPE = 5;

            /**
             * Unexpected audio type
             */
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

            /**
             * The user exhausted his / her upload quota.
             * SamsungVR has a quota, normally about 20 videos
             * or live streams per user.
             */
            public static final int STATUS_OUT_OF_UPLOAD_QUOTA = 1;


            /**
             * Unexpected stereoscopy type
             */
            public static final int STATUS_INVALID_STEREOSCOPIC_TYPE = 5;

            /**
             * Unexpected audio type
             */
            public static final int STATUS_INVALID_AUDIO_TYPE = 6;
        }

    }


    /**
     * Returns the number of upload credits of the user.
     *
     * @return The number of uploads or Live Events the user can create.
     *         0 means the user is out of quota.
     *         -1 means the user has unlimited upload privileges
     */
    Integer getUploadCredits();


    /**
     * Returns the profile picture URL of the user.
     *
     * @return the profile picture URL of the user.
     */
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
                            UserLiveEvent.Source protocol,
                            UserVideo.VideoStereoscopyType videoStereoscopyType,
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
     * Given an live event id, return the corresponding live event
     *
     * @param callback This may be NULL.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the request was queued, false otherwise
     */


    boolean queryLiveEvent(String liveEventId, UserLiveEvent.Result.QueryLiveEvent callback,
                           Handler handler, Object closure);

    public class LocationInfo {

        public final double mLatitude, mLongitude, mAltitude;

        public LocationInfo(double latitude, double longitude, double altitude) {
            mLatitude = latitude;
            mLongitude = longitude;
            mAltitude = altitude;
        }

        public LocationInfo(double latitude, double longitude) {
            this(latitude, longitude, Double.NaN);
        }

    }

    /**
     * Upload a video
     *
     * @param source Ownership of this FD passes onto the SDK from this point onwards till the
     *               results are delivered via callback. The SDK may use a FileChannel to change the
     *               file pointer position.  The SDK will not close the FD. It is the application's
     *               responsibility to close the FD on success, failure, cancel or exception.
     * @param title Short description of the video. This field is shown by all the different players.
     * @param description Detailed description of the video.
     * @param tags A list of strings associated with this video. Passed thru as-is to cloud with no
     *             processing by sdk.
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
        List<String> tags, UserVideo.Permission permission, User.LocationInfo locationInfo,
        Result.UploadVideo callback, Handler handler, Object closure);


    /**
     * Cancels an already started video upload
     *
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the upload was successful, false otherwise
     */

    boolean cancelUploadVideo(Object closure);

}