package com.example.dynamicsprocessing;

import android.media.MediaPlayer;
import android.media.audiofx.DynamicsProcessing;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "DynamicsProcessingSample";

    private final String[] BAND_NAME = {"31HZ", "62HZ", "125HZ", "250HZ", "500HZ", "1KHZ", "2KHZ", "4KHZ", "8KHZ", "16KHZ"};//频段名字
    private final int[] BAND_VALUE = {31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};//频段值
    private final int[][] EFFECT_VALUES = {
            {0, 0, 0, 0, 0, 0, -6, -6, -6, -7},// 古典 Classical
            {8, 6, 2, 0, 0, -4, -6, -6, 0, 0},// 舞曲 Dance
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},// 平直 Flat
            {3, 3, 1, 2, -1, -1, 0, 1, 2, 4},// 爵士 Jazz
            {-1, 0, 0, 1, 4, 3, 1, 0, -1, 1},// 流行 Pop
            {5, 2, -3, -6, -3, 3, 6, 8, 8, 8},// 摇滚 Rock
            {-4, 0, 5, 6, 7, 6, 3, 2, 1, 0},// 现场 On site
            {0, 0, 2, 6, 6, 6, 2, 0, 0, 0},// 俱乐部 Club
            {6, 4, 6, 2, 0, 0, 0, 0, 0, 0},// 低音 Bass
            {9, 9, 9, 5, 0, 4, 11, 11, 11, 11},// 高音 Treble
            {-2, 5, 4, -2, -2, -1, 2, 3, 1, 4},// 重金属
            {10, 10, 5, -5, -3, 2, 8, 10, 11, 12},// 强劲 Strong
            {2, 0, -2, -4, -2, 2, 5, 7, 8, 9},// 轻柔 Gentle
            {2, 6, 4, 0, -2, -1, 2, 2, 1, 3},// 蓝调 Blues
            {0, 3, 0, 0, 1, 4, 5, 3, 0, 1},// 民谣 Folk
            {6, 6, 0, 0, 0, 0, 0, 0, 6, 6},// 聚会 Gather
    };
    private final int maxBandCount = BAND_VALUE.length;
    private final int mVariant = 0;
    private final int mChannelCount = 1;

    private MediaPlayer mMediaPlayer;
    private int mAudioSessionId = 0;
    private DynamicsProcessing mDynamicsProcessing;
    private static final int PRIORITY = Integer.MAX_VALUE;
    private DynamicsProcessing.Eq mDynamicsProcessingEq;
    private TextView tvInputGain;

    private final int Input_SEEKBAR_MAX_VALUE = 100;//输入增益调节范围[-50dB,50dB]
    private final int Input_MAX_VALUE = Input_SEEKBAR_MAX_VALUE / 2;

    private boolean mEqEnable = true;//均衡器开关
    private final int EQ_SEEKBAR_MAX_VALUE = 30;//均衡器调节范围[-15dB,15dB]
    private final int EQ_GAIN_MAX_VALUE = EQ_SEEKBAR_MAX_VALUE / 2;
    private TextView[] mEqTitles;
    private int[] mCurrentEqBandValues;

    private DynamicsProcessing.Mbc mDynamicsProcessingMbc;
    private DynamicsProcessing.Limiter mDynamicsProcessingLimiter;
    //Limiter常量
    private static final boolean LIMITER_DEFAULT_ENABLED = true;
    private static final int LIMITER_DEFAULT_LINK_GROUP = 0;//;
    private static final float LIMITER_DEFAULT_ATTACK_TIME = 1; // ms
    private static final float LIMITER_DEFAULT_RELEASE_TIME = 60; // ms
    private static final float LIMITER_DEFAULT_RATIO = 10; // N:1
    private static final float LIMITER_DEFAULT_THRESHOLD = -2; // dB
    private static final float LIMITER_DEFAULT_POST_GAIN = 0; // dB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMediaPlayer = MediaPlayer.create(this, R.raw.music);
        mAudioSessionId = mMediaPlayer.getAudioSessionId();
        mMediaPlayer.start();

        initDynamicsProcessing();

        initEqView();
        CheckBox eqEnableCheckBox = findViewById(R.id.eqEnableCheckBox);
        eqEnableCheckBox.setChecked(mEqEnable);
        eqEnableCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mEqEnable = isChecked;
                if (mDynamicsProcessing != null) {
                    mDynamicsProcessing.setEnabled(isChecked);
                    for (int i = 0; i < mCurrentEqBandValues.length; i++) {
                        setEqBandGain(i, mCurrentEqBandValues[i]);
                    }
                }

            }
        });

        tvInputGain = findViewById(R.id.tvInputGain);
        SeekBar sbInputGain = findViewById(R.id.sbInputGain);
        sbInputGain.setMax(Input_SEEKBAR_MAX_VALUE);
        sbInputGain.setProgress(Input_MAX_VALUE);
        sbInputGain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvInputGain.setText("InputGain:" + (progress - Input_MAX_VALUE));
                setInputGain(progress - Input_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //初始化 DynamicsProcessing 相关类
    private void initDynamicsProcessing() {
        if (mDynamicsProcessing == null) {
            DynamicsProcessing.Config.Builder builder = new DynamicsProcessing.Config.Builder(mVariant, mChannelCount, true, maxBandCount, true, maxBandCount, true, maxBandCount, true);
            mDynamicsProcessing = new DynamicsProcessing(PRIORITY, mAudioSessionId, builder.build());
            mDynamicsProcessing.setEnabled(mEqEnable);

            mDynamicsProcessingEq = new DynamicsProcessing.Eq(true, true, maxBandCount);
            mDynamicsProcessingEq.setEnabled(mEqEnable);

            mDynamicsProcessingMbc = new DynamicsProcessing.Mbc(true, true, maxBandCount);
            mDynamicsProcessingMbc.setEnabled(mEqEnable);

            mDynamicsProcessingLimiter = new DynamicsProcessing.Limiter(true, LIMITER_DEFAULT_ENABLED, LIMITER_DEFAULT_LINK_GROUP, LIMITER_DEFAULT_ATTACK_TIME, LIMITER_DEFAULT_RELEASE_TIME, LIMITER_DEFAULT_RATIO, LIMITER_DEFAULT_THRESHOLD, LIMITER_DEFAULT_POST_GAIN);
            mDynamicsProcessingLimiter.setEnabled(mEqEnable);
        }
        try {
            for (int i = 0; i < maxBandCount; i++) {
                mDynamicsProcessingEq.getBand(i).setCutoffFrequency(BAND_VALUE[i]);
                setEqBandGain(i, mCurrentEqBandValues[i]);

                mDynamicsProcessingMbc.getBand(i).setCutoffFrequency(BAND_VALUE[i]);

            }
            mDynamicsProcessing.setPreEqAllChannelsTo(mDynamicsProcessingEq);
            mDynamicsProcessing.setMbcAllChannelsTo(mDynamicsProcessingMbc);
            mDynamicsProcessing.setPostEqAllChannelsTo(mDynamicsProcessingEq);
            mDynamicsProcessing.setLimiterAllChannelsTo(mDynamicsProcessingLimiter);
        } catch (Exception e) {
            Toast.makeText(this, "Init DynamicsProcessing failed!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "initDynamicsProcessing failed:" + e.toString());
            e.printStackTrace();
        }
    }

    //初始化均衡器视图
    private void initEqView() {
        LinearLayout eqLayout = findViewById(R.id.eqLayout);
        SeekBar[] seekBars = new SeekBar[BAND_NAME.length];
        mEqTitles = new TextView[BAND_NAME.length];
        mCurrentEqBandValues = new int[BAND_VALUE.length];
        for (int i = 0; i < BAND_NAME.length; i++) {
            seekBars[i] = new SeekBar(this);
            seekBars[i].setMax(EQ_SEEKBAR_MAX_VALUE);
            seekBars[i].setProgress(EQ_GAIN_MAX_VALUE);
            final int eqSeekBarIndex = i;
            seekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mCurrentEqBandValues[eqSeekBarIndex] = progress - EQ_GAIN_MAX_VALUE;
                    mEqTitles[eqSeekBarIndex].setText(BAND_NAME[eqSeekBarIndex] + "-DB:" + mCurrentEqBandValues[eqSeekBarIndex]);
                    setEqBandGain(eqSeekBarIndex, mCurrentEqBandValues[eqSeekBarIndex]);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            mCurrentEqBandValues[i] = 0;
            mEqTitles[i] = new TextView(this);
            mEqTitles[i].setText(BAND_NAME[i] + "-DB:" + mCurrentEqBandValues[i]);
            mEqTitles[i].setTextSize(18);
            linearLayout.addView(mEqTitles[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = 50;
            linearLayout.addView(seekBars[i]);
            eqLayout.addView(linearLayout, params);
        }
    }

    //设置输入增益
    private void setInputGain(float val) {
        if (mDynamicsProcessing != null) {
            mDynamicsProcessing.setInputGainAllChannelsTo(val);
        }
    }

    //设置均衡器指定频段的增益
    private void setEqBandGain(int band, int gain) {
        if (mCurrentEqBandValues == null) {
            return;
        }
        mCurrentEqBandValues[band] = gain;
        if (mDynamicsProcessing != null && mDynamicsProcessingEq != null) {
            try {
                mDynamicsProcessingEq.getBand(band).setEnabled(true);
                mDynamicsProcessingEq.getBand(band).setGain(mCurrentEqBandValues[band]);
                mDynamicsProcessing.setPreEqBandAllChannelsTo(band, mDynamicsProcessingEq.getBand(band));
                mDynamicsProcessing.setPostEqBandAllChannelsTo(band, mDynamicsProcessingEq.getBand(band));
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "setEqBandGain failed:" + e.toString());
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            mMediaPlayer.release();
        }
        if (mDynamicsProcessing != null) {
            mDynamicsProcessing.setEnabled(false);
            mDynamicsProcessing.release();
            mDynamicsProcessing = null;
        }
    }
}