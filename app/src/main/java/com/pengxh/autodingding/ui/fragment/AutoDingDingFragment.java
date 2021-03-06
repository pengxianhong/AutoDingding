package com.pengxh.autodingding.ui.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.gyf.immersionbar.ImmersionBar;
import com.jzxiang.pickerview.TimePickerDialog;
import com.jzxiang.pickerview.data.Type;
import com.pengxh.app.multilib.utils.BroadcastManager;
import com.pengxh.app.multilib.utils.ColorUtil;
import com.pengxh.app.multilib.utils.LogToFile;
import com.pengxh.app.multilib.widget.EasyToast;
import com.pengxh.autodingding.BaseFragment;
import com.pengxh.autodingding.R;
import com.pengxh.autodingding.ui.WelcomeActivity;
import com.pengxh.autodingding.utils.Constant;
import com.pengxh.autodingding.utils.SQLiteUtil;
import com.pengxh.autodingding.utils.SendMailUtil;
import com.pengxh.autodingding.utils.StatusBarColorUtil;
import com.pengxh.autodingding.utils.TimeOrDateUtil;
import com.pengxh.autodingding.utils.Utils;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;

public class AutoDingDingFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = "AutoDingDingFragment";

    @BindView(R.id.mTitleLeftView)
    ImageView mTitleLeftView;
    @BindView(R.id.mTitleView)
    TextView mTitleView;
    @BindView(R.id.mTitleRightView)
    ImageView mTitleRightView;
    @BindView(R.id.currentTime)
    TextView currentTime;
    @BindView(R.id.startTimeView)
    TextView startTimeView;
    @BindView(R.id.endTimeView)
    TextView endTimeView;
    @BindView(R.id.amTime)
    TextView amTime;
    @BindView(R.id.pmTime)
    TextView pmTime;

    private FragmentManager fragmentManager;
    private BroadcastManager broadcastManager;
    private FragmentActivity activity;
    private CountDownTimer amCountDownTimer, pmCountDownTimer;

    @Override
    protected int initLayoutView() {
        return R.layout.fragment_day;
    }

    @Override
    protected void initData() {
        activity = getActivity();

        mTitleLeftView.setVisibility(View.GONE);
        mTitleView.setText("自动打卡");
        mTitleRightView.setVisibility(View.GONE);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                String systemTime = TimeOrDateUtil.timestampToTime(System.currentTimeMillis());
                currentTime.post(() -> currentTime.setText(systemTime));
            }
        }, 0, 1000);

        fragmentManager = activity.getSupportFragmentManager();
        broadcastManager = BroadcastManager.getInstance(activity);
    }

    @Override
    protected void initEvent() {
        broadcastManager.addAction(Constant.DINGDING_ACTION, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals(Constant.DINGDING_ACTION)) {
                    String message = intent.getStringExtra("data");
                    Log.d(TAG, "接收到广播, 通知内容: " + message);
                    LogToFile.d(TAG, "接收到广播, 通知内容: " + message);
                    //保存打卡记录
                    SQLiteUtil.getInstance().saveHistory(Utils.uuid(), TimeOrDateUtil.rTimestampToDate(System.currentTimeMillis()), message);
                    broadcastManager.sendBroadcast(Constant.ACTION_UPDATE, "update");
                    //回到主页
                    Message msg = handler.obtainMessage();
                    msg.what = 110;
                    msg.obj = message;
                    handler.sendMessage(msg);
                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 110) {
                Log.d(TAG, "回主页");
                LogToFile.d(TAG, "回主页");
                Intent intent = new Intent(activity, WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                String emailAddress = Utils.readEmailAddress();
                if (emailAddress.equals("")) {
                    Log.d(TAG, "邮箱地址为空");
                    LogToFile.d(TAG, "邮箱地址为空");
                } else {
                    String message = (String) msg.obj;
                    if (message == null || message.equals("")) {
                        Log.d(TAG, "邮件内容为空");
                        LogToFile.d(TAG, "邮件内容为空");
                    } else {
                        //发送打卡成功的邮件
                        Log.d(TAG, "邮箱地址: " + emailAddress + ", 邮件内容： " + message);
                        LogToFile.d(TAG, "邮箱地址: " + emailAddress + ", 邮件内容： " + message);
                        SendMailUtil.send(emailAddress, message);
                    }
                }
            }
        }
    };

    @Override
    public void initImmersionBar() {
        StatusBarColorUtil.setColor(activity, Color.parseColor("#0094FF"));
        ImmersionBar.with(this).init();
    }

    @OnClick({R.id.startLayoutView, R.id.endLayoutView, R.id.endAmDuty, R.id.endPmDuty})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startLayoutView:
                //设置上班时间
                new TimePickerDialog.Builder().setThemeColor(ColorUtil.getRandomColor())
                        .setWheelItemTextSize(15)
                        .setCyclic(false)
                        .setMinMillseconds(System.currentTimeMillis())
                        .setMaxMillseconds(System.currentTimeMillis() + Constant.ONE_MONTH)
                        .setType(Type.ALL)
                        .setCallBack((timePickerView, millSeconds) -> {
                            amTime.setText(TimeOrDateUtil.timestampToDate(millSeconds));
                            //计算时间差
                            onDuty(millSeconds);
                        }).build().show(fragmentManager, "year_month_day_hour_minute");
                break;
            case R.id.endLayoutView:
                //设置下班时间
                new TimePickerDialog.Builder().setThemeColor(ColorUtil.getRandomColor())
                        .setWheelItemTextSize(15)
                        .setCyclic(false)
                        .setMinMillseconds(System.currentTimeMillis())
                        .setMaxMillseconds(System.currentTimeMillis() + Constant.ONE_MONTH)
                        .setType(Type.ALL)
                        .setCallBack((timePickerView, millSeconds) -> {
                            pmTime.setText(TimeOrDateUtil.timestampToDate(millSeconds));
                            //计算时间差
                            offDuty(millSeconds);
                        }).build().show(fragmentManager, "year_month_day_hour_minute");
                break;
            case R.id.endAmDuty:
                if (amCountDownTimer != null) {
                    amCountDownTimer.cancel();
                    startTimeView.setText("--");
                }
                break;
            case R.id.endPmDuty:
                if (pmCountDownTimer != null) {
                    pmCountDownTimer.cancel();
                    endTimeView.setText("--");
                }
                break;
            default:
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void onDuty(long millSeconds) {
        long deltaTime = TimeOrDateUtil.deltaTime(millSeconds / 1000);
        if (deltaTime == 0) {
            LogToFile.w(TAG, String.valueOf(new Throwable()));
            return;
        }
        //显示倒计时
        String text = startTimeView.getText().toString();

        if (text.equals("--")) {
            amCountDownTimer = new CountDownTimer(deltaTime * 1000, 1000) {
                @Override
                public void onTick(long l) {
                    startTimeView.setText((int) (l / 1000) + "s");
                }

                @Override
                public void onFinish() {
                    startTimeView.setText("--");
                    Utils.openDingDing(Constant.DINGDING);
                }
            };
            amCountDownTimer.start();
        } else {
            EasyToast.showToast("已有任务在进行中", EasyToast.WARING);
        }
    }

    @SuppressLint("SetTextI18n")
    private void offDuty(long millSeconds) {
        long deltaTime = TimeOrDateUtil.deltaTime(millSeconds / 1000);
        if (deltaTime == 0) {
            return;
        }
        //显示倒计时
        String text = endTimeView.getText().toString();
        if (text.equals("--")) {
            pmCountDownTimer = new CountDownTimer(deltaTime * 1000, 1000) {
                @Override
                public void onTick(long l) {
                    endTimeView.setText((int) (l / 1000) + "s");
                }

                @Override
                public void onFinish() {
                    endTimeView.setText("--");
                    Utils.openDingDing(Constant.DINGDING);
                }
            };
            pmCountDownTimer.start();
        } else {
            EasyToast.showToast("已有任务在进行中", EasyToast.WARING);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        broadcastManager.destroy(Constant.DINGDING_ACTION);
    }
}