package com.izuiyou;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class NetWork extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass NetCrypto;
    private final VM vm;

    public NetWork() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("cn.xiaochuankeji.tieba")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/zuiyou/zuiyou.apk"));
        // 设置 JNI
        vm.setJni(this);
        vm.setVerbose(true);
        // 使用 libandroid.so 虚拟模块
        new AndroidModule(emulator, vm).register(memory);
        DalvikModule dm = vm.loadLibrary("net_crypto", true);
        NetCrypto = vm.resolveClass("com.izuiyou.network.NetCrypto");
        dm.callJNI_OnLoad(emulator);
    }

    public String callSign(){
        String arg1 = "hello word";
        byte[] arg2 = "V I 50".getBytes(StandardCharsets.UTF_8);
        String ret = NetCrypto.callStaticJniMethodObject(emulator, "sign(Ljava/lang/String;[B)Ljava/lang/String;", arg1, arg2).getValue().toString();
        return ret;
    }

    public static void main(String[] args) {
        NetWork nw = new NetWork();
        String result = nw.callSign();
        System.out.println("call s result: " + result);
    }
}
