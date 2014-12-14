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

package com.androguide.honamicontrol.kernel.misc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.androguide.honamicontrol.R;
import com.androguide.honamicontrol.cards.CardSeekBarVibrator;
import com.androguide.honamicontrol.cards.CardSpinner;
import com.androguide.honamicontrol.helpers.CPUHelper;
import com.androguide.honamicontrol.helpers.Helpers;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardTextStripe;
import com.fima.cardsui.views.CardUI;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.CompoundButton;
import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LinePoint;

import java.util.ArrayList;
import java.util.Collections;

public class MiscActivity extends ActionBarActivity implements MiscInterface {


    private static LineGraph graph;
    private static Line line;
    private static int currX = 0;
    private static int counter = 0;
    private static TextView mCurFreq;
    private TextView mMaxNR;
    private TextView mMinNR;

    private CurNRThread mCurNRThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setIcon(getResources().getDrawable(R.drawable.ic_tools_misc));
        setContentView(R.layout.card_misc);

	    final LinearLayout mCardNRGraph;

	    mCardNRGraph = (LinearLayout) findViewById(R.id.card_show_nr_graph);

        final SharedPreferences bootPrefs = getSharedPreferences("BOOT_PREFS", 0);
        CardUI cardsUI = (CardUI) findViewById(R.id.cardsui);



	Switch enableShowNR = (Switch) findViewById(R.id.enable_show_nr_switch);

 		int showNRState = Integer.parseInt(CPUHelper.readOneLineNotRoot(ENABLE_SHOW_NR));

		if (showNRState == 0) {
			enableShowNR.setChecked(false);
		} else if (showNRState == 1) {
			enableShowNR.setChecked(true);
			mCardNRGraph.setVisibility(View.VISIBLE);
		}

