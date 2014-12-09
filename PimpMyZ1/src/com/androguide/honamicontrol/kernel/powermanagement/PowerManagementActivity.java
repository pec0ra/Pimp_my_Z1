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

    private int spinnerCounter = 0, ecoCoresCounter = 0, alucardCoresCounter = 0, hotplugCounter = 0, minCounter = 0, maxCounter = 0, boostCounter = 0, thresholdBoostCounter = 0, idleCounter = 0, inThresholdCounter1 = 0, inThresholdCounter2 = 0, inThresholdCounter3 = 0, inDelayCounter1 = 0, inDelayCounter2 = 0, inDelayCounter3 = 0, outThresholdCounter1 = 0, outThresholdCounter2 = 0, outThresholdCounter3 = 0, outDelayCounter1 = 0, outDelayCounter2 = 0, outDelayCounter3 = 0;
    private Boolean isIntelliPlugOn;
    private LinearLayout mCardIntelliEco, mCardIntelliCores, mCardAlucardCores;
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

		bootPrefs.edit().putInt("HOTPLUG_DRIVER", driverNumbers.get(pos)).commit();
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

		Switch fastHotplugSingleCore = (Switch) findViewById(R.id.fast_hotplug_screen_off_singlecore_switch);

		int singleCoreState = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_SCREEN_OFF_SINGLECORE));

		if (singleCoreState == 0) {
			fastHotplugSingleCore.setChecked(false);
		} else if (singleCoreState == 1) {
			fastHotplugSingleCore.setChecked(true);
		}

		fastHotplugSingleCore.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isOn) {
				bootPrefs.edit().putBoolean("FAST_HOTPLUG_SCREEN_OFF_SINGLECORE", isOn).commit();

				if (isOn) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 1 > " + FAST_HOTPLUG_SCREEN_OFF_SINGLECORE);
				} else {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 0 > " + FAST_HOTPLUG_SCREEN_OFF_SINGLECORE);
				}
			}
		});



		// Plug in thresholds

		Spinner fastHotplugInThreshold2 = (Spinner) findViewById(R.id.fast_hotplug_in_threshold_spinner_2);

		ArrayAdapter<String> inThresholdAdapter2 = new ArrayAdapter<String>(this, R.layout.spinner_row, loadLevels);
		fastHotplugInThreshold2.setAdapter(inThresholdAdapter2);
		int inThreshold2 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_IN_THRESHOLD_2)) / 100;
		if(inThreshold2 < 0)
			inThreshold2 = 0;
		if(inThreshold2 > 100)
			inThreshold2 = 100;	
		fastHotplugInThreshold2.setSelection(inThreshold2);

		fastHotplugInThreshold2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position * 100;
				if (inThresholdCounter2 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_IN_THRESHOLD_2);
					bootPrefs.edit().putInt("FAST_HOTPLUG_IN_THRESHOLD_2", toApply).commit();
				} else {
					inThresholdCounter2++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});

		Spinner fastHotplugInThreshold3 = (Spinner) findViewById(R.id.fast_hotplug_in_threshold_spinner_3);

		ArrayAdapter<String> inThresholdAdapter3 = new ArrayAdapter<String>(this, R.layout.spinner_row, loadLevels);
		fastHotplugInThreshold3.setAdapter(inThresholdAdapter3);
		int inThreshold3 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_IN_THRESHOLD_3)) / 100;
		if(inThreshold3 < 0)
			inThreshold3 = 0;
		if(inThreshold3 > 100)
			inThreshold3 = 100;	
		fastHotplugInThreshold3.setSelection(inThreshold3);

		fastHotplugInThreshold3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position * 100;
				if (inThresholdCounter3 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_IN_THRESHOLD_3);
					bootPrefs.edit().putInt("FAST_HOTPLUG_IN_THRESHOLD_3", toApply).commit();
				} else {
					inThresholdCounter3++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});

		Spinner fastHotplugInThreshold1 = (Spinner) findViewById(R.id.fast_hotplug_in_threshold_spinner_1);

		ArrayAdapter<String> inThresholdAdapter1 = new ArrayAdapter<String>(this, R.layout.spinner_row, loadLevels);
		fastHotplugInThreshold1.setAdapter(inThresholdAdapter1);
		int inThreshold1 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_IN_THRESHOLD_1)) / 100;
		if(inThreshold1 < 0)
			inThreshold1 = 0;
		if(inThreshold1 > 100)
			inThreshold1 = 100;	
		fastHotplugInThreshold1.setSelection(inThreshold1);

		fastHotplugInThreshold1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position * 100;
				if (inThresholdCounter1 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_IN_THRESHOLD_1);
					bootPrefs.edit().putInt("FAST_HOTPLUG_IN_THRESHOLD_1", toApply).commit();
				} else {
					inThresholdCounter1++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});


		// Plug in delays

		Spinner fastHotplugInDelay2 = (Spinner) findViewById(R.id.fast_hotplug_in_delay_spinner_2);

		ArrayList<String> delayLevels = new ArrayList<String>();

		for(int i = 0; i <= 20; i++){
			delayLevels.add(String.valueOf(i));
		}


		ArrayAdapter<String> inDelayAdapter2 = new ArrayAdapter<String>(this, R.layout.spinner_row, delayLevels);
		fastHotplugInDelay2.setAdapter(inDelayAdapter2);
		int inDelay2 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_IN_DELAY_2));
		if(inDelay2 < 0)
			inDelay2 = 0;
		if(inDelay2 > 20)
			inDelay2 = 20;	
		fastHotplugInDelay2.setSelection(inDelay2);

		fastHotplugInDelay2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position;
				if (inDelayCounter2 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_IN_DELAY_2);
					bootPrefs.edit().putInt("FAST_HOTPLUG_IN_DELAY_2", toApply).commit();
				} else {
					inDelayCounter2++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});

		Spinner fastHotplugInDelay3 = (Spinner) findViewById(R.id.fast_hotplug_in_delay_spinner_3);

		ArrayAdapter<String> inDelayAdapter3 = new ArrayAdapter<String>(this, R.layout.spinner_row, delayLevels);
		fastHotplugInDelay3.setAdapter(inDelayAdapter3);
		int inDelay3 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_IN_DELAY_3));
		if(inDelay3 < 0)
			inDelay3 = 0;
		if(inDelay3 > 20)
			inDelay3 = 20;	
		fastHotplugInDelay3.setSelection(inDelay3);

		fastHotplugInDelay3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position;
				if (inDelayCounter3 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_IN_DELAY_3);
					bootPrefs.edit().putInt("FAST_HOTPLUG_IN_DELAY_3", toApply).commit();
				} else {
					inDelayCounter3++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});

		Spinner fastHotplugInDelay1 = (Spinner) findViewById(R.id.fast_hotplug_in_delay_spinner_1);

		ArrayAdapter<String> inDelayAdapter1 = new ArrayAdapter<String>(this, R.layout.spinner_row, delayLevels);
		fastHotplugInDelay1.setAdapter(inDelayAdapter1);
		int inDelay1 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_IN_DELAY_1));
		if(inDelay1 < 0)
			inDelay1 = 0;
		if(inDelay1 > 20)
			inDelay1 = 20;	
		fastHotplugInDelay1.setSelection(inDelay1);

		fastHotplugInDelay1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position;
				if (inDelayCounter1 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_IN_DELAY_1);
					bootPrefs.edit().putInt("FAST_HOTPLUG_IN_DELAY_1", toApply).commit();
				} else {
					inDelayCounter1++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});




		// Plug out thresholds

		Spinner fastHotplugOutThreshold2 = (Spinner) findViewById(R.id.fast_hotplug_out_threshold_spinner_2);

		ArrayAdapter<String> outThresholdAdapter2 = new ArrayAdapter<String>(this, R.layout.spinner_row, loadLevels);
		fastHotplugOutThreshold2.setAdapter(outThresholdAdapter2);
		int outThreshold2 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_OUT_THRESHOLD_2)) / 100;
		if(outThreshold2 < 0)
			outThreshold2 = 0;
		if(outThreshold2 > 100)
			outThreshold2 = 100;	
		fastHotplugOutThreshold2.setSelection(outThreshold2);

		fastHotplugOutThreshold2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position * 100;
				if (outThresholdCounter2 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_OUT_THRESHOLD_2);
					bootPrefs.edit().putInt("FAST_HOTPLUG_OUT_THRESHOLD_2", toApply).commit();
				} else {
					outThresholdCounter2++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});

		Spinner fastHotplugOutThreshold3 = (Spinner) findViewById(R.id.fast_hotplug_out_threshold_spinner_3);

		ArrayAdapter<String> outThresholdAdapter3 = new ArrayAdapter<String>(this, R.layout.spinner_row, loadLevels);
		fastHotplugOutThreshold3.setAdapter(outThresholdAdapter3);
		int outThreshold3 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_OUT_THRESHOLD_3)) / 100;
		if(outThreshold3 < 0)
			outThreshold3 = 0;
		if(outThreshold3 > 100)
			outThreshold3 = 100;	
		fastHotplugOutThreshold3.setSelection(outThreshold3);

		fastHotplugOutThreshold3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position * 100;
				if (outThresholdCounter3 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_OUT_THRESHOLD_3);
					bootPrefs.edit().putInt("FAST_HOTPLUG_OUT_THRESHOLD_3", toApply).commit();
				} else {
					outThresholdCounter3++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});

		Spinner fastHotplugOutThreshold1 = (Spinner) findViewById(R.id.fast_hotplug_out_threshold_spinner_1);

		ArrayAdapter<String> outThresholdAdapter1 = new ArrayAdapter<String>(this, R.layout.spinner_row, loadLevels);
		fastHotplugOutThreshold1.setAdapter(outThresholdAdapter1);
		int outThreshold1 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_OUT_THRESHOLD_1)) / 100;
		if(outThreshold1 < 0)
			outThreshold1 = 0;
		if(outThreshold1 > 100)
			outThreshold1 = 100;	
		fastHotplugOutThreshold1.setSelection(outThreshold1);

		fastHotplugOutThreshold1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position * 100;
				if (outThresholdCounter1 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_OUT_THRESHOLD_1);
					bootPrefs.edit().putInt("FAST_HOTPLUG_OUT_THRESHOLD_1", toApply).commit();
				} else {
					outThresholdCounter1++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});


		// Plug out delays

		Spinner fastHotplugOutDelay2 = (Spinner) findViewById(R.id.fast_hotplug_out_delay_spinner_2);

		ArrayAdapter<String> outDelayAdapter2 = new ArrayAdapter<String>(this, R.layout.spinner_row, delayLevels);
		fastHotplugOutDelay2.setAdapter(outDelayAdapter2);
		int outDelay2 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_OUT_DELAY_2));
		if(outDelay2 < 0)
			outDelay2 = 0;
		if(outDelay2 > 20)
			outDelay2 = 20;	
		fastHotplugOutDelay2.setSelection(outDelay2);

		fastHotplugOutDelay2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position;
				if (outDelayCounter2 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_OUT_DELAY_2);
					bootPrefs.edit().putInt("FAST_HOTPLUG_OUT_DELAY_2", toApply).commit();
				} else {
					outDelayCounter2++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});

		Spinner fastHotplugOutDelay3 = (Spinner) findViewById(R.id.fast_hotplug_out_delay_spinner_3);

		ArrayAdapter<String> outDelayAdapter3 = new ArrayAdapter<String>(this, R.layout.spinner_row, delayLevels);
		fastHotplugOutDelay3.setAdapter(outDelayAdapter3);
		int outDelay3 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_OUT_DELAY_3));
		if(outDelay3 < 0)
			outDelay3 = 0;
		if(outDelay3 > 20)
			outDelay3 = 20;	
		fastHotplugOutDelay3.setSelection(outDelay3);

		fastHotplugOutDelay3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position;
				if (outDelayCounter3 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_OUT_DELAY_3);
					bootPrefs.edit().putInt("FAST_HOTPLUG_OUT_DELAY_3", toApply).commit();
				} else {
					outDelayCounter3++;
				}
			}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
		});

		Spinner fastHotplugOutDelay1 = (Spinner) findViewById(R.id.fast_hotplug_out_delay_spinner_1);

		ArrayAdapter<String> outDelayAdapter1 = new ArrayAdapter<String>(this, R.layout.spinner_row, delayLevels);
		fastHotplugOutDelay1.setAdapter(outDelayAdapter1);
		int outDelay1 = Integer.parseInt(CPUHelper.readOneLineNotRoot(FAST_HOTPLUG_OUT_DELAY_1));
		if(outDelay1 < 0)
			outDelay1 = 0;
		if(outDelay1 > 20)
			outDelay1 = 20;	
		fastHotplugOutDelay1.setSelection(outDelay1);

		fastHotplugOutDelay1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int toApply = position;
				if (outDelayCounter1 > 0) {
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + toApply + " > " + FAST_HOTPLUG_OUT_DELAY_1);
					bootPrefs.edit().putInt("FAST_HOTPLUG_OUT_DELAY_1", toApply).commit();
				} else {
					outDelayCounter1++;
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
	    LinearLayout mCardFastPlugNumberCores, mCardFastBoostDuration, mCardFastThresholdBoost, mCardIdleThreshold, mCardScreenOff, mCardInThresholds, mCardInDelays, mCardOutThresholds, mCardOutDelays;

	    mCardFastPlugNumberCores = (LinearLayout) findViewById(R.id.card_fast_hotplug_number_of_cores);
	    mCardFastBoostDuration = (LinearLayout) findViewById(R.id.card_fast_hotplug_boost_duration);
	    mCardFastThresholdBoost = (LinearLayout) findViewById(R.id.card_fast_hotplug_threshold_to_boost);
	    mCardIdleThreshold = (LinearLayout) findViewById(R.id.card_fast_hotplug_idle_threshold);
	    mCardInThresholds = (LinearLayout) findViewById(R.id.card_fast_hotplug_in_threshold);
	    mCardInDelays = (LinearLayout) findViewById(R.id.card_fast_hotplug_in_delay);
	    mCardOutThresholds = (LinearLayout) findViewById(R.id.card_fast_hotplug_out_threshold);
	    mCardOutDelays = (LinearLayout) findViewById(R.id.card_fast_hotplug_out_delay);
	    mCardScreenOff = (LinearLayout) findViewById(R.id.card_fast_hotplug_screen_off_singlecore);

	    mCardFastPlugNumberCores.setVisibility(View.VISIBLE);
	    mCardFastBoostDuration.setVisibility(View.VISIBLE);
	    mCardFastThresholdBoost.setVisibility(View.VISIBLE);
	    mCardIdleThreshold.setVisibility(View.VISIBLE);
	    mCardInThresholds.setVisibility(View.VISIBLE);
	    mCardInDelays.setVisibility(View.VISIBLE);
	    mCardOutThresholds.setVisibility(View.VISIBLE);
	    mCardOutDelays.setVisibility(View.VISIBLE);
	    mCardScreenOff.setVisibility(View.VISIBLE);
    }
    private void hideFastHotplugConfig(){
	    LinearLayout mCardFastPlugNumberCores, mCardFastBoostDuration, mCardFastThresholdBoost, mCardIdleThreshold, mCardScreenOff, mCardInThresholds, mCardInDelays, mCardOutThresholds, mCardOutDelays;

	    mCardFastPlugNumberCores = (LinearLayout) findViewById(R.id.card_fast_hotplug_number_of_cores);
	    mCardFastBoostDuration = (LinearLayout) findViewById(R.id.card_fast_hotplug_boost_duration);
	    mCardFastThresholdBoost = (LinearLayout) findViewById(R.id.card_fast_hotplug_threshold_to_boost);
	    mCardIdleThreshold = (LinearLayout) findViewById(R.id.card_fast_hotplug_idle_threshold);
	    mCardInThresholds = (LinearLayout) findViewById(R.id.card_fast_hotplug_in_threshold);
	    mCardInDelays = (LinearLayout) findViewById(R.id.card_fast_hotplug_in_delay);
	    mCardOutThresholds = (LinearLayout) findViewById(R.id.card_fast_hotplug_out_threshold);
	    mCardOutDelays = (LinearLayout) findViewById(R.id.card_fast_hotplug_out_delay);
	    mCardScreenOff = (LinearLayout) findViewById(R.id.card_fast_hotplug_screen_off_singlecore);

	    mCardFastPlugNumberCores.setVisibility(View.GONE);
	    mCardFastBoostDuration.setVisibility(View.GONE);
	    mCardFastThresholdBoost.setVisibility(View.GONE);
	    mCardIdleThreshold.setVisibility(View.GONE);
	    mCardInThresholds.setVisibility(View.GONE);
	    mCardInDelays.setVisibility(View.GONE);
	    mCardOutThresholds.setVisibility(View.GONE);
	    mCardOutDelays.setVisibility(View.GONE);
	    mCardScreenOff.setVisibility(View.GONE);
	    }

}
