/**   Copyright (C) 2013  Louis Teboul (a.k.a Androguide)
 *
 *    admin@pimpmyrom.org  || louisteboul@gmail.com
 *    http://pimpmyrom.org || http://androguide.fr
 *    71 quai Clémenceau, 69300 Caluire-et-Cuire, FRANCE.
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License along
 *      with this program; if not, write to the Free Software Foundation, Inc.,
 *      51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 **/

package com.androguide.honamicontrol.kernel.powermanagement;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.androguide.honamicontrol.R;
import com.androguide.honamicontrol.helpers.CMDProcessor.CMDProcessor;
import com.androguide.honamicontrol.helpers.CPUHelper;
import com.androguide.honamicontrol.helpers.Helpers;

import java.util.ArrayList;

public class PowerManagementActivity extends ActionBarActivity implements PowerManagementInterface {

    private int spinnerCounter = 0, ecoCoresCounter = 0, alucardCoresCounter = 0, hotplugCounter = 0, minCounter = 0, maxCounter = 0, boostCounter = 0, thresholdBoostCounter = 0, idleCounter = 0;
    private Boolean isIntelliPlugOn;
    private LinearLayout mCardIntelliEco, mCardIntelliCores, mCardAlucardCores, mCardFastPlugNumberCores, mCardFastBoostDuration, mCardFastThresholdBoost;
    private Spinner mEcoCoresSpinner;
    private Boolean hasIntelliPlug = false;
    private Boolean hasAlucardPlug = false;
    private Boolean hasMsmMpdecision = false;
    private Boolean hasFastHotplug = false;
    private ArrayList<Integer> driverNumbers = new ArrayList<Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setIcon(getResources().getDrawable(R.drawable.ic_tools_power_management));
        setContentView(R.layout.card_power_management);
        final SharedPreferences bootPrefs = getSharedPreferences("BOOT_PREFS", 0);

        mCardIntelliEco = (LinearLayout) findViewById(R.id.card_intelliplug_eco_mode);
        mCardIntelliCores = (LinearLayout) findViewById(R.id.card_intelliplug_eco_cores);
        mCardAlucardCores = (LinearLayout) findViewById(R.id.card_alucard_cores);