		enableShowNR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isOn) {

				if (isOn) {
					mCardNRGraph.setVisibility(View.VISIBLE);
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 1 > " + ENABLE_SHOW_NR);

				} else {
					mCardNRGraph.setVisibility(View.GONE);
					Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 0 > " + ENABLE_SHOW_NR);
				}
			}
		});


        graph = (LineGraph) findViewById(R.id.graph);
        line = new Line();
        LinePoint point = new LinePoint();
        point.setX(currX);
        point.setY(1);
        line.addPoint(point);
        line.setColor(Color.parseColor("#FFBB33"));
        graph.addLine(line);
        graph.setLineToFill(0);

        mCurFreq = (TextView) findViewById(R.id.currspeed);
        mMaxNR = (TextView) findViewById(R.id.max_nr);
        mMinNR = (TextView) findViewById(R.id.min_nr);


	minMaxNRPolling();


        cardsUI.addStack(new CardStack(""));
        cardsUI.addStack(new CardStack(""));
        if (Helpers.doesFileExist(VIBRATOR_SYSFS))
            cardsUI.addCard(new CardSeekBarVibrator(getString(R.string.vibrator_intensity), getString(R.string.vibrator_intensity_text), "#1abc9c", this));

        if (Helpers.doesFileExist(FAST_CHARGE_VERSION)) {

            // Fast Charge Warning
            cardsUI.addStack(new CardStack(getString(R.string.fast_charge_stack)));
            cardsUI.addCard(new CardTextStripe(
                            getString(R.string.fastcharge_warning),
                            getString(R.string.fastcharge_warning_text) + "\n\nFast Charge " + CPUHelper.readOneLineNotRoot(FAST_CHARGE_VERSION),
                            "#F4842D", "#F4842D", false)
            );

            final Boolean[] isManual = {false};
            int currMode = Integer.parseInt(CPUHelper.readOneLineNotRoot(FORCE_FAST_CHARGE));
            if (currMode == 2)
                isManual[0] = true;

            final View[] spinnerView = {null};

            // Fast Charge Mode
            ArrayList<String> modes = new ArrayList<String>();
            modes.add(getString(R.string.disabled));
            modes.add(getString(R.string.fast_charge_force_ac));
            modes.add(getString(R.string.manual));
            cardsUI.addCard(new CardSpinner(
                    getString(R.string.fast_charge_mode),
                    getString(R.string.fast_charge_mode_text),
                    "#1abc9c",
                    FORCE_FAST_CHARGE, Integer.parseInt(CPUHelper.readOneLineNotRoot(FORCE_FAST_CHARGE)),
                    modes,
                    this,
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            bootPrefs.edit().putInt("FASTCHARGE_MODE", i).commit();
                            isManual[0] = i == 2;

                            if (spinnerView[0] != null) {
                                if (isManual[0])
                                    spinnerView[0].setEnabled(true);
                                else if (!isManual[0])
                                    spinnerView[0].setEnabled(false);
                            }

                            Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + i + " > " + FORCE_FAST_CHARGE);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    }
            ));

            // Fast Charge Level
            final String[] amps = CPUHelper.readOneLineNotRoot(AVAILABLE_FAST_CHARGE_LEVELS).split(" ");
            ArrayList<String> amperages = new ArrayList<String>();
            Collections.addAll(amperages, amps);
            int currLevel = amperages.indexOf(CPUHelper.readOneLineNotRoot(FAST_CHARGE_LEVEL));

            cardsUI.addCard(new CardSpinner(
                    getString(R.string.fast_charge_level),
                    getString(R.string.fast_charge_level_text), "#1abc9c", FAST_CHARGE_LEVEL, currLevel, amperages, this, new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    spinnerView[0] = adapterView;

                    if (isManual[0]) {
                        Helpers.CMDProcessorWrapper.runSuCommand("busybox echo " + amps[i] + " > " + FAST_CHARGE_LEVEL);
                        bootPrefs.edit().putString("FASTCHARGE_LEVEL", amps[i]).commit();
                        adapterView.setEnabled(true);
                    } else {
                        adapterView.setEnabled(false);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            }
            ));

            if (spinnerView[0] != null) {
                if (isManual[0])
                    spinnerView[0].setEnabled(true);
                else if (!isManual[0])
                    spinnerView[0].setEnabled(false);
            }
        }

        cardsUI.refresh();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mCurNRThread == null) {
            mCurNRThread = new CurNRThread();
            mCurNRThread.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCurNRThread.isAlive()) {
            mCurNRThread.interrupt();
            try {
                mCurNRThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    // Read current frequency from /sys in a separate thread
    protected class CurNRThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(500);
                    String curFreq = "";
                    if (Helpers.doesFileExist(CURRENT_NR))
                        curFreq = CPUHelper.readOneLineNotRoot(CURRENT_NR);

                    mCurNRHandler.sendMessage(mCurNRHandler.obtainMessage(0,
                            curFreq));
                }
            } catch (InterruptedException e) {
                Log.e("CPU Thread", e.getMessage());
            }
        }
    }

    // Update real-time current frequency & stats in a separate thread
    protected static Handler mCurNRHandler = new Handler() {
        public void handleMessage(Message msg) {
            try {
                mCurFreq.setText((String) msg.obj);
                currX += 1;
                final int p = Integer.parseInt((String) msg.obj);
                counter++;
                addStatPoint(currX, p, line, graph);
                ArrayList<LinePoint> array = line.getPoints();
                if (line.getSize() > 10)
                    array.remove(0);
                line.setPoints(array);

                // Reset the line every 50 updates of the current frequency
                // to make-up for the lack of garbage collection in the
                // HoloGraph card
                if (counter == 50) {
                    graph.removeAllLines();
                    line = new Line();
                    LinePoint point = new LinePoint();
                    point.setX(currX);
                    point.setY(1);
                    line.addPoint(point);
                    line.setColor(Color.parseColor("#FFBB33"));
                    graph.addLine(line);
                    counter = 0;
                }
            } catch (NumberFormatException e) {
                Log.e("CPUHandler", e.getMessage());
            }
        }
    };

    // Static method to add new point to the graph
    public static void addStatPoint(int X, int Y, Line line, LineGraph graph) {
        LinePoint point = new LinePoint();
        point.setX(X);
        point.setY(Y);
        line.addPoint(point);
        graph.addLine(line);
    }

    public void minMaxNRPolling() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String displayedMax = CPUHelper.readOneLineNotRoot(LAST_MAX);
                String displayedMin = CPUHelper.readOneLineNotRoot(LAST_MIN);
                mMaxNR.setText(getString(R.string.max_nr) + displayedMax);
                mMinNR.setText(getString(R.string.min_nr) + displayedMin);
                minMaxNRPolling();
            }

        }, 200);
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

}
