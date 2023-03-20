/*
 * Copyright (C) 2023 The LeafOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.privacy;

import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class AdBlockPreferenceController extends TogglePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String PROP_ADBLOCK_ENABLED = "persist.sys.adblock_enabled";
    private static final String PROP_ADBLOCK_STATUS = "sys.adblock_status";

    private SwitchPreference mPreference;

    public AdBlockPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = (SwitchPreference)preference;
        super.updateState(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return mPreference != null;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public boolean isChecked() {
        return SystemProperties.getBoolean(PROP_ADBLOCK_ENABLED, false);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!isChecked) {
            SystemProperties.set(PROP_ADBLOCK_STATUS, "stopped");
        }
        SystemProperties.set(PROP_ADBLOCK_ENABLED, isChecked ? "true" : "false");
        return true;
    }
}
