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

public interface PowerManagementInterface {
    public static final String SCHED_MC_POWER_SAVINGS = "/sys/devices/system/cpu/sched_mc_power_savings";
    public static final String INTELLI_PLUG_TOGGLE = "/sys/module/intelli_plug/parameters/intelli_plug_active";
    public static final String INTELLI_PLUG_ECO_MODE = "/sys/module/intelli_plug/parameters/eco_mode_active";
    public static final String INTELLI_PLUG_ECO_CORES = "/sys/module/intelli_plug/parameters/eco_cores_enabled";
    public static final String ALUCARD_HOTPLUG_TOGGLE = "/sys/kernel/alucard_hotplug/hotplug_enable";
    public static final String ALUCARD_HOTPLUG_CORES = "/sys/kernel/alucard_hotplug/maxcoreslimit";
    public static final String MSM_MPDECISION_TOGGLE = "/sys/kernel/msm_mpdecision/conf/enabled";
    public static final String FAST_HOTPLUG_TOGGLE = "/sys/module/fast_hotplug/parameters/fast_hotplug_enabled";
    public static final String FAST_HOTPLUG_MIN_CORES = "/sys/module/fast_hotplug/parameters/min_cpu_on";
    public static final String FAST_HOTPLUG_MAX_CORES = "/sys/module/fast_hotplug/parameters/max_cpu_on";
    public static final String FAST_HOTPLUG_BOOST_DURATION = "/sys/module/fast_hotplug/parameters/boost_duration";
    public static final String FAST_HOTPLUG_THRESHOLD_TO_BOOST = "/sys/module/fast_hotplug/parameters/threshold_to_boost";
    public static final String FAST_HOTPLUG_IDLE_THRESHOLD = "/sys/module/fast_hotplug/parameters/idle_threshold";
//     public static final String FAST_HOTPLUG_ = "/sys/module/fast_hotplug/parameters/";
    public static final String POWER_SUSPEND_TOGGLE = "/sys/kernel/power_suspend/power_suspend_mode";
}

