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

import java.util.Locale;

public interface UserLiveEvent {

    final class Result {

        private Result() {
        }

        /**
         * This callback is used to provide status update for querying the details of a live event.
         */

        public interface QueryLiveEvent extends VR.Result.BaseCallback,
                VR.Result.SuccessWithResultCallback<UserLiveEvent> {

            int INVALID_LIVE_EVENT_ID = 1;
        }

        /**
         * This callback is used to provide status update for deleting a live event.
         */

        public interface DeleteLiveEvent extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

            int INVALID_LIVE_EVENT_ID = 1;
        }


        /**
         * This callback is used to provide status update for finish() live event call.
         */

        public interface Finish extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

            int INVALID_LIVE_EVENT_ID = 1;

        }


        /**
         * This callback is used to provide status update for setPermission() live event call.
         */

        public interface SetPermission extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

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

        /**
         * This callback is used to provide status update for uploading a live event segment.
         */

        public interface UploadSegment extends
                VR.Result.BaseCallback, VR.Result.SuccessCallback,
                VR.Result.ProgressCallback {

            /**
             * The server issued a video id for this upload.  The contents
             * of the video may not have been uploaded yet.
             */
            void onSegmentIdAvailable(Object closure, UserLiveEventSegment segment);

            int INVALID_LIVE_EVENT_ID = 1;

            public static final int STATUS_SEGMENT_NO_MD5_IMPL = 101;
            public static final int STATUS_SEGMENT_UPLOAD_FAILED = 102;
            public static final int STATUS_SEGMENT_END_NOTIFY_FAILED = 103;
        }

    }

    enum State {
        UNKNOWN,
        LIVE_CREATED,
        LIVE_CONNECTED ,
        LIVE_DISCONNECTED,
        LIVE_FINISHED_ARCHIVED,
        LIVE_ACTIVE,
        LIVE_ARCHIVING
    }

    enum FinishAction {
        ARCHIVE,
        DELETE
    }

    enum Source {
        RTMP {
            @Override
            String getStringValue() {
                return "rtmp";
            }
        },
        SEGMENTED_TS {
            @Override
            String getStringValue() {
                return "segmented_ts";
            }
        },
        SEGMENTED_MP4 {
            @Override
            String getStringValue() {
                return "segmented_mp4";
            }
        };

        abstract String getStringValue();

        private static final Source[] sSources = Source.values();

        public static Source fromString(String str) {
            Source result = null;
            Locale locale = Locale.US;
            if (null != str) {
                String strInLower = str.toLowerCase(locale);
                for (int i = sSources.length - 1; i >= 0; i -= 1) {
                    String mineInLower = sSources[i].getStringValue().toLowerCase(locale);
                    if (mineInLower.equals(strInLower)) {
                        result = sSources[i];
                        break;
                    }
                }

            }
            return result;
        }
    }



    /**
     * Queries the the details if the specific live event
     *
     * @param callback This may be NULL.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the query succeeded, false otherwise
     */

    boolean query(Result.QueryLiveEvent callback, Handler handler, Object closure);



    /**
     * Deletes a new live event
     *
     * @param callback This may be NULL..
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


    UserVideo.Permission getPermission();


    /**
     * Sets viewing permissions to the live event.
     *
     * @param permission  The viewing permission granted by the live event creator.
     *                    Through this enum the live even creator can control who is allowed to
     *                    view the live event. The values and their meanings are subject to
     *                    SamsungVR policies.
     * @param callback This may be NULL. SDK does not close the source parcel file descriptor.
     *                 SDK transfers back ownership of the FD only on the callback.  Consider
     *                 providing a Non Null callback so that the application can close the FD.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the live event got deleted, false otherwise
     */
    boolean setPermission(UserVideo.Permission permission, Result.SetPermission callback,
                          Handler handler, Object closure);


    String getId();
    String getTitle();
    String getDescription();
    String getProducerUrl();
    String getViewUrl();

    /**
     * This call returns a Reactions object with the counts of the various user
     * reactions the live stream received since created.
     *
     * @return Reactions
     */
    UserVideo.Reactions getReactions();


    State getState();

    /**
     * The number of viewers currently watching this libe event
     *
     * @return Long the current number of viewers
     */

    Long getViewerCount();

    /**
     * The inbound protocol of the stream. RTMP or SEGMENTED_TS
     *
     * @return Protocol The time the stream finished.
     */

    Source getSource();
    UserVideo.VideoStereoscopyType getVideoStereoscopyType();
    String getThumbnailUrl();

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

    /**
     * Upload a video
     *
     * @param source Ownership of this FD passes onto the SDK from this point onwards till the
     *               results are delivered via callback. The SDK may use a FileChannel to change the
     *               file pointer position.  The SDK will not close the FD. It is the application's
     *               responsibility to close the FD on success, failure, cancel or exception.
     * @param callback This may be NULL. SDK does not close the source parcel file descriptor.
     *                 SDK transfers back ownership of the FD only on the callback.  Consider
     *                 providing a Non Null callback so that the application can close the FD.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the upload was started, false otherwise
     */

    boolean uploadSegmentFromFD(ParcelFileDescriptor source,
        UserLiveEvent.Result.UploadSegment callback, Handler handler, Object closure);

    /**
     * Cancels an already started segment upload
     *
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the upload was successful, false otherwise
     */

    boolean cancelUploadSegment(Object closure);


}
