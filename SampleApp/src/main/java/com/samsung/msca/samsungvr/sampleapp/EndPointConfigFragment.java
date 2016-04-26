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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EndPointConfigFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(EndPointConfigFragment.class);
    private static final boolean DEBUG = Util.DEBUG;
    private SharedPreferences mSharedPrefs;

    private ViewGroup mItems;
    private TextView mApiKey, mEndPointUrl, mStatus;
    private View mAdd, mApply, mCancel, mEditBlock;
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

    private final View.OnClickListener mPerformAdd = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setMode(Mode.ADD, false);
        }
    };

    private final View.OnClickListener mPerformApply = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String apiKey = String.valueOf(mApiKey.getText());
            String endPointUrl = String.valueOf(mEndPointUrl.getText());

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
                        item.setAll(apiKey, endPointUrl);
                        item.writeToPrefs(mSharedPrefs, !hasSelectedConfig());
                        mConfigItems.put(item.getId(), item);
                        updateConfigUI();
                    }
                    break;

                case EDIT: {
                        ConfigItem item = mConfigItems.get(mCurrentEditItemId);
                        if (null != item) {
                            item.setAll(apiKey, endPointUrl);
                            item.writeToPrefs(mSharedPrefs, !hasSelectedConfig());
                            updateConfigUI();
                        }
                        setMode(Mode.VIEW, false);
                    }
                    break;
            }
        }
    };

    private final View.OnClickListener mPerformCancel = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setMode(Mode.VIEW, false);
        }
    };

    private boolean hasSelectedConfig() {
        return null != mSharedPrefs.getString(SELECTED, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_endpoint_config, null, false);

        mItems = (ViewGroup)result.findViewById(R.id.items);
        mEndPointUrl = (TextView)result.findViewById(R.id.end_point_url);
        mApiKey = (TextView)result.findViewById(R.id.api_key);
        mStatus = (TextView)result.findViewById(R.id.status);

        mEditBlock = result.findViewById(R.id.edit_block);
        mAdd = result.findViewById(R.id.add);
        mApply = result.findViewById(R.id.apply);
        mCancel = result.findViewById(R.id.cancel);

        mAdd.setOnClickListener(mPerformAdd);
        mApply.setOnClickListener(mPerformApply);
        mCancel.setOnClickListener(mPerformCancel);

        updateConfigModel();
        setMode(Mode.VIEW, true);
        updateConfigUI();

        return result;
    }

    @Override
    public void onDestroyView() {
        mAdd.setOnClickListener(null);
        mApply.setOnClickListener(null);
        mCancel.setOnClickListener(null);

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

    static ConfigItem getSelectedEndPointConfig(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(EndPointConfigFragment.PREFS_FILE, Context.MODE_PRIVATE);
        String selected = prefs.getString(SELECTED, null);
        if (null == selected) {
            return null;
        }
        String endPointUrl = prefs.getString(makeAttr(PREFIX_ENDPOINT, selected), null);
        if (null == endPointUrl) {
            return null;
        }
        String apiKey = prefs.getString(makeAttr(PREFIX_API_KEY, selected), null);
        if (null == apiKey) {
            return null;
        }
        ConfigItem result = new ConfigItem(selected);
        result.setAll(apiKey, endPointUrl);
        return result;
    }

    static class ConfigItem {

        private String mEndPoint, mApiKey;
        private final String mId;

        private ConfigItem() {
            mId = UUID.randomUUID().toString();
        }

        private void setAll(String apiKey, String endPoint) {
            setEndPoint(endPoint);
            setApiKey(apiKey);
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
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(makeAttr(PREFIX_API_KEY, mId), mApiKey);
            editor.putString(makeAttr(PREFIX_ENDPOINT, mId), mEndPoint);
            if (markAsSelected) {
                editor.putString(SELECTED, mId);
            }
            editor.commit();
        }

        private void deleteFromPrefs(SharedPreferences prefs) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(makeAttr(PREFIX_API_KEY, mId));
            editor.remove(makeAttr(PREFIX_ENDPOINT, mId));
            editor.commit();
        }

        private void setAsSelectedInPrefs(SharedPreferences prefs) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SELECTED, mId);
            editor.commit();
        }

        boolean matches(String endPoint, String apiKey) {
            return mApiKey.equals(apiKey) && mEndPoint.equals(endPoint);
        }

    }

    static final String SELECTED = "selected";
    static final String PREFIX_API_KEY = "apikey";
    static final String PREFIX_ENDPOINT = "endpoint";
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
        Map<String, ?> items = mSharedPrefs.getAll();
        for (String attr : items.keySet()) {
            String value = items.get(attr).toString();
            String parts[] = attr.split(SEPARATOR);
            if (parts.length != 2) {
                continue;
            }
            String id = parts[1];
            if (id.isEmpty()) {
                continue;
            }
            String type = parts[0];
            ConfigItem item = getConfigItem(id);
            if (PREFIX_API_KEY.equals(type)) {
                item.setApiKey(value);
            } else if (PREFIX_ENDPOINT.equals(type)) {
                item.setEndPoint(value);
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
            deleted.deleteFromPrefs(mSharedPrefs);
            if (isSelected(deleted)) {
                SharedPreferences.Editor editor = mSharedPrefs.edit();
                Set<String> keys = mConfigItems.keySet();
                if (keys.isEmpty()) {
                    editor.remove(SELECTED);
                } else {
                    String someId = keys.iterator().next();
                    editor.putString(SELECTED, someId);
                }
                editor.commit();
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
        return item.getId().equals(mSharedPrefs.getString(SELECTED, null));
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
