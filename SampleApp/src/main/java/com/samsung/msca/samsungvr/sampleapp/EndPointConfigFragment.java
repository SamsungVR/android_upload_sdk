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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.samsung.msca.samsungvr.sampleapp.R.id.items;

public class EndPointConfigFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(EndPointConfigFragment.class);
    private static final boolean DEBUG = Util.DEBUG;
    private SharedPreferences mSharedPrefs;

    private ViewGroup mItems;
    private TextView mApiKey, mEndPointUrl, mSSOAppSecret, mSSOAppId, mStatus, mConfigUri;
    private View mAdd, mEditBlock;
    private LayoutInflater mLayoutInflater;

    private static final String PREFS_FILE = BuildConfig.APPLICATION_ID + ".epc_prefs";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mLayoutInflater = LayoutInflater.from(context);
        mSharedPrefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLayoutInflater = null;
    }

    public static final int PICK_CONFIG_URI = 0x1000;

    private static JSONObject sConfig = new JSONObject();

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityRest code: " + requestCode + " result: " + resultCode +
                " data: " + (null == data? "NULL" : data.toString()));
        if (resultCode == Activity.RESULT_OK && null != data) {
            switch (requestCode) {
                case PICK_CONFIG_URI:
                    Uri uri = data.getData();
                    if (null != mConfigUri) {
                        mConfigUri.setText(uri.toString());
                    }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private boolean hasSelectedConfig() {
        return null != sConfig.optString(CFG_SELECTED, null);
    }

    static final String PREFS_CONFIG_URI = "config_uri";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_endpoint_config, null, false);

        mConfigUri = (TextView)result.findViewById(R.id.config_uri);
        mConfigUri.setText(mSharedPrefs.getString(PREFS_CONFIG_URI, ""));

        result.findViewById(R.id.pick_config_uri).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_CONFIG_URI);
            }
        });

        result.findViewById(R.id.load_config_from_uri).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uriStr = mConfigUri.getText().toString();
                if (null == uriStr || uriStr.isEmpty()) {
                    return;
                }
                loadConfigFromUri(Uri.parse(uriStr));
            }
        });

        result.findViewById(R.id.save_config_to_uri).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uriStr = mConfigUri.getText().toString();
                if (null == uriStr || uriStr.isEmpty()) {
                    return;
                }
                saveConfigToUri(Uri.parse(uriStr));
            }
        });

        mAdd = result.findViewById(R.id.add);
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(Mode.ADD, false);
            }
        });

        result.findViewById(R.id.add2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String apiKey = String.valueOf(mApiKey.getText());
                String endPointUrl = String.valueOf(mEndPointUrl.getText());
                String ssoAppId = String.valueOf(mSSOAppId.getText());
                String ssoAppSecret = String.valueOf(mSSOAppSecret.getText());

                if (apiKey.isEmpty()) {
                    mStatus.setText(R.string.api_key_empty);
                    return;
                }
                if (endPointUrl.isEmpty()) {
                    mStatus.setText(R.string.end_point_url_empty);
                    return;
                }

                switch (mMode) {
                    case ADD: {
                        ConfigItem item = new ConfigItem();
                        item.setAll(apiKey, endPointUrl, ssoAppId, ssoAppSecret );
                        item.writeToPrefs(mSharedPrefs, !hasSelectedConfig());
                        mConfigItems.put(item.getId(), item);
                        updateConfigUI();
                    }
                    break;

                    case EDIT: {
                        ConfigItem item = mConfigItems.get(mCurrentEditItemId);
                        if (null != item) {
                            item.setAll(apiKey, endPointUrl, ssoAppId, ssoAppSecret);
                            item.writeToPrefs(mSharedPrefs, !hasSelectedConfig());
                            updateConfigUI();
                        }
                        setMode(Mode.VIEW, false);
                    }
                    break;
                }
            }
        });

        result.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(Mode.VIEW, false);
            }
        });

        mItems = (ViewGroup)result.findViewById(items);
        mEndPointUrl = (TextView)result.findViewById(R.id.end_point_url);
        mApiKey = (TextView)result.findViewById(R.id.api_key);
        mSSOAppId = (TextView)result.findViewById(R.id.sso_app_id);
        mSSOAppSecret = (TextView)result.findViewById(R.id.sso_app_secret);
        mStatus = (TextView)result.findViewById(R.id.status);

        mEditBlock = result.findViewById(R.id.edit_block);

        updateConfigModel();
        setMode(Mode.VIEW, true);
        updateConfigUI();

        return result;
    }

    private void saveConfigToUri(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getActivity().getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            String json = sConfig.toString(2);
            Log.d(TAG, "json uri: " + uri + " config: " + json);
            fileOutputStream.write(json.getBytes());
            fileOutputStream.close();
            pfd.close();
        } catch (Exception ex) {
            Log.e(TAG, "saveConfigToUri", ex);
        }
    }

    private void loadConfigFromUri(Uri uri) {
        JSONObject tempConfig = null;
        try {
            ContentResolver resolver = getActivity().getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();
            tempConfig = new JSONObject(stringBuilder.toString());
        } catch (IOException ex) {
        } catch (JSONException ex) {
            tempConfig = new JSONObject();
        }
        if (null == tempConfig) {
            return;
        }
        sConfig = tempConfig;
        updateConfigUI();
    }

    @Override
    public void onDestroyView() {
        mConfigUri = null;
        mItems.removeAllViews();
        mItems = null;
        mEditBlock = null;
        mApiKey = null;
        mStatus = null;
        mEndPointUrl = null;
        super.onDestroyView();
    }

    static EndPointConfigFragment newFragment() {
        return new EndPointConfigFragment();
    }


    private Mode mMode;
    private final List<View> mViewStack = new ArrayList<>();

    private enum Mode {
        ADD,
        EDIT,
        VIEW
    }

    private void setMode(Mode mode, boolean force) {
        if (!force && mode == mMode) {
            return;
        }

        switch (mode) {
            case VIEW:
                mApiKey.setText("");
                mEndPointUrl.setText("");

                mCurrentEditItemId = null;
                Util.setEnabled(mViewStack, mEditBlock, false);
                Util.setEnabled(mViewStack, mItems, true);
                mAdd.setEnabled(true);
                break;

            case ADD:
                mCurrentEditItemId = null;
                Util.setEnabled(mViewStack, mEditBlock, true);
                Util.setEnabled(mViewStack, mItems, false);
                mAdd.setEnabled(false);
                mEndPointUrl.requestFocus();
                break;

            case EDIT:
                ConfigItem item = mConfigItems.get(mCurrentEditItemId);
                if (null == item) {
                    return;
                }
                mApiKey.setText(item.getApiKey());
                mEndPointUrl.setText(item.getEndPoint());
                mEndPointUrl.requestFocus();
                Util.setEnabled(mViewStack, mEditBlock, true);
                Util.setEnabled(mViewStack, mItems, false);
                mAdd.setEnabled(false);
                break;
        }
        mMode = mode;
    }

    private static String makeAttr(String type, String id) {
        return type + SEPARATOR + id;
    }

    static final String CFG_SELECTED = "selected";
    static final String CFG_ENDPOINT = "endpoint";
    static final String CFG_API_KEY = "apikey";
    static final String CFG_SSO_APP_ID = "ssoappid";
    static final String CFG_SSO_APP_SECRET = "ssoappsecret";


    static ConfigItem getSelectedEndPointConfig(Context context) {
        String selected = sConfig.optString(CFG_SELECTED);
        if (null == selected) {
            return null;
        }
        String endPointUrl = sConfig.optString(makeAttr(CFG_ENDPOINT, selected), null);
        if (null == endPointUrl) {
            return null;
        }
        String apiKey = sConfig.optString(makeAttr(CFG_API_KEY, selected), null);
        if (null == apiKey) {
            return null;
        }
        String ssoAppId = sConfig.optString(makeAttr(CFG_SSO_APP_ID, selected), null);
        String ssoAppSecret = sConfig.optString(makeAttr(CFG_SSO_APP_SECRET, selected), null);

        ConfigItem result = new ConfigItem(selected);
        result.setAll(apiKey, endPointUrl, ssoAppId, ssoAppSecret);
        return result;
    }

    static class ConfigItem {

        private String mEndPoint, mApiKey, mSSOAppId, mSSOAppSecret;
        private final String mId;

        private ConfigItem() {
            mId = UUID.randomUUID().toString();
        }

        private void setAll(String apiKey, String endPoint, String ssoAppId, String ssoAppSecret) {
            setEndPoint(endPoint);
            setApiKey(apiKey);
            setSSOAppId(ssoAppId);
            setSSOAppSecret(ssoAppSecret);
        }

        private ConfigItem(String id) {
            mId = id;
        }

        private void setEndPoint(String endPoint) {
            mEndPoint = endPoint;
        }

        private void setApiKey(String apiKey) {
            mApiKey = apiKey;
        }

        private void setSSOAppId(String ssoAppId) {
            mSSOAppId = ssoAppId;
        }

        private void setSSOAppSecret(String ssoAppSecret) {
            mSSOAppSecret = ssoAppSecret;
        }

        private boolean isValid() {
            return null != mId && null != mApiKey && null != mEndPoint;
        }

        String getEndPoint() {
            return mEndPoint;
        }

        String getApiKey() {
            return mApiKey;
        }

        private String getId() {
            return mId;
        }

        private void writeToPrefs(SharedPreferences prefs, boolean markAsSelected) {
            try {
                sConfig.put(makeAttr(CFG_API_KEY, mId), mApiKey);
                sConfig.put(makeAttr(CFG_ENDPOINT, mId), mEndPoint);
                if (null != mSSOAppId) {
                    sConfig.put(makeAttr(CFG_SSO_APP_ID, mId), mSSOAppId);
                }
                if (null != mSSOAppSecret) {
                    sConfig.put(makeAttr(CFG_SSO_APP_SECRET, mId), mSSOAppSecret);
                }
            } catch (JSONException ex) {
            }
        }

        private void deleteFromPrefs() {
            sConfig.remove(makeAttr(CFG_API_KEY, mId));
            sConfig.remove(makeAttr(CFG_ENDPOINT, mId));
            sConfig.remove(makeAttr(CFG_SSO_APP_ID, mId));
            sConfig.remove(makeAttr(CFG_SSO_APP_SECRET, mId));
        }

        private void setAsSelectedInPrefs(SharedPreferences prefs) {
            try {
                sConfig.put(CFG_SELECTED, mId);
            } catch (JSONException ex) {
            }
        }

        boolean matches(String endPoint, String apiKey) {
            return mApiKey.equals(apiKey) && mEndPoint.equals(endPoint);
        }

    }


    private static final String SEPARATOR = "_";

    private final Map<String, ConfigItem> mConfigItems = new HashMap<>();

    private ConfigItem getConfigItem(String id) {
        ConfigItem result = mConfigItems.get(id);
        if (null == result) {
            result = new ConfigItem(id);
            mConfigItems.put(id, result);
        }
        return result;
    }

    private void updateConfigModel() {
        mConfigItems.clear();
        Iterator<String> keys = sConfig.keys();

        while (keys.hasNext()) {
            String attr = keys.next();
            String parts[] = attr.split(SEPARATOR);
            if (parts.length != 2) {
                continue;
            }
            String id = parts[1];
            if (id.isEmpty()) {
                continue;
            }
            ConfigItem item = getConfigItem(id);
            String type = parts[0];
            String value = sConfig.optString(attr, null);

            if (CFG_API_KEY.equals(type)) {
                item.setApiKey(value);
            } else if (CFG_ENDPOINT.equals(type)) {
                item.setEndPoint(value);
            } else if (CFG_SSO_APP_ID.equals(type)) {
                item.setSSOAppId(value);
            } else if (CFG_SSO_APP_SECRET.equals(type)) {
                item.setSSOAppSecret(value);
            }

        }
    }

    private class ConfigViewHolder implements View.OnClickListener {

        private final ConfigItem mConfigItem;
        private final View mRootView;
        private final TextView mEndPointUrl, mApiKey;
        private final View mDelete, mEdit, mSelect;

        private ConfigViewHolder(ConfigItem configItem) {
            mConfigItem = configItem;

            mRootView = mLayoutInflater.inflate(R.layout.end_point_config_item, null, false);
            if (isSelected(mConfigItem)) {
                mRootView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.selected_config_item_bg));
            }
            mEndPointUrl = (TextView)mRootView.findViewById(R.id.end_point_url);
            mApiKey = (TextView)mRootView.findViewById(R.id.api_key);
            mDelete = mRootView.findViewById(R.id.delete);
            mEdit = mRootView.findViewById(R.id.edit);
            mSelect = mRootView.findViewById(R.id.select);

            mDelete.setOnClickListener(this);
            mSelect.setOnClickListener(this);
            mEdit.setOnClickListener(this);

            mEndPointUrl.setText(mConfigItem.getEndPoint());
            mApiKey.setText(mConfigItem.getApiKey());
        }

        private View getRootView() {
            return mRootView;
        }

        @Override
        public void onClick(View v) {
            if (v == mDelete) {
                deleteConfigItem(mConfigItem);
            } else if (v == mEdit) {
                editConfigItem(mConfigItem);
            } else if (v == mSelect) {
                selectConfigItem(mConfigItem);
            }
        }
    }

    private void deleteConfigItem(ConfigItem item) {
        ConfigItem deleted = mConfigItems.remove(item.getId());
        if (null != deleted) {
            deleted.deleteFromPrefs();
            if (isSelected(deleted)) {
                Iterator<String> keys = sConfig.keys();
                if (keys.hasNext()) {
                    try {
                        sConfig.put(CFG_SELECTED, keys.next());
                    } catch (JSONException ex) {
                    }
                } else {
                    sConfig.remove(CFG_SELECTED);
                }
            }
            updateConfigUI();
        }
    }

    private void selectConfigItem(ConfigItem item) {
        if (mConfigItems.containsKey(item.getId())) {
            item.setAsSelectedInPrefs(mSharedPrefs);
            updateConfigUI();
        }
    }

    private String mCurrentEditItemId;

    private void editConfigItem(ConfigItem item) {
        String id = item.getId();
        if (mConfigItems.containsKey(id)) {
            mCurrentEditItemId = id;
            setMode(Mode.EDIT, false);
        }
    }

    private boolean isSelected(ConfigItem item) {
        return item.getId().equals(sConfig.optString(CFG_SELECTED, null));
    }

    private void updateConfigUI() {
        boolean enabled = mItems.isEnabled();

        mItems.removeAllViews();

        for (ConfigItem item : mConfigItems.values()) {
            mItems.addView(new ConfigViewHolder(item).getRootView());
        }
        Util.setEnabled(mViewStack, mItems, enabled);
    }

}
