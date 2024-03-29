/*
 * Copyright 2014 Peter Heisig
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
public class CreativeCommonsAttributionNoDerivs30Unported extends License {

    @Override
    public String getName() {
        return "Creative Commons Attribution-NoDerivs 3.0 Unported";
    }

    @Override
    public String readSummaryTextFromResources(final Context context) {
        return getContent(context, R.raw.ccand_30_summary);
    }

    @Override
    public String readFullTextFromResources(final Context context) {
        return getContent(context, R.raw.ccand_30_full);
    }

    @Override
    public String getVersion() {
        return "3.0";
    }

    @Override
    public String getUrl() {
        return "http://creativecommons.org/licenses/by-nd/3.0/";
    }

}
