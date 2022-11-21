package com.dji.sdk.sample.demo.flightcontroller;

import org.jetbrains.annotations.NotNull;

import com.dji.sdk.sample.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import dc.android.base.activity.BridgeActivity;
import dc.common.Logger;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.realname.AircraftBindingState;
import dji.common.realname.AppActivationState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKManager;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

/**
 * 飞行控制
 * https://developer.dji.com/api-reference/android-api/Components/FlightController/DJIFlightController.html
 *
 * @author senrsl
 * @ClassName: FlightActivity
 * @Package: com.dji.sdk.sample.demo.flightcontroller
 * @CreateTime: 2022/11/18 14:54
 */
public class FlightActivity extends BridgeActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, FlightActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void initOnCreate() {
        if (checkDroneConnection()) super.initOnCreate();
    }

    /*
CompletionCallback ：仅包括 onResult(DJIError  error) 抽象方法。当 error 为空时，任务执行成功；当 error 不为空时，通过其 getDescription() 方法详细错误信息。

CompletionCallbackWith<T>：包括 onSuccess(T  val) 和 onFailure(DJIError  error) 两个方法。当任务执行成功时，回调 onSuccess(…) 方法，且返回参数 val ；当任务执行失败时，回调 onFailure(…) 方法，且其中的 error 对象包含了错误描述。

CompletionCallbackWithTwoParam<X, Y>：与 CompletionCallbackWith<T> 类似，包括 onSuccess(X val1, Y val2) 和 onFailure(DJIError  error) 两个方法，方法执行成功时返回 val1 和 val2 两个参数。
     */

    @Override
    protected void initLayout() {
        super.initLayout();
        setLayout(true, R.layout.activity_flight, true, Color.WHITE);

        //起飞
        findViewById(R.id.btn_takeoff).setOnClickListener(v -> takeoff());
        //取消起飞
        findViewById(R.id.btn_takeoff_cancel).setOnClickListener(v -> cancelTakeoff());
        //降落
        findViewById(R.id.btn_landing).setOnClickListener(v -> landing());
        //取消降落
        findViewById(R.id.btn_landing_cancel).setOnClickListener(v -> cancelLanding());
        //设置返航高度
        findViewById(R.id.btn_set_home_height).setOnClickListener(v -> setHomeHeight());
        //获取返航高度
        findViewById(R.id.btn_get_home_height).setOnClickListener(v -> getHomeHeight());
        //返航
        findViewById(R.id.btn_home).setOnClickListener(v -> home());
        //取消返航
        findViewById(R.id.btn_home_cancel).setOnClickListener(v -> cancelHome());
    }

    @Override
    protected void initData() {
        super.initData();
    }

