package com.LingClub;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.ReadHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import unicorn.ArmConst;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

public class LingLingBang extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    public LingLingBang() {
        emulator = AndroidEmulatorBuilder.for32Bit().build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分

        // 模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建Android虚拟机
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/LingClub/lingclub.apk"));
        vm.setVerbose(true);
        // 加载基本库
        new AndroidModule(emulator, vm).register(memory);
        // 加载so到虚拟内存
        DalvikModule dm = vm.loadLibrary("encrypt", true);
        module = dm.getModule();
        emulator.attach().addBreakPoint(module.base + 0xe535);
        // 设置JNI
        vm.setJni(this);
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "android/app/ActivityThread->currentActivityThread()Landroid/app/ActivityThread;":
                return dvmClass.newObject(null);
            case "android/os/SystemProperties->get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;":
                Object key = varArg.getObjectArg(0).getValue();
                System.out.println("SystemProperties->get key=" + key);
                if (key.equals("ro.serialno")) {
                    return new StringObject(vm, "89PX0CQ22");
                }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "android/app/ActivityThread->getSystemContext()Landroid/app/ContextImpl;":
                return vm.resolveClass("android/app/ContextImpl").newObject(null);
            case "android/app/ContextImpl->getPackageManager()Landroid/content/pm/PackageManager;":
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            case "android/app/ContextImpl->getSystemService(Ljava/lang/String;)Ljava/lang/Object;":
                StringObject serviceName = varArg.getObjectArg(0);
                if (serviceName.getValue().equals("wifi")) {
                    return vm.resolveClass("android/net/wifi/WifiManager").newObject(null);
                }
            case "android/net/wifi/WifiManager->getConnectionInfo()Landroid/net/wifi/WifiInfo;":
                return vm.resolveClass("android/net/wifi/WifiInfo").newObject(signature);
            case "android/net/wifi/WifiInfo->getMacAddress()Ljava/lang/String;":
                return new StringObject(vm, "00:11:22:33:44:55");
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    public void call_CheckCodeUtil_decheckcode() {
        // args list
        List<Object> list = new ArrayList<>(10);
        // jnienv
        list.add(vm.getJNIEnv());
        // jclazz
        list.add(0);
        // str1
        String str = "MsYDpDaPPjwzNLwNdZhfSX+WqnxadmdldJg1hNzR2K/5mTVXSgZXPKfI1UAdpmOnCYBxKvMg4ERNxHTFB970l0UZeuuDlzksnUsJJIGPUNU/YWlmuaw99NDsvhGPljeoD/hJ+Tu18LsqLbang8LH+//Jrku4HZG0jb45IwnVfNGzA3ZYE3AGxFHZA5e1yeqA3H766ujE6InluI1cYvBEl+LP7jR+7zkAeQsYpES41aL0=";
        list.add(vm.addLocalObject(new StringObject(vm, str)));

        Number number = module.callFunction(emulator, 0x165e1, list.toArray());
        System.out.println(number);
    }

    public static void main(String[] args) {
        LingLingBang lingLingBang = new LingLingBang();
        lingLingBang.call_CheckCodeUtil_decheckcode();
    }
}