        // Sched MC
        if (Helpers.doesFileExist(SCHED_MC_POWER_SAVINGS)) {
            Spinner schedMcSpinner = (Spinner) findViewById(R.id.sched_mc_spinner);
            ArrayList<String> schedMCEntries = new ArrayList<String>();
            schedMCEntries.add(getString(R.string.disabled));
            schedMCEntries.add(getString(R.string.moderate));
            schedMCEntries.add(getString(R.string.aggressive));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_row, schedMCEntries);
            schedMcSpinner.setAdapter(adapter);
            schedMcSpinner.setSelection(Integer.valueOf(CPUHelper.readOneLineNotRoot(SCHED_MC_POWER_SAVINGS)));
            schedMcSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    bootPrefs.edit().putInt("SCHED_MC_LEVEL", i).commit();
                    if (spinnerCounter > 0)
                        Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + i + " > " + SCHED_MC_POWER_SAVINGS);
                    else
                        spinnerCounter++;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        // Hotplug Driver
        Spinner hotplugDriverSpinner = (Spinner) findViewById(R.id.hotplug_spinner);
        ArrayList<String> availableDrivers = new ArrayList<String>();
        availableDrivers.add("MPDecision");
	driverNumbers.add(0);
        int intelliState = 0;
        int alucardState = 0;
	int msmState = 0;
	int fastState = 0;

        try {
            if (Helpers.doesFileExist(INTELLI_PLUG_TOGGLE)) {
                hasIntelliPlug = true;
                intelliState = Integer.parseInt(CPUHelper.readOneLineNotRoot(INTELLI_PLUG_TOGGLE));
                availableDrivers.add("Intelliplug");
		driverNumbers.add(1);
            }

            if (Helpers.doesFileExist(ALUCARD_HOTPLUG_TOGGLE)) {
                hasAlucardPlug = true;
                alucardState = Integer.parseInt(CPUHelper.readOneLineNotRoot(ALUCARD_HOTPLUG_TOGGLE));
                availableDrivers.add("Alucard Hotplug");
		driverNumbers.add(2);
            }

            if (Helpers.doesFileExist(MSM_MPDECISION_TOGGLE)) {
                hasMsmMpdecision = true;
                msmState = Integer.parseInt(CPUHelper.readOneLineNotRoot(MSM_MPDECISION_TOGGLE));
                availableDrivers.add("Msm mpdecision");
		driverNumbers.add(3);
            }

            if (Helpers.doesFileExist(FAST_HOTPLUG_TOGGLE)) {
                hasFastHotplug = true;
                fastState = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_TOGGLE));
                availableDrivers.add("Fast Hotplug");
		driverNumbers.add(4);
            }

            ArrayAdapter<String> hotplugAdapter = new ArrayAdapter<String>(this, R.layout.spinner_row, availableDrivers);
            hotplugDriverSpinner.setAdapter(hotplugAdapter);

            /** If the kernel doesn't have intelliplug nor alucard hotplug */
            if (!hasAlucardPlug && !hasIntelliPlug && !hasMsmMpdecision && !hasFastHotplug) {
                // then default to mpdecision
                hotplugDriverSpinner.setSelection(0);

            } else if (hasFastHotplug && fastState == 1) {

                    hotplugDriverSpinner.setSelection(driverNumbers.indexOf(4));

            } else if (hasIntelliPlug && intelliState == 1) {

                    hotplugDriverSpinner.setSelection(driverNumbers.indexOf(1)); // if intelli plug is on, then it's the current hotplug driver

            } else if (hasMsmMpdecision && msmState == 1) {

                    hotplugDriverSpinner.setSelection(driverNumbers.indexOf(3)); // if msm_mpdecision is on, then it's the current hotplug driver

            } else if (hasAlucardPlug && alucardState == 1) {

                    hotplugDriverSpinner.setSelection(driverNumbers.indexOf(2)); // if alucard hotplug is on, then it's the current hotplug driver

            } else {
		    hotplugDriverSpinner.setSelection(0);
            }

        } catch (Exception e) {
            Log.e("PowerManagement", e.getMessage());
        }

        hotplugDriverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                bootPrefs.edit().putInt("HOTPLUG_DRIVER", pos).commit();
                switch (driverNumbers.get(pos)) {
                    case 0:
                        mCardIntelliEco.setVisibility(View.GONE);
                        mCardIntelliCores.setVisibility(View.GONE);
                        mCardAlucardCores.setVisibility(View.GONE);
			hideFastHotplugConfig();
			isIntelliPlugOn = false;
			if (hotplugCounter > 0) {
				if(hasIntelliPlug)
					CMDProcessor.runSuCommand("echo 0 > " + INTELLI_PLUG_TOGGLE);
				if(hasAlucardPlug)
					CMDProcessor.runSuCommand("echo 0 > " + ALUCARD_HOTPLUG_TOGGLE);
				if(hasMsmMpdecision)
					CMDProcessor.runSuCommand("echo 0 > " + MSM_MPDECISION_TOGGLE);
				if(hasFastHotplug)
					CMDProcessor.runSuCommand("echo 0 > " + FAST_HOTPLUG_TOGGLE);

				CMDProcessor.runSuCommand("start mpdecision");

                        } else hotplugCounter++;
			break;

		    case 1:
			mCardAlucardCores.setVisibility(View.GONE);
			mCardIntelliEco.setVisibility(View.VISIBLE);
			mCardIntelliCores.setVisibility(View.VISIBLE);
			hideFastHotplugConfig();
			isIntelliPlugOn = true;
			if (hotplugCounter > 0) {
				if(hasAlucardPlug)
					CMDProcessor.runSuCommand("echo 0 > " + ALUCARD_HOTPLUG_TOGGLE);
				if(hasMsmMpdecision)
					CMDProcessor.runSuCommand("echo 0 > " + MSM_MPDECISION_TOGGLE);
				if(hasFastHotplug)
					CMDProcessor.runSuCommand("echo 0 > " + FAST_HOTPLUG_TOGGLE);
				CMDProcessor.runSuCommand("stop mpdecision");
				CMDProcessor.runSuCommand("echo 1 > " + INTELLI_PLUG_TOGGLE);
			} else hotplugCounter++;
			break;

		    case 2:
			mCardIntelliEco.setVisibility(View.GONE);
			mCardIntelliCores.setVisibility(View.GONE);
			mCardAlucardCores.setVisibility(View.VISIBLE);
			hideFastHotplugConfig();
			isIntelliPlugOn = false;
			if (hotplugCounter > 0) {
				if(hasIntelliPlug)
					CMDProcessor.runSuCommand("echo 0 > " + INTELLI_PLUG_TOGGLE);
				if(hasMsmMpdecision)
					CMDProcessor.runSuCommand("echo 0 > " + MSM_MPDECISION_TOGGLE);
				if(hasFastHotplug)
					CMDProcessor.runSuCommand("echo 0 > " + FAST_HOTPLUG_TOGGLE);
				CMDProcessor.runSuCommand("stop mpdecision");
				CMDProcessor.runSuCommand("echo 1 > " + ALUCARD_HOTPLUG_TOGGLE);
			} else hotplugCounter++;

		    case 3:
			mCardIntelliEco.setVisibility(View.GONE);
			mCardIntelliCores.setVisibility(View.GONE);
			mCardAlucardCores.setVisibility(View.GONE);
			hideFastHotplugConfig();
			isIntelliPlugOn = false;
			if (hotplugCounter > 0) {
				if(hasIntelliPlug)
					CMDProcessor.runSuCommand("echo 0 > " + INTELLI_PLUG_TOGGLE);
				if(hasAlucardPlug)
					CMDProcessor.runSuCommand("echo 0 > " + ALUCARD_HOTPLUG_TOGGLE);
				if(hasFastHotplug)
					CMDProcessor.runSuCommand("echo 0 > " + FAST_HOTPLUG_TOGGLE);
				CMDProcessor.runSuCommand("stop mpdecision");
				CMDProcessor.runSuCommand("echo 1 > " + MSM_MPDECISION_TOGGLE);

			} else hotplugCounter++;
			break;

		    case 4:
			mCardIntelliEco.setVisibility(View.GONE);
			mCardIntelliCores.setVisibility(View.GONE);
			mCardAlucardCores.setVisibility(View.GONE);
			showFastHotplugConfig();
			isIntelliPlugOn = false;
			if (hotplugCounter > 0) {
				if(hasIntelliPlug)
					CMDProcessor.runSuCommand("echo 0 > " + INTELLI_PLUG_TOGGLE);
				if(hasAlucardPlug)
					CMDProcessor.runSuCommand("echo 0 > " + ALUCARD_HOTPLUG_TOGGLE);
				if(hasMsmMpdecision)
					CMDProcessor.runSuCommand("echo 0 > " + MSM_MPDECISION_TOGGLE);
				CMDProcessor.runSuCommand("stop mpdecision");
				CMDProcessor.runSuCommand("echo 1 > " + FAST_HOTPLUG_TOGGLE);

			} else hotplugCounter++;
			break;
		    default:
			break;
		}
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

	// Smart hotplug controls
	if(Helpers.doesFileExist(FAST_HOTPLUG_TOGGLE)){
		Spinner fastHotplugMinCoresSpinner = (Spinner) findViewById(R.id.fast_hotplug_min_cores_spinner);
		Spinner fastHotplugMaxCoresSpinner = (Spinner) findViewById(R.id.fast_hotplug_max_cores_spinner);
		ArrayList<String> minMaxCores = new ArrayList<String>();
		minMaxCores.add("1");
		minMaxCores.add("2");
		minMaxCores.add("3");
		minMaxCores.add("4");
		ArrayAdapter<String> minCoresAdapter = new ArrayAdapter<String>(this, R.layout.spinner_row, minMaxCores);
		ArrayAdapter<String> maxCoresAdapter = new ArrayAdapter<String>(this, R.layout.spinner_row, minMaxCores);
		fastHotplugMinCoresSpinner.setAdapter(minCoresAdapter);
		fastHotplugMaxCoresSpinner.setAdapter(maxCoresAdapter);
		int minCores = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_MIN_CORES)) - 1;
		int maxCores = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_MAX_CORES)) - 1;
		if(minCores < 0)
			minCores = 0;
		if(minCores > 2)
			minCores = 2;	
		fastHotplugMinCoresSpinner.setSelection(minCores);
		if(maxCores < 0)
			maxCores = 0;
		if(maxCores > 3)
			maxCores = 3;	
		fastHotplugMaxCoresSpinner.setSelection(maxCores);

		fastHotplugMinCoresSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position + 1;
				if (minCounter > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_MIN_CORES);
					bootPrefs.edit().putInt("FAST_HOTPLUG_MIN_CORES", toApply).commit();
				} else {
					minCounter++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});
		fastHotplugMaxCoresSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position + 1;
				if (maxCounter > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_MAX_CORES);
					bootPrefs.edit().putInt("FAST_HOTPLUG_MAX_CORES", toApply).commit();
				} else {
					maxCounter++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});



		Spinner fastHotplugBoostDuration = (Spinner) findViewById(R.id.fast_hotplug_boost_duration_spinner);
		ArrayList<String> loadLevels = new ArrayList<String>();

		for(int i = 0; i <= 10000; i+=100){
			loadLevels.add(String.valueOf(i));
		}
		ArrayAdapter<String> boostDurationAdapter = new ArrayAdapter<String>(this, R.layout.spinner_row, loadLevels);
		fastHotplugBoostDuration.setAdapter(boostDurationAdapter);
		int boostDuration = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_BOOST_DURATION)) / 100;
		if(boostDuration < 0)
			boostDuration = 0;
		if(boostDuration > 100)
			boostDuration = 100;	
		fastHotplugBoostDuration.setSelection(boostDuration);

		fastHotplugBoostDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position * 100;
				if (boostCounter > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_BOOST_DURATION);
					bootPrefs.edit().putInt("FAST_HOTPLUG_BOOST_DURATION", toApply).commit();
				} else {
					boostCounter++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});

		Spinner fastHotplugThresholdToBoost = (Spinner) findViewById(R.id.fast_hotplug_threshold_to_boost_spinner);

		ArrayAdapter<String> thresholdToBoostAdapter = new ArrayAdapter<String>(this, R.layout.spinner_row, loadLevels);
		fastHotplugThresholdToBoost.setAdapter(thresholdToBoostAdapter);
		int thresholdBoost = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_THRESHOLD_TO_BOOST)) / 100;
		if(thresholdBoost < 0)
			thresholdBoost = 0;
		if(thresholdBoost > 100)
			thresholdBoost = 100;	
		fastHotplugThresholdToBoost.setSelection(thresholdBoost);

		fastHotplugThresholdToBoost.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position * 100;
				if (thresholdBoostCounter > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_THRESHOLD_TO_BOOST);
					bootPrefs.edit().putInt("FAST_HOTPLUG_THRESHOLD_TO_BOOST", toApply).commit();
				} else {
					thresholdBoostCounter++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});


		Spinner fastHotplugIdleThreshold = (Spinner) findViewById(R.id.fast_hotplug_idle_threshold_spinner);

		ArrayAdapter<String> idleThresholdAdapter = new ArrayAdapter<String>(this, R.layout.spinner_row, loadLevels);
		fastHotplugIdleThreshold.setAdapter(idleThresholdAdapter);
		int idleThreshold = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_IDLE_THRESHOLD)) / 100;
		if(idleThreshold < 0)
			idleThreshold = 0;
		if(idleThreshold > 100)
			idleThreshold = 100;	
		fastHotplugIdleThreshold.setSelection(idleThreshold);

		fastHotplugIdleThreshold.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position * 100;
				if (idleCounter > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_IDLE_THRESHOLD);
					bootPrefs.edit().putInt("FAST_HOTPLUG_IDLE_THRESHOLD", toApply).commit();
				} else {
					idleCounter++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});
	}

        // Intelliplug Eco Mode
        if (Helpers.doesFileExist(INTELLI_PLUG_ECO_MODE)) {
            final Switch intelliEcoSwitch = (Switch) findViewById(R.id.intelliplug_eco_switch);
            int currEcoState = Integer.parseInt(CPUHelper.readOneLineNotRoot(INTELLI_PLUG_ECO_MODE));

            if (currEcoState == 0) {
                intelliEcoSwitch.setChecked(false);
                findViewById(R.id.intelliplug_eco_cores_spinner).setEnabled(false);
            } else if (currEcoState == 1) {
                intelliEcoSwitch.setChecked(true);
                findViewById(R.id.intelliplug_eco_cores_spinner).setEnabled(true);
            }

            intelliEcoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isOn) {
                    bootPrefs.edit().putBoolean("INTELLI_PLUG_ECO", isOn).commit();

                    if (isOn) {
                        Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 1 > " + INTELLI_PLUG_ECO_MODE);
                        findViewById(R.id.intelliplug_eco_cores_spinner).setEnabled(true);
                    } else {
                        Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 0 > " + INTELLI_PLUG_ECO_MODE);
                        findViewById(R.id.intelliplug_eco_cores_spinner).setEnabled(false);
                    }
                }
            });
        }

        // Intelliplug eco cores
        if (Helpers.doesFileExist(INTELLI_PLUG_ECO_CORES)) {
            Spinner intelliEcoCoresSpinner = (Spinner) findViewById(R.id.intelliplug_eco_cores_spinner);
            mEcoCoresSpinner = intelliEcoCoresSpinner;
            ArrayList<String> possibleEcoCores = new ArrayList<String>();
            possibleEcoCores.add("1");
            possibleEcoCores.add("2");
            possibleEcoCores.add("3");
            ArrayAdapter<String> ecoCoresAdapter = new ArrayAdapter<String>(this, R.layout.spinner_row, possibleEcoCores);
            intelliEcoCoresSpinner.setAdapter(ecoCoresAdapter);
            intelliEcoCoresSpinner.setSelection(Integer.parseInt(CPUHelper.readOneLineNotRoot(INTELLI_PLUG_ECO_CORES)) - 1);
            if (!bootPrefs.getBoolean("INTELLI_PLUG_ECO", false))
                intelliEcoCoresSpinner.setEnabled(false);
            intelliEcoCoresSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int toApply = position + 1;
                    if (ecoCoresCounter > 0) {
                        Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + INTELLI_PLUG_ECO_CORES);
                        bootPrefs.edit().putInt("INTELLI_PLUG_ECO_CORES", toApply).commit();
                    } else {
                        ecoCoresCounter++;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

	}

        // Alucard eco cores
        if (Helpers.doesFileExist(ALUCARD_HOTPLUG_CORES)) {
            final Spinner alucardCoresSpinner = (Spinner) findViewById(R.id.alucard_cores_spinner);
            mEcoCoresSpinner = alucardCoresSpinner;
            ArrayList<String> possibleEcoCores = new ArrayList<String>();
            possibleEcoCores.add("1");
            possibleEcoCores.add("2");
            possibleEcoCores.add("3");
            possibleEcoCores.add("4");
            ArrayAdapter<String> ecoCoresAdapter = new ArrayAdapter<String>(this, R.layout.spinner_row, possibleEcoCores);
            alucardCoresSpinner.setAdapter(ecoCoresAdapter);
            alucardCoresSpinner.setSelection(Integer.parseInt(CPUHelper.readOneLineNotRoot(ALUCARD_HOTPLUG_CORES)) - 1);
            alucardCoresSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int toApply = position + 1;
                    if (alucardCoresCounter > 0) {
                        Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + ALUCARD_HOTPLUG_CORES);
                        bootPrefs.edit().putInt("ALUCARD_CORES", toApply).commit();
                    } else {
                        alucardCoresCounter++;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        // Power Suspend
        if (Helpers.doesFileExist(POWER_SUSPEND_TOGGLE)) {
            Switch powerSuspendSwitch = (Switch) findViewById(R.id.power_suspend_switch);
            int isPowerSuspendOn = Integer.parseInt(CPUHelper.readOneLineNotRoot(POWER_SUSPEND_TOGGLE));
            if (isPowerSuspendOn == 0)
                powerSuspendSwitch.setChecked(false);
            else if (isPowerSuspendOn == 1)
                powerSuspendSwitch.setChecked(true);

            powerSuspendSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    bootPrefs.edit().putBoolean("POWER_SUSPEND", isChecked).commit();
                    if (isChecked)
                        Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 1 > " + POWER_SUSPEND_TOGGLE);
                    else {
                        Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 0 > " + POWER_SUSPEND_TOGGLE);
                    }
                }
            });
        } else {
            findViewById(R.id.card_power_suspend).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showFastHotplugConfig(){
	    LinearLayout mCardFastPlugNumberCores, mCardFastBoostDuration, mCardFastThresholdBoost, mCardIdleThreshold;

	    mCardFastPlugNumberCores = (LinearLayout) findViewById(R.id.card_fast_hotplug_number_of_cores);
	    mCardFastBoostDuration = (LinearLayout) findViewById(R.id.card_fast_hotplug_boost_duration);
	    mCardFastThresholdBoost = (LinearLayout) findViewById(R.id.card_fast_hotplug_threshold_to_boost);
	    mCardIdleThreshold = (LinearLayout) findViewById(R.id.card_fast_hotplug_idle_threshold);
	    mCardFastPlugNumberCores.setVisibility(View.VISIBLE);
	    mCardFastBoostDuration.setVisibility(View.VISIBLE);
	    mCardFastThresholdBoost.setVisibility(View.VISIBLE);
	    mCardIdleThreshold.setVisibility(View.VISIBLE);
    }
    private void hideFastHotplugConfig(){
	    LinearLayout mCardFastPlugNumberCores, mCardFastBoostDuration, mCardFastThresholdBoost, mCardIdleThreshold;

	    mCardFastPlugNumberCores = (LinearLayout) findViewById(R.id.card_fast_hotplug_number_of_cores);
	    mCardFastBoostDuration = (LinearLayout) findViewById(R.id.card_fast_hotplug_boost_duration);
	    mCardFastThresholdBoost = (LinearLayout) findViewById(R.id.card_fast_hotplug_threshold_to_boost);
	    mCardIdleThreshold = (LinearLayout) findViewById(R.id.card_fast_hotplug_idle_threshold);
	    mCardFastPlugNumberCores.setVisibility(View.GONE);
	    mCardFastBoostDuration.setVisibility(View.GONE);
	    mCardFastThresholdBoost.setVisibility(View.GONE);
	    mCardIdleThreshold.setVisibility(View.GONE);
	    }

}
