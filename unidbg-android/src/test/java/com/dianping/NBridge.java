package com.dianping;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.SystemService;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NBridge extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final DvmObject<?> SIUACollector;
    private final VM vm;
    public FileInputStream fileInputStream;
    public InputStreamReader inputStreamReader;
    public BufferedReader bufferedReader;
    public File file1;
    public File file2;
    public SimpleDateFormat simpleDateFormat;

    public NBridge() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/dianping/dazhongdianping.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        emulator.getSyscallHandler().addIOResolver(this);
        // 使用 libandroid.so 的虚拟模块
        new AndroidModule(emulator, vm).register(memory);
        ;
        // 使用 libjnigraphics.so 的虚拟模块
        new JniGraphics(emulator, vm).register(memory);
        DalvikModule dm = vm.loadLibrary("mtguard", true);
        SIUACollector = vm.resolveClass("com/meituan/android/common/mtguard/NBridge$SIUACollector").newObject(null);
        //SIUACollector = vm.resolveClass("com/meituan/android/common/mtguard/NBridge$SIUACollector").newObject(null);
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public DvmObject<?> allocObject(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "java/lang/StringBuilder->allocObject":
                return dvmClass.newObject(new StringBuilder());
            case "java/io/BufferedReader->allocObject":
                return dvmClass.newObject(null);
            case "java/io/InputStreamReader->allocObject":
                return dvmClass.newObject(signature);
            case "java/io/FileInputStream->allocObject":
                return dvmClass.newObject(signature);
            case "java/io/File->allocObject":
                return dvmClass.newObject(signature);
            case "java/text/SimpleDateFormat->allocObject":
                return dvmClass.newObject(signature);
            case "java/util/Date->allocObject":
                return ProxyDvmObject.createObject(vm, new Date());
        }
        return super.allocObject(vm, dvmClass, signature);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/StringBuilder-><init>()V":
                return;
            case "java/io/FileInputStream-><init>(Ljava/lang/String;)V":
                String path = vaList.getObjectArg(0).getValue().toString();
                if (path.equals("/proc/cpuinfo")) {
                    path = "unidbg-android/src/test/resources/dianping/cpuinfo";
                }
                System.out.println("FileInputStream:" + path);
                try {
                    fileInputStream = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return;
            case "java/io/InputStreamReader-><init>(Ljava/io/InputStream;)V":
                inputStreamReader = new InputStreamReader(fileInputStream);
                return;
            case "java/io/BufferedReader-><init>(Ljava/io/Reader;)V":
                bufferedReader = new BufferedReader(inputStreamReader);
                return;
            case "java/io/BufferedReader->close()V":
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->safeClose(Ljava/io/Closeable;)V":
                return;
            case "java/io/File-><init>(Ljava/lang/String;)V":
                path = vaList.getObjectArg(0).getValue().toString();
                System.out.println("filePath:" + path);
                if (path.equals("/sys/class/power_supply/battery/voltage_now")) {
                    file1 = new File("unidbg-android/src/test/resources/dianping/voltage_now");
                    return;
                }
                if (path.equals("/sys/class/power_supply/battery/temp")) {
                    file2 = new File("unidbg-android/src/test/resources/dianping/voltage_now");
                    return;
                }
            case "java/text/SimpleDateFormat-><init>(Ljava/lang/String;Ljava/util/Locale;)V":
                String pattern = vaList.getObjectArg(0).getValue().toString();
                Locale locale = (Locale) vaList.getObjectArg(1).getValue();
                simpleDateFormat = new SimpleDateFormat(pattern, locale);
                return;
            case "java/util/Date-><init>()V":
                return;
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getEnvironmentInfo()Ljava/lang/String;":
                return new StringObject(vm, "0|0|0|-|0|");
            case "java/lang/StringBuilder->append(Ljava/lang/String;)Ljava/lang/StringBuilder;":
                String value = vaList.getObjectArg(0).getValue().toString();
                System.out.println(vaList.getObjectArg(0).getValue());
                System.out.println(dvmObject.getValue());
                return ProxyDvmObject.createObject(vm, ((StringBuilder) dvmObject.getValue()).append(value));
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->isVPN()Ljava/lang/String;":
                return new StringObject(vm, "0");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->brightness(Landroid/content/Context;)Ljava/lang/String;":
                return new StringObject(vm, "0.8");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->systemVolume(Landroid/content/Context;)Ljava/lang/String;":
                return new StringObject(vm, "0");
            case "java/lang/StringBuilder->toString()Ljava/lang/String;":
                return new StringObject(vm, ((StringBuilder) dvmObject.getValue()).toString());
            case "android/content/Context->getApplicationContext()Landroid/content/Context;":
                return vm.resolveClass("android/content/Context").newObject(null);
            case "android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;":
                StringObject serviceName = vaList.getObjectArg(0);
                assert serviceName != null;
                return new SystemService(vm, serviceName.getValue());
            case "android/telephony/TelephonyManager->getDeviceId()Ljava/lang/String;":
                return new StringObject(vm, "000000000000000");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->checkBuildAttribute(Ljava/lang/String;)Ljava/lang/String;":
                String arg = vaList.getObjectArg(0).getValue().toString();
                System.out.println("checkBuildAttribute:" + arg);
                return new StringObject(vm, arg.isEmpty() || arg.equalsIgnoreCase("unknown") ? "-" : arg);
            case "android/view/WindowManager->getDefaultDisplay()Landroid/view/Display;":
                return vm.resolveClass("android/view/Display").newObject(null);
            case "java/lang/StringBuilder->append(I)Ljava/lang/StringBuilder;":
                return ProxyDvmObject.createObject(vm, ((StringBuilder) dvmObject.getValue()).append(vaList.getIntArg(0)));
            case "java/lang/StringBuilder->append(C)Ljava/lang/StringBuilder;":
                return ProxyDvmObject.createObject(vm, ((StringBuilder) dvmObject.getValue()).append(vaList.getIntArg(0)));
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getTotalInternalMemorySize()Ljava/lang/String;":
                return new StringObject(vm, "110GB");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getTotalExternalMemorySize()Ljava/lang/String;":
                return new StringObject(vm, "110GB");
            // SimOperator 由 MCC + MNC 组成。
            //MCC由国际电信联盟 ITU 在全世界范围内统一分配和管理，唯一识别移动用户所属的国家，共3位，中国为460。
            //MNC用于识别移动用户所归属的移动通信网，2~3位。中国移动系统使用 00、02、04、07，中国联通系统使用 01、06、09，中国电信使用 03、05、11，中国铁通系统使用 20 。我的测试机手机卡是中国联通，具体值是46001
            case "android/telephony/TelephonyManager->getSimOperator()Ljava/lang/String;":
                return new StringObject(vm, "46001");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getAccessSubType()Ljava/lang/String;":
                return new StringObject(vm, "4G");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getCpuInfoType()Ljava/lang/String;":
                return new StringObject(vm, "arm");
            case "java/io/BufferedReader->readLine()Ljava/lang/String;":
                String oneline;
                try {
                    oneline = bufferedReader.readLine();
                    if (oneline == null) {
                        return null;
                    }
                    return new StringObject(vm, oneline);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            case "android/hardware/SensorManager->getDefaultSensor(I)Landroid/hardware/Sensor;":
                int type = vaList.getIntArg(0);
                System.out.println("Sensor type:" + type);
                return vm.resolveClass("android/hardware/Sensor").newObject(type);
            // getMaximumRange() 最大取值范围
            // getName() 设备名称
            // getPower() 功率
            // getResolution() 精度
            // getType() 传感器类型
            // getVentor() 设备供应商
            // getVersion() 设备版本号
            case "android/hardware/Sensor->getName()Ljava/lang/String;":
                int typ = ((Integer) dvmObject.getValue()).intValue();
                System.out.println("Sensor getName:" + typ);
                if (typ == 1) {
                    return new StringObject(vm, "ICM20690");
                } else if (typ == 9) {
                    return new StringObject(vm, "gravity  Non-wakeup");
                } else {
                    throw new UnsupportedOperationException(signature);
                }
            case "android/hardware/Sensor->getVendor()Ljava/lang/String;":
                typ = ((Integer) dvmObject.getValue()).intValue();
                System.out.println("Sensor getVendor:" + typ);
                if (typ == 1) {
                    return new StringObject(vm, "qualcomm");
                } else if (typ == 9) {
                    return new StringObject(vm, "qualcomm");
                } else {
                    throw new UnsupportedOperationException(signature);
                }
                // getprop | grep ro.product.cpu.abi
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getSysProp(Ljava/lang/String;)Ljava/lang/String;":
                String name = vaList.getObjectArg(0).getValue().toString();
                System.out.println("getSysProp:" + name);
                if (name.equals("ro.product.cpu.abi")) {
                    return new StringObject(vm, "arm64-v8a");
                } else if (name.equals("ro.product.cpu.abi2")) {
                    return new StringObject(vm, "");
                } else if (name.equals("ro.build.product")) {
                    return new StringObject(vm, "polaris");
                } else if (name.equals("ro.build.description")) {
                    return new StringObject(vm, "polaris-user 10 QKQ1.190828.002 V12.0.2.0.QDGCNXM release-keys");
                } else if (name.equals("ro.secure")) {
                    return new StringObject(vm, "1");
                } else if (name.equals("ro.debuggable")) {
                    return new StringObject(vm, "0");
                } else if (name.equals("persist.sys.usb.config")) {
                    return new StringObject(vm, "");
                } else if (name.equals("sys.usb.config")) {
                    return new StringObject(vm, "");
                } else if (name.equals("sys.usb.state")) {
                    return new StringObject(vm, "");
                } else if (name.equals("gsm.version.baseband")) {
                    return new StringObject(vm, "4.0.c2.6-00335-0914_2350_3c8fca6,4.0.c2.6-00335-0914_2350_3c8fca6");
                } else if (name.equals("gsm.version.ril-impl")) {
                    return new StringObject(vm, "Qualcomm RIL 1.0");
                } else if (name.equals("gsm.sim.state")) {
                    return new StringObject(vm, "ABSENT,ABSENT");
                } else if (name.equals("gsm.sim.state.2")) {
                    return new StringObject(vm, "");
                } else if (name.equals("wifi.interface")) {
                    return new StringObject(vm, "wlan0");
                }
                throw new UnsupportedOperationException(signature);
            case "android/content/Context->getResources()Landroid/content/res/Resources;":
                return vm.resolveClass("android/content/res/Resources").newObject(null);
            case "android/content/res/Resources->getConfiguration()Landroid/content/res/Configuration;":
                return vm.resolveClass("android/content/res/Configuration").newObject(null);
            case "android/net/wifi/WifiManager->getConnectionInfo()Landroid/net/wifi/WifiInfo;":
                return vm.resolveClass("android/net/wifi/WifiInfo").newObject(signature);
            case "android/net/wifi/WifiInfo->getSSID()Ljava/lang/String;":
                return new StringObject(vm, "Redmi_7B25");
            case "android/telephony/TelephonyManager->getNetworkOperator()Ljava/lang/String;":
                return new StringObject(vm, "");
            case "java/text/SimpleDateFormat->format(Ljava/util/Date;)Ljava/lang/String;":
                Date date = (Date) vaList.getObjectArg(0).getValue();
                return new StringObject(vm, simpleDateFormat.format(date));
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->appCache(Landroid/content/Context;)Ljava/lang/String;":
                return new StringObject(vm, "149045277");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->availableSystem()Ljava/lang/String;":
                return new StringObject(vm, "unknown");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->totalMemory()Ljava/lang/String;":
                return new StringObject(vm, "5905514496");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getFirstLaunchTime(Landroid/content/Context;)Ljava/lang/String;":
                return new StringObject(vm, "1723368286983");
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getDataActivity(Landroid/content/Context;)Ljava/lang/String;":
                return new StringObject(vm, "0");
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->isAccessibilityEnable()Z":
                return false;
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->isPermissionGranted(Ljava/lang/String;Landroid/content/Context;)Z":
                String permissionName = vaList.getObjectArg(0).getValue().toString();
                System.out.println("check permission:" + permissionName);
                if (permissionName.equals("android.permission.READ_PHONE_STATE")) {
                    return true;
                } else if (permissionName.equals("android.permission.ACCESS_WIFI_STATE")) {
                    return true;
                } else {
                    throw new UnsupportedOperationException(signature);
                }
            case "android/content/pm/PackageManager->hasSystemFeature(Ljava/lang/String;)Z":
                String name = vaList.getObjectArg(0).getValue().toString();
                System.out.println("check hasSystemFeature:" + name);
                switch (name) {
                    // 检测加速传感器
                    case "android.hardware.sensor.accelerometer":
                        return true;
                    // 检测陀螺仪传感器
                    case "android.hardware.sensor.gyroscope":
                        return true;
                }
            case "java/io/File->exists()Z":
                return file1.exists();
            case "java/lang/String->equalsIgnoreCase(Ljava/lang/String;)Z":
                String value = vaList.getObjectArg(0).getValue().toString();
                return dvmObject.getValue().toString().equalsIgnoreCase(value);
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getBatteryInfo()Z":
                return true;
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->uiAutomatorClickCount()I":
                return 0;
            case "android/view/Display->getHeight()I":
                return 2160;
            case "android/view/Display->getWidth()I":
                return 1080;
            case "java/lang/String->compareToIgnoreCase(Ljava/lang/String;)I":
                String str = vaList.getObjectArg(0).getValue().toString();
                return dvmObject.getValue().toString().compareToIgnoreCase(str);
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->boolean2Integer(Z)I":
                return vaList.getIntArg(0);
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->androidAppCnt(Landroid/content/Context;)I":
                return 519;
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/String->valueOf(I)Ljava/lang/String;":
                return new StringObject(vm, String.valueOf(vaList.getIntArg(0)));
            case "java/lang/Integer->toString(I)Ljava/lang/String;":
                return new StringObject(vm, Integer.toString(vaList.getIntArg(0)));
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "android/text/TextUtils->isEmpty(Ljava/lang/CharSequence;)Z":
                String str = vaList.getObjectArg(0).getValue().toString();
                return str == null || str.length() == 0;
        }
        return super.callStaticBooleanMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->mContext:Landroid/content/Context;":
                return vm.resolveClass("android/content/Context").newObject(null);
            case "android/content/res/Configuration->locale:Ljava/util/Locale;":
                return ProxyDvmObject.createObject(vm, Locale.getDefault());
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->batteryHelper:Lcom/meituan/android/common/dfingerprint/collection/utils/BatteryHelper;":
                return vm.resolveClass("com/meituan/android/common/dfingerprint/collection/utils/BatteryHelper").newObject(null);
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "android/os/Build->BOARD:Ljava/lang/String;":
                return new StringObject(vm, "sdm845");
            case "android/os/Build->MANUFACTURER:Ljava/lang/String;":
                return new StringObject(vm, "xiaomi");
            case "android/os/Build->BRAND:Ljava/lang/String;":
                return new StringObject(vm, "Xiaomi");
            case "android/os/Build->MODEL:Ljava/lang/String;":
                return new StringObject(vm, "MIX 2S");
            case "android/os/Build->PRODUCT:Ljava/lang/String;":
                return new StringObject(vm, "polaris");
            case "android/os/Build->HARDWARE:Ljava/lang/String;":
                return new StringObject(vm, "qcom");
            case "android/os/Build->DEVICE:Ljava/lang/String;":
                return new StringObject(vm, "polaris");
            case "android/os/Build->HOST:Ljava/lang/String;":
                return new StringObject(vm, "c3-miui-ota-bd134.bj");
            case "android/os/Build->ID:Ljava/lang/String;":
                return new StringObject(vm, "QKQ1.190828.002");
            case "android/os/Build$VERSION->RELEASE:Ljava/lang/String;":
                return new StringObject(vm, "10");
            case "android/os/Build->TAGS:Ljava/lang/String;":
                return new StringObject(vm, "release-keys");
            case "android/os/Build->FINGERPRINT:Ljava/lang/String;":
                return new StringObject(vm, "Xiaomi/polaris/polaris:10/QKQ1.190828.002/V12.0.2.0.QDGCNXM:user/release-keys");
            case "android/os/Build->TYPE:Ljava/lang/String;":
                return new StringObject(vm, "user");
            case "android/os/Build$VERSION->SDK:Ljava/lang/String;":
                return new StringObject(vm, "29");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "android/os/Build$VERSION->SDK_INT:I":
                return 29;
        }
        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public int getIntField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            // level 电量剩余多少
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->level:I":
                return 88;
            // scale 电量精度
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->scale:I":
                return 100;
            // status 电池状态，未知、充电、放电、未充电、满电;
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->status:I":
                return 0;
        }
        return super.getIntField(vm, dvmObject, signature);
    }

    @Override
    public boolean getBooleanField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->plugged:Z":
                return false;
        }
        return super.getBooleanField(vm, dvmObject, signature);
    }

    public String getEnvironmentInfo() {
        String result = SIUACollector.callJniMethodObject(emulator, "getEnvironmentInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getEnvironmentInfoExtra() {
        String result = SIUACollector.callJniMethodObject(emulator, "getEnvironmentInfoExtra()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getExternalEquipmentInfo() {
        String result = SIUACollector.callJniMethodObject(emulator, "getExternalEquipmentInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getHWEquipmentInfo() {
        String result = SIUACollector.callJniMethodObject(emulator, "getHWEquipmentInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getHWProperty() {
        String result = SIUACollector.callJniMethodObject(emulator, "getHWProperty()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getHWStatus() {
        String result = SIUACollector.callJniMethodObject(emulator, "getHWStatus()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getLocationInfo() {
        String result = SIUACollector.callJniMethodObject(emulator, "getLocationInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getPlatformInfo() {
        String result = SIUACollector.callJniMethodObject(emulator, "getPlatformInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getUserAction() {
        String result = SIUACollector.callJniMethodObject(emulator, "getUserAction()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public static void main(String[] args) {
        NBridge nBridge = new NBridge();
        System.out.println("getEnvironmentInfo:" + nBridge.getEnvironmentInfo());
        System.out.println("getEnvironmentInfoExtra:" + nBridge.getEnvironmentInfoExtra());
        System.out.println("getExternalEquipmentInfo:" + nBridge.getExternalEquipmentInfo());
        System.out.println("getHWEquipmentInfo:" + nBridge.getHWEquipmentInfo());
        System.out.println("getHWProperty:" + nBridge.getHWProperty());
        System.out.println("getHWStatus:" + nBridge.getHWStatus());
        System.out.println("getLocationInfo:" + nBridge.getLocationInfo());
        System.out.println("getPlatformInfo:" + nBridge.getPlatformInfo());
        System.out.println("getUserAction:" + nBridge.getUserAction());
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("lilac open:" + pathname);
        return null;
    }
}
