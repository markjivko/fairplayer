/**
 * Copyright 2016 Mark Jivko https://markjivko.com
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://gnu.org/licenses/gpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Redistributions of files must retain the above copyright notice.
 */
package com.fairplayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.libvlc.MediaPlayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;

public class ActivityEqualizer extends Activity { 
    
    protected ToggleButton mButtonEnable;
    
    protected Button mButtonAction;

    protected Spinner mButtonPresets;

    protected ElementKnob mPreAmpKnob;

    protected LinearLayout mBandsContainers;
    
    protected ElementKnob mVolumeKnob;
    
    protected static MediaPlayer sMediaPlayer = null;
    
    protected static AudioManager sAudioManager = null;
    
    static {
        sMediaPlayer = FpServiceRendering.get(ActivityCommon.getContext()).getFpMediaPlayer().getPlayer();
        sAudioManager = (AudioManager) ActivityCommon.getContext().getSystemService(Context.AUDIO_SERVICE);
    }
    
   
    @SuppressLint("NewApi") 
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // Store this activity
        Presets.mActivity = this;
        
        // Get the preferences
        PreferenceUtils.getPreferences(this);
        
        // Set the layout
        setContentView(R.layout.fp_activity_equalizer);
        
        // Set the page background
        ((LinearLayout) findViewById(R.id.fp_content)).setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_page));
        
        // Get the "enable" button
        mButtonEnable = (ToggleButton) findViewById(R.id.fp_equalizer_button_enable);
        mButtonEnable.setBackground(Theme.Resources.getDrawable(R.drawable.fp_switch));
        mButtonEnable.setTextColor(Theme.Resources.getColor(R.color.fp_color_button));
        
        // Get the "action" button
        mButtonAction = (Button) findViewById(R.id.fp_equalizer_button_action);
        mButtonAction.setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_button));
        mButtonAction.setTextColor(Theme.Resources.getColor(R.color.fp_color_button));
        mButtonAction.setText(getText(R.string.fp_equalizer_label_button_action_save));
        mButtonAction.setVisibility(View.GONE);
        
        // Get the "presets" button
        mButtonPresets = (Spinner) findViewById(R.id.fp_equalizer_button_presets);
        mButtonPresets.setBackground(Theme.Resources.getDrawable(R.drawable.fp_bg_button));
        mButtonPresets.setPopupBackgroundDrawable(Theme.Resources.getDrawable(R.drawable.fp_bg_button));
        
        // Get the bands container
        mBandsContainers = (LinearLayout) findViewById(R.id.fp_equalizer_bands);
        mPreAmpKnob = (ElementKnob) findViewById(R.id.fp_equalizer_preamp);
        mVolumeKnob = (ElementKnob) findViewById(R.id.fp_equalizer_volume);
        
        // Set the label colors
        ((TextView) findViewById(R.id.fp_equalizer_preamp_label)).setTextColor(Theme.Resources.getColor(R.color.fp_color_equalizer_label));
        ((TextView) findViewById(R.id.fp_equalizer_volume_label)).setTextColor(Theme.Resources.getColor(R.color.fp_color_equalizer_label));
        
        // Prepare the ads
        try {
            Ads ads = Ads.getInstance((ActivityCommon) ActivityCommon.getActivity());
            ads.place(Ads.AdPlacement.ENTER_EQUALIZER);
        } catch (Exception e){}
    }
    
    public static class Presets {   	
    	// Preset type
    	public static final int PRESET_DEFAULT = 0;
    	public static final int PRESET_CUSTOM = 1;
    	protected static int type = PRESET_DEFAULT;
    	
    	// ActivityEqualizer
    	public static ActivityEqualizer mActivity;
    	
    	/**
    	 * Refresh the presets dropdown list
    	 */
    	public static void uiRefreshList() {
            Presets.uiRefreshList(-1);
    	}
    	
    	/**
    	 * Refresh the presets dropdown list and set the current element
    	 */
    	public static void uiRefreshList(int position) {
            // Get the presets count
            final int presetsCount = MediaPlayer.Equalizer.getPresetCount();
            
            // Get the custom presets
            Map<Integer, String> customPresets = Presets.getAllCustom();
            
            // Prepare the prests
            final String[] presets = new String[presetsCount + customPresets.size()];
            for (int i = 0; i < presetsCount; ++i) {
                presets[i] = MediaPlayer.Equalizer.getPresetName(i);
            }

            // Get the custom presets
            for (Map.Entry<Integer, String> customPresetPosition : customPresets.entrySet()) {
            	presets[presetsCount + customPresetPosition.getKey()] = customPresetPosition.getValue();
            }
            
            // Presets adapter
            mActivity.mButtonPresets.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.fp_spinner, presets) {
                
            	public View getView(int position, View convertView, ViewGroup parent) {
                    TextView textView = (TextView) super.getView(position, convertView, parent);

                    // Stylize
                    textView.setTextColor(Theme.Resources.getColor(R.color.fp_color_button));
                    textView.setGravity(Gravity.CENTER);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mActivity.getResources().getDimension(R.dimen.fp_text_size_equalizer_buttons));

                    // Set the text
                    textView.setText(mActivity.getText(R.string.fp_equalizer_label_button_preset) + ": " + textView.getText());
                    return textView;
                }
                
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView, parent);
                    ((TextView) v).setTextColor(Theme.Resources.getColor(R.color.fp_color_button));
                    ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_PX, mActivity.getResources().getDimension(R.dimen.fp_text_size_equalizer_buttons));
                    return v;
                }
            });

            // Go to the element
            if (position >= 0 && position < presets.length) {
            	mActivity.mButtonPresets.setSelection(position, true);
            }
    	}
    	
    	/**
    	 * Assume save-to-cache calls come from the user
    	 */
    	public static void saveToCache() {
            saveToCache(true);
    	}
    	
    	/**
    	 * Commit the current adjustments to the Cache
    	 */
    	public static void saveToCache(boolean fromUser) {
            Presets.updateActionButton(fromUser);
    		
            // Equalizer was set
            if (EqualizerUtil.getEqualizer() != null) {
                // Get the preferences editor
                SharedPreferences.Editor editor = PreferenceUtils.edit();
                
                // Store the state
                editor.putBoolean(Constants.Keys.SETTINGS_EQUALIZER_ENABLED, mActivity.mButtonEnable.isChecked());
                
                // Get the band count
                final int bandCount = MediaPlayer.Equalizer.getBandCount();
                
                // Prepare the bands
                final float[] bands = new float[bandCount + 1];
                
                // First item is pre-amplification
                bands[0] = EqualizerUtil.getEqualizer().getPreAmp();
                
                // Store the local values
                for (int i = 0; i < bandCount; ++i) {
                    bands[i + 1] = EqualizerUtil.getEqualizer().getAmp(i);
                }
                
                // Save the bands
                PreferenceUtils.putFloatArray(editor, Constants.Keys.SETTINGS_EQUALIZER_VALUES, bands);
                
                // Save the preset
                editor.putInt(Constants.Keys.SETTINGS_EQUALIZER_PRESET, mActivity.mButtonPresets.getSelectedItemPosition());
                
                // Get the presets count
                int presetsCount = MediaPlayer.Equalizer.getPresetCount();
                int presetPosition = mActivity.mButtonPresets.getSelectedItemPosition();
                
                // Store the preset type
                Presets.type = presetPosition > presetsCount - 1 ? Presets.PRESET_CUSTOM : Presets.PRESET_DEFAULT;
                
                // Save the settings
                editor.commit();
            }
    	}
    	
    	/**
    	 * Set the state flag
    	 */
    	public static void updateActionButton(boolean changed) {
            // Get the presets count
            int presetsCount = MediaPlayer.Equalizer.getPresetCount();
            int presetPosition = mActivity.mButtonPresets.getSelectedItemPosition();
            
            // Store the preset type
            Presets.type = presetPosition > presetsCount - 1 ? Presets.PRESET_CUSTOM : Presets.PRESET_DEFAULT;

                // Set the button visibility
    		mActivity.mButtonAction.setVisibility(!changed && Presets.PRESET_DEFAULT == Presets.type ? View.GONE : View.VISIBLE);
    		
    		// No need to continue
    		if (!changed && Presets.PRESET_DEFAULT == Presets.type) {
                return;
    		}
    		
    		// Set the button text
    		mActivity.mButtonAction.setText(mActivity.getText(Presets.PRESET_CUSTOM == Presets.type ? R.string.fp_equalizer_label_button_action_update : R.string.fp_equalizer_label_button_action_save));
    		
    		// Prepare the dialog
    		LayoutInflater inflater = mActivity.getLayoutInflater();
    		
    		// Prepare the dialog layout
    		View dialogLayout = inflater.inflate(R.layout.fp_activity_equalizer_dialog, null);
    		
    		// Get the dialog builder
    		AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(mActivity);
    		
    		// Set the custom view
    		mDialogBuilder.setView(dialogLayout);
    		
    		// Set the name
    		final String presetNamePrefix = "* ";
    		final EditText presetName = ((EditText) dialogLayout.findViewById(R.id.fp_activity_equalizer_preset_name));
    		
    		// Set the new name
    		presetName.setText(presetNamePrefix + mActivity.mButtonPresets.getSelectedItem().toString().replaceFirst("^\\* ", ""));
    		
    		// Don't allow the user to delete the "* "
    		Selection.setSelection(presetName.getText(), presetNamePrefix.length());
    		presetName.addTextChangedListener(new TextWatcher() {

    	        @Override
    	        public void onTextChanged(CharSequence s, int start, int before, int count) {
    	        }

    	        @Override
    	        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    	        }

    	        @Override
    	        public void afterTextChanged(Editable s) {
    	            if(!s.toString().startsWith(presetNamePrefix)){
    	            	presetName.setText(presetNamePrefix);
    	                Selection.setSelection(presetName.getText(), presetNamePrefix.length());
    	            }
    	        }
    	    });
    		
            // Set the title
            mDialogBuilder.setTitle(mActivity.getText(Presets.PRESET_CUSTOM == Presets.type ? R.string.fp_equalizer_label_button_action_update_title : R.string.fp_equalizer_label_button_action_save_title));
            mDialogBuilder.setCancelable(true);

            // Changed a default preset
            if (changed && Presets.PRESET_DEFAULT == Presets.type) {
            	// Save Default Changed
            	mDialogBuilder.setPositiveButton(R.string.fp_equalizer_label_button_action_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Save Default Changed
                        int position = Presets.addCustom(presetName.getText().toString());
                        int presetsCount = MediaPlayer.Equalizer.getPresetCount();
                        
                        // Refresh the list
                        Presets.uiRefreshList(position + presetsCount);
                        
                        // Inform the user
                        Toast.makeText(mActivity, mActivity.getText(R.string.fp_equalizer_toast_saved), Toast.LENGTH_SHORT).show();
                    }
            	});
            } else {
            	// Save Custom
            	mDialogBuilder.setPositiveButton(R.string.fp_equalizer_label_button_action_update, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get the position
                        int presetsCount = MediaPlayer.Equalizer.getPresetCount();
                        int position = mActivity.mButtonPresets.getSelectedItemPosition() - presetsCount;

                        // Update
                        Presets.updateCustom(position, presetName.getText().toString());

                        // Refresh the list
                        Presets.uiRefreshList(position + presetsCount);
                        
                        // Inform the user
                        Toast.makeText(mActivity, mActivity.getText(R.string.fp_equalizer_toast_updated), Toast.LENGTH_SHORT).show();
                    }

            	});
            	
            	// Delete custom
            	mDialogBuilder.setNeutralButton(R.string.fp_equalizer_label_button_action_delete, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Delete Custom
                        final int presetsCount = MediaPlayer.Equalizer.getPresetCount();
                        Presets.deleteCustom(mActivity.mButtonPresets.getSelectedItemPosition() - presetsCount);
                        
                        // Inform the user
                        Presets.uiRefreshList(); 
                        
                        // Inform the user
                        Toast.makeText(mActivity, mActivity.getText(R.string.fp_equalizer_toast_deleted), Toast.LENGTH_SHORT).show();
                    }

            	});
            }
            
            // Set the cancel button
            mDialogBuilder.setNegativeButton(R.string.fp_equalizer_label_button_action_cancel, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
    		
            // Prepare the dialog
            final AlertDialog mDialog = mDialogBuilder.create();
            
            // Set the click action
            mActivity.mButtonAction.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog.show();
                }
            });
    	}
    	
    	/**
    	 * Load the specified preset
    	 */
    	public static void loadPreset(int position) {
            // Assume the user hasn't changed anything at this point
            Presets.updateActionButton(false);

            // Get the presets count
            final int presetsCount = MediaPlayer.Equalizer.getPresetCount();

            // Load custom preset
            if (position >= presetsCount) {
                // Get the bands
                float[] bands = Presets.getCustom(position - presetsCount);

                // Get the preferences editor
                SharedPreferences.Editor editor = PreferenceUtils.edit();

                // Store the bands
                PreferenceUtils.putFloatArray(editor, Constants.Keys.SETTINGS_EQUALIZER_VALUES, bands);

                try {
                    // Refresh the EQ
                    MediaPlayer.Equalizer equalizer = EqualizerUtil.getEqualizer();

                    // Set the pre-amp
                    equalizer.setPreAmp(bands[0]);

                    // Set the amplification
                    for (int i = 0; i < bands.length - 1; ++i) {
                        equalizer.setAmp(i, bands[i + 1]);
                    }

                    // Set the equalizer
                    EqualizerUtil.setEqualizer(equalizer);
                } catch (Exception e) {}
            } else {
                // Set the appropiate equalizer
                EqualizerUtil.setEqualizer(MediaPlayer.Equalizer.createFromPreset(position));
            }

    	}
    	
    	/**
    	 * Get all the custom presets as ID : Name
    	 */
    	public static Map<Integer, String> getAllCustom() {
            // Prepare the result
            Map<Integer, String> result = new HashMap<Integer, String>();

            // Prepare the preferences
            String jsonString = PreferenceUtils.getString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS, Constants.Defaults.SETTINGS_EQUALIZER_CUSTOM_PRESETS);

            try {
                // Get the JSON object
                JSONObject json = new JSONObject(jsonString);

                // Prepare the iterator
                Iterator<?> keys = json.keys();

                // Get the values
                while (keys.hasNext()) {
                    // Ge the key
                    String key = (String) keys.next();

                    // Get the value
                    String value = json.getString(key);

                    // Store the preset
                    result.put(Integer.valueOf(key), value);
                }
            } catch (Exception e) {}

            // All done
            return result;
    	}
    	
    	/**
    	 * Get the bands for a custom preset
    	 * 
    	 * @param position
    	 */
    	public static float[] getCustom(int position) {
            // Prepare the preferences
            String jsonString = PreferenceUtils.getString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS, Constants.Defaults.SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS);

            // Get the band count
            final int bandCount = MediaPlayer.Equalizer.getBandCount();
            
            // Prepare the bands
            final float[] bands = new float[bandCount + 1];
    		
            try {
                // Get the JSON object
                JSONObject json = new JSONObject(jsonString);

                // Get the value
                JSONArray valueArray = json.getJSONArray(String.valueOf(position));

                // Go through the values
                for (int i = 0; i < bandCount + 1; i++) {
                    bands[i] = (float) valueArray.getDouble(i);
                }
            } catch (JSONException e) {}
    		
            // All done
            return bands;
    	}
    	
    	/**
    	 * Add a custom preset
    	 */
    	public static int addCustom(String name) {
            return Presets.addCustom(name, -1);
    	}

    	/**
    	 * Add/update custom preset at position
    	 */
        public static int addCustom(String name, int customPosition) {
            // Get the band count
            final int bandCount = MediaPlayer.Equalizer.getBandCount();

            // Store the bands
            JSONArray bandsArray = new JSONArray();

            try {
                // First item is pre-amplification
                bandsArray.put(0, EqualizerUtil.getEqualizer().getPreAmp());

                // Store the local values
                for (int i = 0; i < bandCount; ++i) {
                    bandsArray.put(i + 1, EqualizerUtil.getEqualizer().getAmp(i));
                }
            } catch (JSONException e1) {}

            // Prepare the preferences
            String jsonStringPresets = PreferenceUtils.getString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS, Constants.Defaults.SETTINGS_EQUALIZER_CUSTOM_PRESETS);
            String jsonStringBands = PreferenceUtils.getString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS, Constants.Defaults.SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS);

            // Prepare the editor
            SharedPreferences.Editor editor = PreferenceUtils.edit();

            try {
                // Get the presets and bands
                JSONObject jsonPresets = new JSONObject(jsonStringPresets);
                JSONObject jsonBands = new JSONObject(jsonStringBands);

                // Get the new key
                int newKey = customPosition >= 0 ? customPosition : jsonPresets.length();

                // Store the preset
                jsonPresets.put(String.valueOf(newKey), name);

                // Store the bands
                jsonBands.put(String.valueOf(newKey), bandsArray);

                // Save to cache
                editor.putString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS, jsonPresets.toString());
                editor.putString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS, jsonBands.toString());
                editor.commit();

                // All done
                return newKey;
            } catch (Exception e) {}

            // Something went wrong
            return -1;
    	}
    	
        /**
         * Update a custom preset
         */
    	public static void updateCustom(int position, String name) {
            Presets.addCustom(name, position);
    	}
    	
    	/**
    	 * Delete the preset
    	 */
    	public static void deleteCustom(int position) {
            // Prepare the preferences
            String jsonStringPresets = PreferenceUtils.getString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS, Constants.Defaults.SETTINGS_EQUALIZER_CUSTOM_PRESETS);
            String jsonStringBands = PreferenceUtils.getString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS, Constants.Defaults.SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS);

            // Prepare the editor
            SharedPreferences.Editor editor = PreferenceUtils.edit();

            try {
                // Get the presets
                JSONObject jsonPresets = new JSONObject(jsonStringPresets);
                JSONObject jsonPresetsNew = new JSONObject();

                // Get the bands
                JSONObject jsonBands = new JSONObject(jsonStringBands);
                JSONObject jsonBandsNew = new JSONObject();

                // Remove the preset
                jsonPresets.remove(String.valueOf(position));
                Iterator<String> keysPresets = jsonPresets.keys();

                // Re-index the presets
                int keysPresetsIndex = 0;
                while (keysPresets.hasNext()) {
                    // Ge the key
                    String key = (String) keysPresets.next();

                    // Get the value
                    String value = jsonPresets.getString(key);

                    // Store the new value
                    jsonPresetsNew.put(String.valueOf(keysPresetsIndex), value);

                    // Increment the index
                    keysPresetsIndex++;
                }

                // Remove the bands
                jsonBands.remove(String.valueOf(position));
                Iterator<String> keysBands = jsonBands.keys();

                // Re-index the bands
                int keysBandsIndex = 0;
                while (keysBands.hasNext()) {
                    // Ge the key
                    String key = (String) keysBands.next();

                    // Get the value
                    JSONArray value = jsonBands.getJSONArray(key);

                    // Store the new value
                    jsonBandsNew.put(String.valueOf(keysBandsIndex), value);

                    // Increment the index
                    keysBandsIndex++;
                }

                // Save to cache
                editor.putString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS, jsonPresetsNew.toString());
                editor.putString(Constants.Keys.SETTINGS_EQUALIZER_CUSTOM_PRESETS_BANDS, jsonBandsNew.toString());
                editor.commit();
            } catch (Exception e) {}
    	}
    }
    
    /**
     * Band listener
     */
    protected class BandListener implements EqualizerBar.OnEqualizerBarChangeListener {
        private final int index;

        /**
         * Band listener
         */
        public BandListener(int index) {
            this.index = index;
        }

        @Override
        public void onProgressChanged(float value) {
            // Set the amplification
            if (null != EqualizerUtil.getEqualizer()) {
                EqualizerUtil.getEqualizer().setAmp(index, value);
            }
        	
            // Store the changes
            ActivityEqualizer.Presets.saveToCache();
            
            // Update the player's eq
            EqualizerUtil.updatePlayer(sMediaPlayer);
            
            // Toggle the action
            mButtonAction.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Track pageview
    	Tracker.trackPageview(this);

        // Set the checked button state
        mButtonEnable.setChecked(PreferenceUtils.getBoolean(Constants.Keys.SETTINGS_EQUALIZER_ENABLED, Constants.Defaults.SETTINGS_EQUALIZER_ENABLED));
        
        // Set the checked button listener
        mButtonEnable.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            	// Store the changes
            	ActivityEqualizer.Presets.saveToCache(false);
                
                // Update the player's eq
                EqualizerUtil.updatePlayer(sMediaPlayer);
                
                // Inform the user
                Toast.makeText(ActivityEqualizer.this, ActivityEqualizer.this.getText(isChecked ? R.string.fp_equalizer_toast_enabled : R.string.fp_equalizer_toast_disabled), Toast.LENGTH_LONG).show();
            }
        });
        
        // Get the volume indexes
        final int volumeMax = sAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volumeCurrent = sAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        
        // Set the listener
        mVolumeKnob.setPercentage(Math.round(100 * volumeCurrent / volumeMax));
        mVolumeKnob.setListener(new ElementKnob.Listener() {
            public void onRotate(int value) {
            	sAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(volumeMax * value / 100), 0);
            }

            public void onChange(int percentage) {}
        });  

        // Update the presets
        Presets.uiRefreshList();

        // Set the default selection asynchronously to prevent a layout initialization bug.
        final int equalizer_preset_pref = PreferenceUtils.getPreferences(this).getInt(Constants.Keys.SETTINGS_EQUALIZER_PRESET, Constants.Defaults.SETTINGS_EQUALIZER_PRESET);
        
        // Presets change listener 
        mButtonPresets.post(new Runnable() {
            @Override
            public void run() {
            	// Set the on-select listener
                mButtonPresets.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    	// Get the equalizer
                    	Presets.loadPreset(pos);
                    	
                    	// Update the UI
                    	if (null != EqualizerUtil.getEqualizer()) {
                            // Set the preamplifications
                            mPreAmpKnob.setPercentage(
                                Math.round(
                                    100 * (
                                        (null != EqualizerUtil.getEqualizer() ? EqualizerUtil.getEqualizer().getPreAmp() : 0) + 20
                                    ) / 40
                                )
                            );

                            // Set the bars
                            for (int i = 0; i < MediaPlayer.Equalizer.getBandCount(); ++i) {
                                EqualizerBar bar = (EqualizerBar) mBandsContainers.getChildAt(i);
                                if (null != bar) {
                                    bar.setValue(EqualizerUtil.getEqualizer().getAmp(i));
                                }
                            }
                    	}
                    	
                    	// Store the settings
                    	ActivityEqualizer.Presets.saveToCache(false);
                    	
                    	// Update the player's eq
                    	EqualizerUtil.updatePlayer(sMediaPlayer);
                    	
                    	// Inform the user
                        Toast.makeText(ActivityEqualizer.this, ActivityEqualizer.this.getString(R.string.fp_equalizer_toast_loaded, mButtonPresets.getSelectedItem().toString()), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }
        });

        // Initialize the selection
        mButtonPresets.setSelection(equalizer_preset_pref, true);
        
        // Pre-amplifier state
        mPreAmpKnob.setPercentage(
            Math.round(
                100 * (
                    (null != EqualizerUtil.getEqualizer() ? EqualizerUtil.getEqualizer().getPreAmp() : 0) + 20
                ) / 40
            )
        );
        
        // Pre-amplifier change
        mPreAmpKnob.setListener(new ElementKnob.Listener() {
            public void onRotate(int value) {
                // Valid equalizer
                if (null != EqualizerUtil.getEqualizer()) {
                    // Set preamp
                    EqualizerUtil.getEqualizer().setPreAmp(
                        Math.round(value * 40 / 100) - 20
                    );
                }
                
                // Store the changes
                ActivityEqualizer.Presets.saveToCache();
                
                // Update the player's eq
                EqualizerUtil.updatePlayer(sMediaPlayer);
            }

            public void onChange(int percentage) {}
        }); 

        // Bands listener
        for (int bandIndex = 0; bandIndex < MediaPlayer.Equalizer.getBandCount(); bandIndex++) {
            // Prepare the equalizer bar
            EqualizerBar equalizerBar = new EqualizerBar(
                this, 
                MediaPlayer.Equalizer.getBandFrequency(bandIndex)
            );
            
            // Set the value
            if (null != EqualizerUtil.getEqualizer()) {
            	equalizerBar.setValue(EqualizerUtil.getEqualizer().getAmp(bandIndex));
            }

            // Set the listener
            equalizerBar.setListener(new BandListener(bandIndex));

            // Add the view
            mBandsContainers.addView(equalizerBar);
            
            // Set the layout parameters
            equalizerBar.setLayoutParams(
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1)
            );
        }
        
        ActivityEqualizer.Presets.saveToCache(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mButtonEnable.setOnCheckedChangeListener(null);
        mButtonPresets.setOnItemSelectedListener(null);
        mBandsContainers.removeAllViews();
    }
}

/*EOF*/