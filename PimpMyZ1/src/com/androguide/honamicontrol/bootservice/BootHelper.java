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

package com.androguide.honamicontrol.bootservice;

import android.content.Context;
import android.content.SharedPreferences;

import com.androguide.honamicontrol.helpers.CMDProcessor.CMDProcessor;
import com.androguide.honamicontrol.helpers.CPUHelper;
import com.androguide.honamicontrol.helpers.Helpers;
import com.androguide.honamicontrol.kernel.colorcontrol.ColorControlInterface;
import com.androguide.honamicontrol.kernel.cpucontrol.CPUInterface;
import com.androguide.honamicontrol.kernel.gpucontrol.GPUInterface;
import com.androguide.honamicontrol.kernel.iotweaks.IOTweaksInterface;
import com.androguide.honamicontrol.kernel.memory.MemoryManagementInterface;
import com.androguide.honamicontrol.kernel.misc.MiscInterface;
import com.androguide.honamicontrol.kernel.powermanagement.PowerManagementInterface;
import com.androguide.honamicontrol.kernel.voltagecontrol.VoltageInterface;
import com.androguide.honamicontrol.soundcontrol.SoundControlInterface;
import com.androguide.honamicontrol.touchscreen.TouchScreenInterface;
import android.util.Log;

public class BootHelper {
	private static void applyInt(SharedPreferences prefs, String key, String file_path){
		int value;
		if(prefs.contains(key)){
			// The default value should never be used
			Log.e("Honamicontrol", "Restoring preference for " + key);
			value = prefs.getInt(key, 0);
			Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + value + " > " + file_path);
		}
	}

	private static void applyBool(SharedPreferences prefs, String key, String file_path){
		Boolean value;
		if(prefs.contains(key)){
			Log.e("Honamicontrol", "Restoring preference for " + key);
			value = prefs.getBoolean(key, false);
			Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + getIntFromBoolean(value) + " > " + file_path);
		}
	}

	private static void applyString(SharedPreferences prefs, String key, String file_path){
		String value;
		if(prefs.contains(key)){
			Log.e("Honamicontrol", "Restoring preference for " + key);
			value = prefs.getString(key, "");
			Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + value + " > " + file_path);
		}
	}

    public static void generateScriptFromPrefs(SharedPreferences prefs, Context context) {
	    applyString(prefs, "CPU_MAX_FREQ", CPUInterface.MAX_FREQ);
	    applyString(prefs, "CPU_MIN_FREQ", CPUInterface.MIN_FREQ);
	    applyString(prefs, "GPU_MAX_FREQ", GPUInterface.maxFreq);
	    applyString(prefs, "GPU_MIN_FREQ", GPUInterface.minFreq);
	    applyInt(prefs, "SCHED_MC_LEVEL", PowerManagementInterface.SCHED_MC_POWER_SAVINGS);
	    applyInt(prefs, MemoryManagementInterface.KSM_PAGES_TO_SCAN.replaceAll("/", "_"), MemoryManagementInterface.KSM_PAGES_TO_SCAN);
	    applyInt(prefs, MemoryManagementInterface.KSM_SLEEP_TIMER.replaceAll("/", "_"), MemoryManagementInterface.KSM_SLEEP_TIMER);
	    applyInt(prefs, "FASTCHARGE_MODE", MiscInterface.FORCE_FAST_CHARGE);
	    applyBool(prefs, "DYNAMIC_FSYNC", IOTweaksInterface.DYNAMIC_FSYNC_TOGGLE);
	    applyBool(prefs, "INTELLI_PLUG_ECO", PowerManagementInterface.INTELLI_PLUG_ECO_MODE);
	    applyBool(prefs, "POWER_SUSPEND", PowerManagementInterface.POWER_SUSPEND_TOGGLE);
	    applyBool(prefs, "PEN_MODE", TouchScreenInterface.PEN_MODE);
	    applyBool(prefs, "GLOVE_MODE", TouchScreenInterface.GLOVE_MODE);
	    applyBool(prefs, "DT2WAKE", TouchScreenInterface.DT2WAKE);
	    applyBool(prefs, "KSM_ENABLED", MemoryManagementInterface.KSM_TOGGLE);
	    applyBool(prefs, "EMMC_ENTROPY_CONTRIB", IOTweaksInterface.EMMC_ENTROPY_CONTRIB);
	    applyBool(prefs, "SD_ENTROPY_CONTRIB", IOTweaksInterface.SD_ENTROPY_CONTRIB);
	    applyString(prefs, "CORE0_GOVERNOR", CPUInterface.GOVERNOR);
	    applyString(prefs, "CORE1_GOVERNOR", CPUInterface.GOVERNOR2);
	    applyString(prefs, "CORE2_GOVERNOR", CPUInterface.GOVERNOR3);
	    applyString(prefs, "CORE3_GOVERNOR", CPUInterface.GOVERNOR3);
	    applyString(prefs, "GPU_GOVERNOR", GPUInterface.currGovernor);
	    applyString(prefs, "IO_SCHEDULER", IOTweaksInterface.IO_SCHEDULER);
	    applyString(prefs, "IO_SCHEDULER_SD", IOTweaksInterface.IO_SCHEDULER_SD);
	    applyString(prefs, "EMMC_READAHEAD", IOTweaksInterface.EMMC_READAHEAD);
	    applyString(prefs, "SD_READAHEAD", IOTweaksInterface.SD_READAHEAD);
	    if(Helpers.doesFileExist(SoundControlInterface.FAUX_SC_LOCKED)){
		    CMDProcessor.runSuCommand("echo 0 > " + SoundControlInterface.FAUX_SC_LOCKED);
		    applyString(prefs, "SC_MIC", SoundControlInterface.FAUX_SC_MIC);
		    applyString(prefs, "SC_CAM_MIC", SoundControlInterface.FAUX_SC_CAM_MIC);
		    applyString(prefs, "HEADPHONE_PA", SoundControlInterface.FAUX_SC_HEADPHONE_POWERAMP);
		    applyString(prefs, "HEADPHONE", SoundControlInterface.FAUX_SC_HEADPHONE);
		    applyString(prefs, "SPEAKER", SoundControlInterface.FAUX_SC_SPEAKER);
		    CMDProcessor.runSuCommand("echo 1 > " + SoundControlInterface.FAUX_SC_LOCKED);
	    }
	    applyString(prefs, "FASTCHARGE_LEVEL", MiscInterface.FAST_CHARGE_LEVEL);
	    applyString(prefs, "KCAL_CONFIG", ColorControlInterface.GAMMA_OK);
	    applyString(prefs, "CURRENT_VOLTAGE_TABLE", VoltageInterface.UV_MV_TABLE);

        // Governor Customization
	    SharedPreferences govPrefs = context.getSharedPreferences("GOVERNOR_CUSTOMIZATION", 0);
	    if(govPrefs.contains("TARGET_GOV")){
		    String TARGET_GOV = govPrefs.getString("TARGET_GOV", CPUHelper.readOneLineNotRoot(CPUInterface.GOVERNOR_ALL_CORES));
		    if (Helpers.doesFileExist(CPUInterface.GOV_CUSTOMIZATION + "/" + TARGET_GOV)) {
			    String[] paramsList = CMDProcessor.runShellCommand("ls " + CPUInterface.GOV_CUSTOMIZATION + "/" + TARGET_GOV)
				    .getStdout().split("\n");

			    String commands = "";

			    for (final String param : paramsList) {
				    String key = govPrefs.getString(param, "null");
				    if (!key.equals("null"))
					    commands += key + "\n";
			    }

			    CMDProcessor.runSuCommand(commands);
		    }
	    }

	if(prefs.contains("SNAKE_CHARMER")){
		Boolean SNAKE_CHARMER = prefs.getBoolean("SNAKE_CHARMER", true);
		
		// This line might still cause some incompatibility with other devices
		int CPU_MAX_FREQ = Integer.valueOf(prefs.getString("CPU_MAX_FREQ", "2150400"));

		if (SNAKE_CHARMER)
		CMDProcessor.runSuCommand("\nbusybox echo " + CPU_MAX_FREQ + " > " + CPUInterface.SNAKE_CHARMER_MAX_FREQ);
	}
	if(prefs.contains("MSM_THERMAL")){
		Boolean MSM_THERMAL = prefs.getBoolean("MSM_THERMAL", false);
		if (MSM_THERMAL)
			CMDProcessor.runSuCommand("busybox echo Y > " + CPUInterface.MSM_THERMAL);
		else
			CMDProcessor.runSuCommand("busybox echo N > " + CPUInterface.MSM_THERMAL);
	}

	if(prefs.contains("TCP_ALGORITHM")){
		String TCP_ALGORITHM = prefs.getString("TCP_ALGORITHM", "cubic");
		CMDProcessor.runSuCommand("busybox echo " + TCP_ALGORITHM + " > " + CPUInterface.CURR_TCP_ALGORITHM + "\n" + CPUInterface.SYSCTL_TCP_ALGORITHM + TCP_ALGORITHM);
	}

	if(prefs.contains("HOTPLUG_DRIVER")){
		int HOTPLUG_DRIVER = prefs.getInt("HOTPLUG_DRIVER", 0);
		switch (HOTPLUG_DRIVER) {
			case 0:
				if(Helpers.doesFileExist(PowerManagementInterface.INTELLI_PLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.INTELLI_PLUG_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.ALUCARD_HOTPLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.ALUCARD_HOTPLUG_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.MSM_MPDECISION_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.MSM_MPDECISION_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.FAST_HOTPLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.FAST_HOTPLUG_TOGGLE);
				}
				CMDProcessor.runSuCommand("start mpdecision");
				break;
			case 1:
				if(Helpers.doesFileExist(PowerManagementInterface.ALUCARD_HOTPLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.ALUCARD_HOTPLUG_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.MSM_MPDECISION_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.MSM_MPDECISION_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.FAST_HOTPLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.FAST_HOTPLUG_TOGGLE);
				}
				CMDProcessor.runSuCommand("stop mpdecision");
				CMDProcessor.runSuCommand("echo 1 > " + PowerManagementInterface.INTELLI_PLUG_TOGGLE);
				applyInt(prefs, "INTELLI_PLUG_ECO_CORES", PowerManagementInterface.INTELLI_PLUG_ECO_CORES);
				break;
			case 2:
				if(Helpers.doesFileExist(PowerManagementInterface.INTELLI_PLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.INTELLI_PLUG_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.MSM_MPDECISION_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.MSM_MPDECISION_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.FAST_HOTPLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.FAST_HOTPLUG_TOGGLE);
				}
				CMDProcessor.runSuCommand("stop mpdecision");
				CMDProcessor.runSuCommand("echo 1 > " + PowerManagementInterface.ALUCARD_HOTPLUG_TOGGLE);
				applyInt(prefs, "ALUCARD_CORES", PowerManagementInterface.ALUCARD_HOTPLUG_CORES);
				break;
			case 3:
				if(Helpers.doesFileExist(PowerManagementInterface.INTELLI_PLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.INTELLI_PLUG_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.ALUCARD_HOTPLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.ALUCARD_HOTPLUG_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.FAST_HOTPLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.FAST_HOTPLUG_TOGGLE);
				}
				CMDProcessor.runSuCommand("stop mpdecision");
				CMDProcessor.runSuCommand("echo 1 > " + PowerManagementInterface.MSM_MPDECISION_TOGGLE);
				break;
			case 4:
				if(Helpers.doesFileExist(PowerManagementInterface.INTELLI_PLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.INTELLI_PLUG_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.ALUCARD_HOTPLUG_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.ALUCARD_HOTPLUG_TOGGLE);
				}
				if(Helpers.doesFileExist(PowerManagementInterface.MSM_MPDECISION_TOGGLE)){
					CMDProcessor.runSuCommand("echo 0 > " + PowerManagementInterface.MSM_MPDECISION_TOGGLE);
				}
				CMDProcessor.runSuCommand("stop mpdecision");
				CMDProcessor.runSuCommand("echo 1 > " + PowerManagementInterface.FAST_HOTPLUG_TOGGLE);
				applyInt(prefs, "FAST_HOTPLUG_MIN_CORES", PowerManagementInterface.FAST_HOTPLUG_MIN_CORES);
				applyInt(prefs, "FAST_HOTPLUG_MAX_CORES", PowerManagementInterface.FAST_HOTPLUG_MAX_CORES);
				applyInt(prefs, "FAST_HOTPLUG_BOOST_DURATION", PowerManagementInterface.FAST_HOTPLUG_BOOST_DURATION);
				applyInt(prefs, "FAST_HOTPLUG_THRESHOLD_TO_BOOST", PowerManagementInterface.FAST_HOTPLUG_THRESHOLD_TO_BOOST);
				applyInt(prefs, "FAST_HOTPLUG_IDLE_THRESHOLD", PowerManagementInterface.FAST_HOTPLUG_IDLE_THRESHOLD);
				applyBool(prefs, "FAST_HOTPLUG_SCREEN_OFF_SINGLECORE", PowerManagementInterface.FAST_HOTPLUG_SCREEN_OFF_SINGLECORE);
				applyInt(prefs, "FAST_HOTPLUG_IN_THRESHOLD_1", PowerManagementInterface.FAST_HOTPLUG_IN_THRESHOLD_1);
				applyInt(prefs, "FAST_HOTPLUG_IN_THRESHOLD_2", PowerManagementInterface.FAST_HOTPLUG_IN_THRESHOLD_2);
				applyInt(prefs, "FAST_HOTPLUG_IN_THRESHOLD_3", PowerManagementInterface.FAST_HOTPLUG_IN_THRESHOLD_3);
				applyInt(prefs, "FAST_HOTPLUG_IN_DELAY_1", PowerManagementInterface.FAST_HOTPLUG_IN_DELAY_1);
				applyInt(prefs, "FAST_HOTPLUG_IN_DELAY_2", PowerManagementInterface.FAST_HOTPLUG_IN_DELAY_2);
				applyInt(prefs, "FAST_HOTPLUG_IN_DELAY_3", PowerManagementInterface.FAST_HOTPLUG_IN_DELAY_3);
				applyInt(prefs, "FAST_HOTPLUG_OUT_THRESHOLD_1", PowerManagementInterface.FAST_HOTPLUG_OUT_THRESHOLD_1);
				applyInt(prefs, "FAST_HOTPLUG_OUT_THRESHOLD_2", PowerManagementInterface.FAST_HOTPLUG_OUT_THRESHOLD_2);
				applyInt(prefs, "FAST_HOTPLUG_OUT_THRESHOLD_3", PowerManagementInterface.FAST_HOTPLUG_OUT_THRESHOLD_3);
				applyInt(prefs, "FAST_HOTPLUG_OUT_DELAY_1", PowerManagementInterface.FAST_HOTPLUG_OUT_DELAY_1);
				applyInt(prefs, "FAST_HOTPLUG_OUT_DELAY_2", PowerManagementInterface.FAST_HOTPLUG_OUT_DELAY_2);
				applyInt(prefs, "FAST_HOTPLUG_OUT_DELAY_3", PowerManagementInterface.FAST_HOTPLUG_OUT_DELAY_3);
				break;
			default:
				break;
		}
	}


	if(prefs.contains(MemoryManagementInterface.VFS_CACHE_PRESSURE)){
		int VFS_CACHE_PRESSURE = prefs.getInt(MemoryManagementInterface.VFS_CACHE_PRESSURE, 100);
		Helpers.applySysctlValue(MemoryManagementInterface.VFS_CACHE_PRESSURE, VFS_CACHE_PRESSURE + "");
	}

	if(prefs.contains(MemoryManagementInterface.SWAPPINESS)){
		int SWAPPINESS = prefs.getInt(MemoryManagementInterface.SWAPPINESS, 60);
		Helpers.applySysctlValue(MemoryManagementInterface.SWAPPINESS, SWAPPINESS + "");
	}

	if(prefs.contains(MemoryManagementInterface.DIRTY_RATIO)){
		int DIRTY_RATIO = prefs.getInt(MemoryManagementInterface.DIRTY_RATIO, 20);
		Helpers.applySysctlValue(MemoryManagementInterface.DIRTY_RATIO, DIRTY_RATIO + "");
	}

	if(prefs.contains(MemoryManagementInterface.DIRTY_BG_RATIO)){
		int DIRTY_BG_RATIO = prefs.getInt(MemoryManagementInterface.DIRTY_BG_RATIO, 5);
		Helpers.applySysctlValue(MemoryManagementInterface.DIRTY_BG_RATIO, DIRTY_BG_RATIO + "");
	}

	if(prefs.contains(MemoryManagementInterface.DIRTY_WRITEBACK_CENTISECS)){
		int DIRTY_WRITEBACK = prefs.getInt(MemoryManagementInterface.DIRTY_WRITEBACK_CENTISECS, 500);
		Helpers.applySysctlValue(MemoryManagementInterface.DIRTY_WRITEBACK_CENTISECS, DIRTY_WRITEBACK + "");
	}

	if(prefs.contains(MemoryManagementInterface.DIRTY_EXPIRE_CENTISECS)){
		int DIRTY_EXPIRE = prefs.getInt(MemoryManagementInterface.DIRTY_EXPIRE_CENTISECS, 200);
		Helpers.applySysctlValue(MemoryManagementInterface.DIRTY_EXPIRE_CENTISECS, DIRTY_EXPIRE + "");
	}
    }

    private static int getIntFromBoolean(Boolean bool) {
        if (bool)
            return 1;
        else
            return 0;
    }
}
