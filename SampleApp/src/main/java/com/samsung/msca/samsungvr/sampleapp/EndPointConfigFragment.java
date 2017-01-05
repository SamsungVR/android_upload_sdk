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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

    private static final String CFG_SELECTED = "selected";

    private static final String CFG_ID = "id";
    static final String CFG_ENDPOINT = "endpoint";
    static final String CFG_API_KEY = "apikey";
    static final String CFG_SSO_APP_ID = "ssoappid";
    static final String CFG_SSO_APP_SECRET = "ssoappsecret";

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != data) {
            int flags = data.getFlags();
            Log.d(TAG, "onActivityRest code: " + requestCode + " result: " + resultCode +
                    " data: " + data.toString() + " flags: " + flags);
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case PICK_CONFIG_URI:
                        Uri uri = data.getData();
                        if (null != mConfigUri) {
                            mConfigUri.setText(uri.toString());
                        }
                        return;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    static final String PREFS_CONFIG_URI = "config_uri";

    private static JSONObject loadConfigFromUri(Context context, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();
            return new JSONObject(stringBuilder.toString());
        } catch (Exception ex) {
            Log.e(TAG, "loadConfigFromUri uri: " + uri, ex);
        }
        return null;
    }

    private static boolean saveConfigToUri(Context context, JSONObject jsonConfig, Uri uri) {
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "w");
            FileDescriptor fd = pfd.getFileDescriptor();
            if (null == fd) {
                return false;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(fd);
            String json = jsonConfig.toString(2);
            Log.d(TAG, "json uri: " + uri + " config: " + json);
            fileOutputStream.write(json.getBytes());
            fileOutputStream.close();
            pfd.close();
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "saveConfigToUri", ex);
        }
        return false;
    }

    private static JSONObject sJsonConfig = null;

    private static JSONObject getJsonConfig(Context context) {
        if (null == sJsonConfig) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
            String uriStr = prefs.getString(PREFS_CONFIG_URI, null);
            if (null != uriStr) {
                sJsonConfig = loadConfigFromUri(context, Uri.parse(uriStr));
            }
            if (null == sJsonConfig) {
                sJsonConfig = new JSONObject();
            }
        }
        return sJsonConfig;
    }

    private static final String JSON_ITEMS = "items";

    static JSONObject getSelectedEndPointConfig(Context context) {
        JSONObject jsonConfig = getJsonConfig(context);
        JSONObject items = jsonConfig .optJSONObject(JSON_ITEMS);
        if (null == items) {
            return null;
        }
        Iterator<String> keys = items.keys();

        if (!keys.hasNext()) {
            jsonConfig.remove(CFG_SELECTED);
            return null;
        }
        String selectedId = jsonConfig.optString(CFG_SELECTED);

        JSONObject selectedItem = null;
        if (null != selectedId) {
            selectedItem = items.optJSONObject(selectedId);
            if (null == selectedItem) {
                selectedId = keys.next();
                jsonConfig.remove(CFG_SELECTED);
            }
        }
        if (null == selectedItem) {
            selectedItem = items.optJSONObject(selectedId);
            if (null == selectedItem) {
                return null;
            }
        }
        try {
            jsonConfig.put(CFG_SELECTED, selectedId);
        } catch (JSONException ex) {
            return null;
        }
        return selectedItem;
    }

    private static JSONObject getJsonItemConfig(Context context, String id) {
        JSONObject jsonConfig = getJsonConfig(context);
        JSONObject items = jsonConfig.optJSONObject(JSON_ITEMS);

        if (null == items) {
            items = new JSONObject();
            try {
                jsonConfig.put(JSON_ITEMS, items);
            } catch (JSONException ex) {
                return null;
            }
        }

        JSONObject result = items.optJSONObject(id);
        if (null == result) {
            result = new JSONObject();
            try {
                items.put(id, result);
                result.put(CFG_ID, id);
            } catch (JSONException ex) {
                return null;
            }
        }
        return result;
    }

    private boolean setAll(JSONObject configItem, String apiKey, String endPointUrl, String ssoAppId,
                        String ssoAppSecret) {
        try {
            configItem.put(CFG_ENDPOINT, endPointUrl);
            configItem.put(CFG_API_KEY, apiKey);
            if (null != ssoAppId) {
                configItem.put(CFG_SSO_APP_ID, ssoAppId);
            }
            if (null != ssoAppSecret) {
                configItem.put(CFG_SSO_APP_SECRET, ssoAppSecret);
            }
            return true;
        } catch (JSONException ex) {

        }
        return false;
    }

    private void saveUriToPrefs(String uriStr) {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.remove(PREFS_CONFIG_URI);
        try {
            getActivity().getContentResolver().takePersistableUriPermission(Uri.parse(uriStr),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            editor.putString(PREFS_CONFIG_URI, uriStr);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to take read/write permission", ex);
        }
        editor.commit();
    }

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
                intent.setFlags(intent.getFlags() | Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(intent, PICK_CONFIG_URI);
            }
        });

        result.findViewById(R.id.load_config_from_uri).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uriStr = mConfigUri.getText().toString();
                if (uriStr.isEmpty()) {
                    return;
                }
                Context context = getActivity();
                JSONObject result = loadConfigFromUri(context, Uri.parse(uriStr));
                if (null == result) {
                    Toast.makeText(context, R.string.failure, Toast.LENGTH_SHORT).show();
                    sJsonConfig = new JSONObject();
                } else {
                    saveUriToPrefs(uriStr);
                    Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show();
                    sJsonConfig = result;
                }
                setMode(Mode.VIEW, false);
                updateConfigUI();
            }
        });

        result.findViewById(R.id.save_config_to_uri).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uriStr = mConfigUri.getText().toString();
                if (uriStr.isEmpty()) {
                    return;
                }
                Context context = getActivity();
                if (saveConfigToUri(context, sJsonConfig, Uri.parse(uriStr))) {
                    saveUriToPrefs(uriStr);
                    Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.failure, Toast.LENGTH_SHORT).show();
                }
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
                        Context context = getActivity();
                        String newId = UUID.randomUUID().toString();
                        JSONObject item = getJsonItemConfig(context, newId);
                        if (null != item && setAll(item, apiKey, endPointUrl, ssoAppId, ssoAppSecret)) {
                            JSONObject jsonConfig = getJsonConfig(context);
                            String selected = jsonConfig.optString(CFG_SELECTED);
                            if (null != selected) {
                                JSONObject items = jsonConfig.optJSONObject(JSON_ITEMS);
                                if (null != items && null == items.optJSONObject(selected)) {
                                    selected = null;
                                }
                            }
                            if (null == selected) {
                                try {
                                    jsonConfig.put(CFG_SELECTED, newId);
                                } catch (JSONException ex) {
                                }
                            }
                            mStatus.setText("");
                            updateConfigUI();
                            Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;

                    case EDIT: {
                        JSONObject item = getJsonItemConfig(getActivity(), mCurrentEditItemId);
                        if (null != item && setAll(item, apiKey, endPointUrl, ssoAppId, ssoAppSecret)) {
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

        setMode(Mode.VIEW, true);
        updateConfigUI();

        return result;
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
                mSSOAppId.setText("");
                mSSOAppSecret.setText("");

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
                JSONObject item = getJsonItemConfig(getActivity(), mCurrentEditItemId);
                if (null == item) {
                    return;
                }
                mApiKey.setText(item.optString(CFG_API_KEY, ""));
                mEndPointUrl.setText(item.optString(CFG_ENDPOINT, ""));
                mSSOAppId.setText(item.optString(CFG_SSO_APP_ID, ""));
                mSSOAppSecret.setText(item.optString(CFG_SSO_APP_SECRET, ""));
                mEndPointUrl.requestFocus();
                Util.setEnabled(mViewStack, mEditBlock, true);
                Util.setEnabled(mViewStack, mItems, false);
                mAdd.setEnabled(false);
                break;
        }
        mMode = mode;
    }

    private class ConfigViewHolder implements View.OnClickListener {

        private final JSONObject mConfigItem;
        private final View mRootView;
        private final TextView mEndPointUrl, mApiKey, mSSOAppSecret, mSSOAppId;
        private final View mDelete, mEdit, mSelect;

        private ConfigViewHolder(JSONObject configItem) {
            mConfigItem = configItem;

            mRootView = mLayoutInflater.inflate(R.layout.end_point_config_item, null, false);
            if (isSelected(mConfigItem)) {
                mRootView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.selected_config_item_bg));
            }
            mEndPointUrl = (TextView)mRootView.findViewById(R.id.end_point_url);
            mApiKey = (TextView)mRootView.findViewById(R.id.api_key);
            mSSOAppId = (TextView)mRootView.findViewById(R.id.sso_app_id);
            mSSOAppSecret = (TextView)mRootView.findViewById(R.id.sso_app_secret);
            mDelete = mRootView.findViewById(R.id.delete);
            mEdit = mRootView.findViewById(R.id.edit);
            mSelect = mRootView.findViewById(R.id.select);

            mDelete.setOnClickListener(this);
            mSelect.setOnClickListener(this);
            mEdit.setOnClickListener(this);

            mEndPointUrl.setText(mConfigItem.optString(CFG_ENDPOINT, ""));
            mApiKey.setText(mConfigItem.optString(CFG_API_KEY, ""));
            mSSOAppSecret.setText(mConfigItem.optString(CFG_SSO_APP_SECRET, ""));
            mSSOAppId.setText(mConfigItem.optString(CFG_SSO_APP_ID, ""));
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

    private boolean isSelected(String itemId) {
        if (null == itemId) {
            return false;
        }
        JSONObject jsonConfig = getJsonConfig(getActivity());
        String selected = jsonConfig.optString(CFG_SELECTED, null);
        return itemId.equals(selected);
    }

    private boolean isSelected(JSONObject item) {
        String itemId = item.optString(CFG_ID, null);
        return isSelected(itemId);
    }

    private void deleteConfigItem(JSONObject item) {
        String id = item.optString(CFG_ID, null);
        if (null == id) {
            return;
        }

        JSONObject jsonConfig = getJsonConfig(getActivity());
        JSONObject items = jsonConfig.optJSONObject(JSON_ITEMS);
        if (null == items) {
            return;
        }
        items.remove(id);
        if (isSelected(id)) {
            Iterator<String> keys = items.keys();
            if (keys.hasNext()) {
                try {
                    jsonConfig.put(CFG_SELECTED, keys.next());
                } catch (JSONException ex) {
                }
            } else {
                jsonConfig.remove(CFG_SELECTED);
            }
        }
        updateConfigUI();
    }

    private void selectConfigItem(JSONObject item) {
        String itemId = item.optString(CFG_ID, null);
        if (null == itemId) {
            return;
        }
        JSONObject jsonConfig = getJsonConfig(getActivity());
        try {
            jsonConfig.put(CFG_SELECTED, itemId);
        } catch (JSONException ex) {
            return;
        }
        updateConfigUI();
    }

    private String mCurrentEditItemId;

    private void editConfigItem(JSONObject item) {
        String itemId = item.optString(CFG_ID, null);
        if (null == itemId) {
            return;
        }
        mCurrentEditItemId = itemId;
        setMode(Mode.EDIT, false);
    }


    private void updateConfigUI() {
        boolean enabled = mItems.isEnabled();

        mItems.removeAllViews();
        JSONObject jsonConfig = getJsonConfig(getActivity());
        JSONObject items = jsonConfig.optJSONObject(JSON_ITEMS);
        if (null == items) {
            return;
        }
        Iterator<String> keys = items.keys();
        while (keys.hasNext()) {
            String id = keys.next();
            JSONObject value = items.optJSONObject(id);
            if (null == value) {
                continue;
            }
            mItems.addView(new ConfigViewHolder(value).getRootView());
        }
        Util.setEnabled(mViewStack, mItems, enabled);
    }

}
