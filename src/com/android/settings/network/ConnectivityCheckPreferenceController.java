/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class ConnectivityCheckPreferenceController
        extends BasePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        OnResume {

    // imported defaults from AOSP NetworkStack
    private static final String STANDARD_HTTPS_URL =
            "https://www.google.com/generate_204";
    private static final String STANDARD_HTTP_URL =
            "http://connectivitycheck.gstatic.com/generate_204";
    private static final String STANDARD_FALLBACK_URL =
            "http://www.google.com/gen_204";
    private static final String STANDARD_OTHER_FALLBACK_URLS =
            "http://play.googleapis.com/generate_204";

    // GrapheneOS
    private static final String GRAPHENEOS_CAPTIVE_PORTAL_HTTPS_URL =
            "https://connectivitycheck.grapheneos.network/generate_204";
    private static final String GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL =
            "http://connectivitycheck.grapheneos.network/generate_204";
    private static final String GRAPHENEOS_CAPTIVE_PORTAL_FALLBACK_URL =
            "http://grapheneos.online/gen_204";
    private static final String GRAPHENEOS_CAPTIVE_PORTAL_OTHER_FALLBACK_URL =
            "http://grapheneos.online/generate_204";

    // DivestOS
    private static final String DIVESTOS_HTTPS_URL =
            "https://divestos.org/generate_204";
    private static final String DIVESTOS_HTTP_URL =
            "http://divestos.org/generate_204";

    // openSUSE
    private static final String OPENSUSE_HTTPS_URL =
            "https://conncheck.opensuse.org";
    private static final String OPENSUSE_HTTP_URL =
            "http://conncheck.opensuse.org";

    // Ubuntu
    private static final String UBUNTU_HTTPS_URL =
            "https://connectivity-check.ubuntu.com";
    private static final String UBUNTU_HTTP_URL =
            "http://connectivity-check.ubuntu.com";

    // Amazon Fire OS
    private static final String AMAZON_HTTPS_URL =
            "https://fireoscaptiveportal.com/generate_204";
    private static final String AMAZON_HTTP_URL =
            "http://fireoscaptiveportal.com/generate_204";

    // Microsoft Edge
    private static final String MICROSOFT_HTTP_URL =
            "http://edge-http.microsoft.com/captiveportal/generate_204";

    // Kuketz, https://www.kuketz-blog.de/android-captive-portal-check-204-http-antwort-von-captiveportal-kuketz-de/
    private static final String KUKETZ_HTTPS_URL =
            "https://captiveportal.kuketz.de";
    private static final String KUKETZ_HTTP_URL =
            "http://captiveportal.kuketz.de";

    // Cloudflare
    private static final String CLOUDFLARE_HTTPS_URL =
            "https://cp.cloudflare.com";
    private static final String CLOUDFLARE_HTTP_URL =
            "http://cp.cloudflare.com";

    // Huawei
    private static final String HUAWEI_HTTPS_URL =
            "https://connectivitycheck.platform.hicloud.com/generate_204";
    private static final String HUAWEI_HTTP_URL =
            "http://connectivitycheck.platform.hicloud.com/generate_204";

    // Xiaomi
    private static final String XIAOMI_HTTPS_URL =
            "https://connect.rom.miui.com/generate_204";
    private static final String XIAOMI_HTTP_URL =
            "http://connect.rom.miui.com/generate_204";

    // Keep this in sync! Commented options must be very last!
    private static final int DISABLED_CAPTIVE_PORTAL_INTVAL = 0;
    private static final int AMAZON_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 1;
    private static final int CLOUDFLARE_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 2;
    private static final int DIVESTOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 3;
    private static final int STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 4;
    private static final int HUAWEI_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 5;
    private static final int KUKETZ_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 6;
    private static final int MICROSOFT_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 7;
    private static final int OPENSUSE_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 8;
    private static final int UBUNTU_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 9;
    private static final int XIAOMI_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 10;
    private static final int GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL = 11;

    private static final String KEY_CONNECTIVITY_CHECK_SETTINGS =
            "connectivity_check_settings";

    private ListPreference mConnectivityPreference;

    public ConnectivityCheckPreferenceController(Context context) {
        super(context, KEY_CONNECTIVITY_CHECK_SETTINGS);
    }

    @Override
    public int getAvailabilityStatus() {
        if (isDisabledByAdmin()) {
            return BasePreferenceController.DISABLED_FOR_USER;
        }
        return BasePreferenceController.AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        ListPreference captiveList = new ListPreference(screen.getContext());
        captiveList.setKey(KEY_CONNECTIVITY_CHECK_SETTINGS);
        captiveList.setOrder(30);
        captiveList.setIcon(R.drawable.ic_settings_language);
        captiveList.setTitle(R.string.connectivity_check_title);
        captiveList.setSummary(R.string.connectivity_check_summary);
        captiveList.setEntries(R.array.connectivity_check_entries);
        captiveList.setEntryValues(R.array.connectivity_check_values);

        if (mConnectivityPreference == null) {
            screen.addPreference(captiveList);
            mConnectivityPreference = captiveList;
        }
        super.displayPreference(screen);
        updatePreferenceState();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CONNECTIVITY_CHECK_SETTINGS;
    }

    private void updatePreferenceState() {
        if (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.CAPTIVE_PORTAL_MODE, Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT)
                == Settings.Global.CAPTIVE_PORTAL_MODE_IGNORE) {
            mConnectivityPreference.setValueIndex(DISABLED_CAPTIVE_PORTAL_INTVAL);
            return;
        }

        String pref = Settings.Global.getString(
                mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_HTTP_URL);
        if (STANDARD_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (DIVESTOS_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    DIVESTOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (OPENSUSE_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    OPENSUSE_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (UBUNTU_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    UBUNTU_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (AMAZON_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    AMAZON_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (MICROSOFT_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    MICROSOFT_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (KUKETZ_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    KUKETZ_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (CLOUDFLARE_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    CLOUDFLARE_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (HUAWEI_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    HUAWEI_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        } else if (XIAOMI_HTTP_URL.equals(pref)) {
            mConnectivityPreference.setValueIndex(
                    XIAOMI_CAPTIVE_PORTAL_HTTP_URL_INTVAL);
        }

    }

    @Override
    public void onResume() {
        updatePreferenceState();
        if (mConnectivityPreference != null) {
            setCaptivePortalURLs(
                    mContext.getContentResolver(),
                    Integer.parseInt(mConnectivityPreference.getValue()));
        }
    }

    private void setCaptivePortalURLs(ContentResolver cr, int mode) {
        switch (mode) {
            case STANDARD_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        STANDARD_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        STANDARD_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        STANDARD_FALLBACK_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        STANDARD_OTHER_FALLBACK_URLS);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        GRAPHENEOS_CAPTIVE_PORTAL_FALLBACK_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        GRAPHENEOS_CAPTIVE_PORTAL_OTHER_FALLBACK_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case DIVESTOS_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        DIVESTOS_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        DIVESTOS_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        DIVESTOS_HTTP_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        DIVESTOS_HTTP_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case OPENSUSE_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        OPENSUSE_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        OPENSUSE_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        OPENSUSE_HTTP_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        OPENSUSE_HTTP_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case UBUNTU_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        UBUNTU_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        UBUNTU_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        UBUNTU_HTTP_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        UBUNTU_HTTP_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case AMAZON_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        AMAZON_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        AMAZON_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        AMAZON_HTTP_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        AMAZON_HTTP_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case MICROSOFT_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        MICROSOFT_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        MICROSOFT_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        MICROSOFT_HTTP_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        MICROSOFT_HTTP_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case KUKETZ_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        KUKETZ_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        KUKETZ_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        KUKETZ_HTTP_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        KUKETZ_HTTP_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case CLOUDFLARE_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        CLOUDFLARE_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        CLOUDFLARE_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        CLOUDFLARE_HTTP_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        CLOUDFLARE_HTTP_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case HUAWEI_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        HUAWEI_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        HUAWEI_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        HUAWEI_HTTP_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        HUAWEI_HTTP_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            case XIAOMI_CAPTIVE_PORTAL_HTTP_URL_INTVAL:
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        XIAOMI_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        XIAOMI_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        XIAOMI_HTTP_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        XIAOMI_HTTP_URL);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT);
                break;
            default:
                // Default URLs as placeholder
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                        STANDARD_HTTP_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                        STANDARD_HTTPS_URL);
                Settings.Global.putString(cr, Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                        STANDARD_FALLBACK_URL);
                Settings.Global.putString(
                        cr, Settings.Global.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS,
                        STANDARD_OTHER_FALLBACK_URLS);
                Settings.Global.putInt(cr, Settings.Global.CAPTIVE_PORTAL_MODE,
                        Settings.Global.CAPTIVE_PORTAL_MODE_IGNORE);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (KEY_CONNECTIVITY_CHECK_SETTINGS.equals(key)) {
            setCaptivePortalURLs(mContext.getContentResolver(),
                    Integer.parseInt((String)value));
            return true;
        } else {
            return false;
        }
    }

    private EnforcedAdmin getEnforcedAdmin() {
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_PRIVATE_DNS,
                UserHandle.myUserId());
    }

    private boolean isDisabledByAdmin() { return getEnforcedAdmin() != null; }
}
