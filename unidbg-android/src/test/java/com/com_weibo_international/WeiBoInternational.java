package com.com_weibo_international;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.Arm32RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import unicorn.ArmConst;

import java.io.File;
import java.util.Arrays;

public class WeiBoInternational extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass WeiboApplication;
    private final VM vm;
    private byte[] savedByteArray;

    public WeiBoInternational() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.weibo.international")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/weibo/com.weibo.international.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("utility", true);
        // path free
        emulator.attach().addBreakPoint(dm.getModule().findSymbolByName("free").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                Arm32RegisterContext registerContext = emulator.getContext();
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 0);
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, registerContext.getLR());
                return true;
            }
        });

        //WeiboApplication = vm.resolveClass("com.sina.weibo.security.WeiboSecurityUtils");
        WeiboApplication = vm.resolveClass("com.sina.weibo.WeiboApplication");
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "android/content/ContextWrapper->getPackageManager()Landroid/content/pm/PackageManager;":
            case "android/app/Application->getPackageManager()Landroid/content/pm/PackageManager;":
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            case "java/security/MessageDigest->digest()[B":
                if (savedByteArray != null) {
                    return new ByteArray(vm, savedByteArray);
                }
                //return new ByteArray(vm, new byte[0]);
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "java/security/MessageDigest->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;":
                return vm.resolveClass("java/security/MessageDigest").newObject(null);
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public void callVoidMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/security/MessageDigest->update([B)V":
                DvmObject<?> argObject = varArg.getObjectArg(0);
                savedByteArray = (byte[]) argObject.getValue();
                System.out.println(Arrays.toString(savedByteArray));
                return;
        }
        super.callVoidMethod(vm, dvmObject, signature, varArg);
    }


    public String callcalculateS() {
        DvmObject<?> context = vm.resolveClass("android/app/Application", vm.resolveClass("android/content/ContextWrapper", vm.resolveClass("android/content/Context"))).newObject(null);
        String agr2 = "hello word";
        String agr3 = "123456";
        return WeiboApplication.newObject(null).callJniMethodObject(emulator, "calculateS", context, agr2, agr3).getValue().toString();
    }


    public static void main(String[] args) {
        WeiBoInternational wb = new WeiBoInternational();
        String result = wb.callcalculateS();
        System.out.println("call s result:"+result);  // call s result:9dd7d4a8
    }
}
