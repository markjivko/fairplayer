/*
 * Copyright 2013 Philip Schiffer
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package de.psdev.licensesdialog.licenses;

import android.content.Context;

import com.fairplayer.R;

@SuppressWarnings("serial")
public class MozillaPublicLicense20 extends License {

    @Override
    public String getName() {
        return "Mozilla Public License 2.0";
    }

    @Override
    public String readSummaryTextFromResources(final Context context) {
        return getContent(context, R.raw.mpl_20_summary);
    }

    @Override
    public String readFullTextFromResources(final Context context) {
        return getContent(context, R.raw.mpl_20_full);
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public String getUrl() {
        return "http://mozilla.org/MPL/2.0/";
    }

}