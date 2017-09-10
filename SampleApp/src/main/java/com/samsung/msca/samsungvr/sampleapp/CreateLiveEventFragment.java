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

package com.samsung.msca.samsungvr.sampleapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.UserLiveEvent;
import com.samsung.msca.samsungvr.sdk.UserVideo;
import com.samsung.msca.samsungvr.sdk.VR;

import java.util.ArrayList;
import java.util.List;

public class CreateLiveEventFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(CreateLiveEventFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private TextView mTitle, mDescription, mStatus;
    private Spinner mSource, mVideoStereoscopicType;
    private Button mCreateLiveEvent;
    private Spinner mPermission;


    private User mUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (null != bundle) {
            String userId = bundle.getString(LoggedInFragment.PARAM_USER);
            if (null != userId) {
                mUser = VR.getUserById(userId);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_create_live_event, null, false);

        mTitle = (TextView)result.findViewById(R.id.title);
        mDescription = (TextView)result.findViewById(R.id.description);
        mSource = (Spinner)result.findViewById(R.id.protocol);
        mVideoStereoscopicType = (Spinner)result.findViewById(R.id.video_stereoscopy_type);
        mCreateLiveEvent = (Button)result.findViewById(R.id.createLiveEvent);
        mStatus = (TextView)result.findViewById(R.id.status);
        mCreateLiveEvent = (Button)result.findViewById(R.id.createLiveEvent);

        ArrayAdapter<UserLiveEvent.Source> sourceAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, UserLiveEvent.Source.values());
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSource.setAdapter(sourceAdapter);

        ArrayAdapter<UserVideo.VideoStereoscopyType> videoStereoscopyTypeAdapter =
                new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, UserVideo.VideoStereoscopyType.values());
        videoStereoscopyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mVideoStereoscopicType.setAdapter(videoStereoscopyTypeAdapter);

        mCreateLiveEvent.setOnClickListener(mOnClickListener);


        mPermission = (Spinner)result.findViewById(R.id.permission);

        ArrayAdapter<UserVideo.Permission> permissionAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, UserVideo.Permission.values());
        permissionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPermission.setAdapter(permissionAdapter);



        return result;
    }

    @Override
    public void onDestroyView() {
        mCreateLiveEvent.setOnClickListener(null);
        mTitle = null;
        mDescription = null;
        mSource = null;
        mCreateLiveEvent = null;
        mStatus = null;
        super.onDestroyView();
    }

    private final User.Result.CreateLiveEvent mCallback = new User.Result.CreateLiveEvent() {

        @Override
        public void onSuccess(Object closure, UserLiveEvent event) {
            if (DEBUG) {
                Log.d(TAG, "onSuccess event: " + event);
            }
            if (hasValidViews()) {
                mStatus.setText(R.string.success);
            }
        }

        @Override
        public void onFailure(Object closure, int status) {
            if (DEBUG) {
                Log.d(TAG, "onError status: " + status);
            }
            if (hasValidViews()) {
                Resources res = getResources();

                String text = String.format(res.getString(R.string.failure_with_status), status);
                if (status == User.Result.CreateLiveEvent.STATUS_OUT_OF_UPLOAD_QUOTA) {
                    text = "Out of upload quota";
                }
                mStatus.setText(text);
            }
        }

        @Override
        public void onCancelled(Object closure) {
            if (DEBUG) {
                Log.d(TAG, "onCancelled");
            }
        }

        @Override
        public void onException(Object closure, Exception ex) {
            Resources res = getResources();
            String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
            if (hasValidViews()) {
                mStatus.setText(text);
            }
        }
    };

    View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (null == mUser) {
                return;
            }

            mStatus.setText(R.string.in_progress);

            UserVideo.G360CameraVROT vrot = new UserVideo.G360CameraVROT(0.1f, 2.3f, 4.5f, 123456789 );

            float[] gain_r  = {1.0f, 0.325752f, -2.372290f, 12.792780f, -20.940200f, 12.136410f, 0.0f};
            float[] gain_g  = {1.0f, 0.280574f, -1.850000f, 10.858280f, -18.077500f, 10.665830f, 0.0f};
            float[] gain_b  = {1.0f, 0.210250f, -1.047330f, 7.959997f, -13.944500f, 8.640565f, 0.0f};
            float[] degamma = {0.0f, 0.268828450792414f,  0.32958420075989f, -1.2789144793969f,
                    4.0301079672571f,   -5.3481567478302f,   2.9985506084177f};
            float[] gamma   = {0.0f, 3.57462357368785f,  -5.36290067934169f,  1.56053996823903f,
                    5.98170159661094f,  -7.35750654299044f,  2.60354208379431f};
            UserVideo.G360CameraOPAI opai0 = new UserVideo.G360CameraOPAI(gain_r, gain_g, gain_b, degamma, gamma);
            UserVideo.G360CameraOPAI opai1 = new UserVideo.G360CameraOPAI(gain_r, gain_g, gain_b, degamma, gamma);
            UserVideo.G360CameraOPAI opai[] = {opai0, opai1};

            float[][] center = { { 1011.45f, 1014.82f   }, { 1024.5f, 1028.96f  }};
            float[][] affine = { { 0.996647f, 0.0f, 0.0f }, { 0.997043f, 0.0f, 0.0f }};
            UserVideo.G360CameraOPAX opax = new UserVideo.G360CameraOPAX( center, affine);
            UserVideo.CameraMetadata cameraMetadata = new UserVideo.G360CameraMetadata(vrot, opai, opax);

            List<String> tags = new ArrayList<String>();
            tags.add("Testing");
            tags.add("SDK");

            UserVideo.LocationInfo locationInfo = null;



            LocationManager locationManager =
                    (LocationManager) CreateLiveEventFragment.super.getContext().
                            getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!enabled) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }

            Location location = null;
            try {
                location = locationManager.getLastKnownLocation(provider);
            }
            catch (SecurityException eee) {

            }

            if (location != null) {
                System.out.println("Provider " + provider + " has been selected.");
                locationInfo = new UserVideo.LocationInfo(
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getAltitude());
            }


            mUser.createLiveEvent(
                    mTitle.getText().toString(),
                    mDescription.getText().toString(),
                    (UserVideo.Permission)mPermission.getSelectedItem(),
                    (UserLiveEvent.Source) mSource.getSelectedItem(),
                    (UserVideo.VideoStereoscopyType) mVideoStereoscopicType.getSelectedItem(),
                    tags,
                    cameraMetadata,
                    locationInfo,
                    // stabilize, NOT SUPPORTED YET
                    mCallback, null, null);
        }


    };

    static CreateLiveEventFragment newFragment() {
        return new CreateLiveEventFragment();
    }

}