//    //在主线程中显示提示
//    private void showToast(final String toastMsg) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
//            }
//        });
//    }

    private boolean checkDroneConnection() {
        //应用程序激活管理器
        AppActivationManager mgrActivation = DJISDKManager.getInstance().getAppActivationManager();

        //判断应用程序是否注册
        if (!DJISDKManager.getInstance().hasSDKRegistered()) {
            showToast("应用程序未注册");
            return false;
        }

        //判断应用程序是否激活
        if (mgrActivation.getAppActivationState() != AppActivationState.ACTIVATED) {
            showToast("应用程序未激活");
            return false;
        }

        //判断无人机是否绑定
//        if (mgrActivation.getAircraftBindingState() != AircraftBindingState.BOUND) {
//            showToast("无人机未绑定");
//            return false;
//        }

        //判断无人机是否连接
        //if ((DJISDKManager.getInstance().getProduct() == null) || !(DJISDKManager.getInstance().getProduct().isConnected())) {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product == null || !product.isConnected()) {
            showToast("无人机连接失败");
            return false;
        }
        return true;
    }


    //获取无人机的飞行控制器
    private FlightController getFlightController() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                return ((Aircraft) product).getFlightController();
            }
        }
        return null;
    }


    //起飞
    private void takeoff() {
        FlightController flightController = getFlightController();
        if (flightController != null) {

            //飞行控制器的相关状态类包括飞行控制器状态类（FlightControllerState）、惯性控制单元状态类（IMUState）和重心状态类（GravityCenterState）
            //        FlightControllerState：获取电机状态、无人机位置、无人机姿态、飞行时间、飞行模式、定位卫星状态、电量状态、返航点状态、超声波避障状态、IMU预热状态等飞行控制与状态信息。
            //        IMUState：获取惯性测量单元的状态信息，包括陀螺仪状态、加速度计状态、IMU校准进度与状态等。
            //        GravityCenterState：获取重心较准的状态。重心校准可通过飞行控制器类的startGravityCenterCalibration 和 stopGravityCenterCalibration 方法启动与停止。

            //飞行状态监听
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull @NotNull FlightControllerState flightControllerState) {
                    //3D位置
                    LocationCoordinate3D locationCoordinate3D = flightControllerState.getAircraftLocation();
                    if (null != locationCoordinate3D)
                        Logger.w("纬度 " + locationCoordinate3D.getLatitude(), "经度" + locationCoordinate3D.getLongitude(), "高度" + locationCoordinate3D.getAltitude());
                }
            });

            //遥控器的相关状态类包括遥控器的硬件状态类（HardwareState）、主辅遥控器状态类（MasterSlaveState）、对焦控制器状态类（FocusControllerState）和一控多机配对状态类（MultiDevicesPairingState）等
            //       硬件状态类（HardwareState）：获取遥控器上各个遥感（Stick）、转盘/拨轮（Dial）、按钮（Button）的状态。
            //       主辅遥控器状态类（MasterSlaveState）：在支持双遥控器的机型（例如悟2、御Pro等），获取遥控器的主辅状态等信息。双遥控器模式可供两个人（飞手和云台手）控制同一台飞机。
            //       对焦控制器状态类（FocusControllerState）：获取遥控器远程对焦状态。
            // @link PushRemoteControllerDataView


            //起飞
            flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Logger.w(getClass().getName(), djiError, djiError.getErrorCode(), djiError.getDescription());
                        showToast(djiError.toString());
                    } else {
                        showToast("开始起飞！");
                    }
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行控制器是否连接正常！");
        }
    }

    //取消起飞
    private void cancelTakeoff() {
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.cancelTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast(djiError.toString());
                    } else {
                        showToast("取消起飞成功！");
                    }
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行控制器是否连接正常！");
        }
    }


    //降落
    private void landing() {
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast("开始降落！");
                    } else {
                        showToast(djiError.toString());
                    }
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行控制器是否连接正常！");
        }
    }

    //取消降落
    private void cancelLanding() {
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.cancelLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast("取消降落成功！");
                    } else {
                        showToast(djiError.toString());
                    }
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行控制器是否连接正常！");
        }
    }


    //返航
    private void home() {
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast("开始返航！");
                    } else {
                        showToast(djiError.toString());
                    }
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行控制器是否连接正常！");
        }
    }

    //取消返航
    private void cancelHome() {
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.cancelGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast("取消返航成功！");
                    } else {
                        showToast(djiError.toString());
                    }
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行控制器是否连接正常！");
        }
    }

    //设置返航高度
    private void setHomeHeight() {
        //设置返航高度文本里
        final EditText editText = new EditText(this);
        //限定只能输入数字
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("请输入返航高度（m）")
                .setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int height = Integer.parseInt(editText.getText().toString());
                        final FlightController flightController = getFlightController();
                        if (flightController != null) {
                            flightController.setGoHomeHeightInMeters(height, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.toString());
                                    } else {
                                        showToast("返航高度设置成功！");
                                    }
                                }
                            });
                        } else {
                            showToast("飞行控制器获取失败，请检查飞行控制器是否连接正常！");
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    //获取返航高度
    private void getHomeHeight() {
        final FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.getGoHomeHeightInMeters(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    showToast("返航高度为：" + integer + "米");
                }

                @Override
                public void onFailure(DJIError djiError) {
                    showToast("获取返航高度失败：" + djiError.toString());
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行控制器是否连接正常！");
        }
    }

}
