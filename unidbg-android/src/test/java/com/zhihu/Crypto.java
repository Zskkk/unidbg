package com.zhihu;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;
import com.github.unidbg.virtualmodule.android.MediaNdkModule;
import com.github.unidbg.virtualmodule.android.SystemProperties;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import com.github.unidbg.arm.backend.ReadHook;
import unicorn.Arm64Const;
import unicorn.ArmConst;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zsk
 * on 2025/02/24 01:53
 */
public class Crypto extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    public Crypto() {
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("com.zhihu.android").build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        emulator.getSyscallHandler().addIOResolver(this);
        // 模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建Android虚拟机
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/zhihu/zhihu10.37.0.apk"));
        // 设置JNI
        vm.setJni(this);
        // 打印日志
        vm.setVerbose(true);
        // 加载基本库
        new AndroidModule(emulator, vm).register(memory);
        new MediaNdkModule(emulator, vm).register(memory);
        new JniGraphics(emulator, vm).register(memory);
        new SystemProperties(emulator, null).register(memory);
        // 加载so到虚拟内存
        DalvikModule dm = vm.loadLibrary("bangcle_crypto_tool", true);
        module = dm.getModule();

        dm.callJNI_OnLoad(emulator);
        //emulator.attach().addBreakPoint(module.base + 0x8b2c);
        //emulator.attach().addBreakPoint(module.base + 0x9070);
        //emulator.attach().addBreakPoint(module.basee + 0x2f8c); // Bangcle_LAES_cbc_encrypt
        //emulator.attach().addBreakPoint(module.base + 0x8a60);
        //emulator.attach().addBreakPoint(module.base + 0x49a8);  // Bangcle_internal_crypto
        //emulator.attach().addBreakPoint(module.base + 0x5d7c);  // Bangcle_WB_LAES_encrypt;
        //emulator.attach().addBreakPoint(module.base + 0x5ea4);  // Bangcle_WB_LAES_encrypt;
        //emulator.attach().addBreakPoint(module.base + 0x5a6c);  // Bangcle_CRYPTO_cbc128_encrypt;
        //emulator.attach().addBreakPoint(module.base + 0x45dc);  // Bangcle_CRYPTO_cbc128_encrypt;
        //emulator.attach().addBreakPoint(module.base + 0x3ba8);  // Bangcle_CRYPTO_cbc128_encrypt;
        // a3a2c9037f665ae3db78347098068bfe 6a3329dae2c7a91d07fe3180688bc3f5 b3edda051b8aa5dd5812afde99234d2e

    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("open file:" + pathname);
        return null;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int unsignedInt = b & 0xff;
            String hex = Integer.toHexString(unsignedInt);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public void callByAddress() {
        //emulator.traceCode(module.base, module.base + module.size);
        // args list
        List<Object> list = new ArrayList<>(10);
        // jnienv
        list.add(vm.getJNIEnv());
        // jclazz
        list.add(0);

        //byte[] byy1 = {-125, 115, 113, -128, -113, -119, 33, 40, -120, 33, 42, -118, 34, 40, -113, -113, -118, -117, 34, 33, 42, -115, -117, 35, 34, 34, -119, 33, -127, 33, -127, -117};
        //byte[] byy1 = {48, 57, 55, 49, 101, 99, 102, 53, 57, 100, 97, 57, 100, 97, 53, 55, 101, 54, 97, 51, 102, 55, 100, 51, 52, 55, 50, 98, 98, 100, 55, 54};
        byte[] byy1 = {-117, -115, -128, -119, 33, 40, 34, -127, -115, 35, 41, -115, 35, 41, -127, -128, 33, -126, 41, -120, 34, -128, 35, -120, -125, -128, -118, 42, 42, 35, -128, -126};

        byte[] byy2 = {-103, 48, 58, 58, 50, 52, 58, 57, -110, -110, 58, 59, 58, -103, -110, -110};
        System.out.println(bytesToHex(byy1));
        System.out.println(bytesToHex(byy2));

        ByteArray arr1 = new ByteArray(vm, byy1);
        ByteArray arr2 = new ByteArray(vm, byy2);

        list.add(vm.addLocalObject(arr1));
        list.add(vm.addLocalObject(new StringObject(vm, "541a3a5896fbefd351917c8251328a236a7efbf27d0fad8283ef59ef07aa386dbb2b1fcbba167135d575877ba0205a02f0aac2d31957bc7f028ed5888d4bbe69ed6768efc15ab703dc0f406b301845a0a64cf3c427c82870053bd7ba6721649c3a9aca8c3c31710a6be5ce71e4686842732d9314d6898cc3fdca075db46d1ccf3a7f9b20615f4a303c5235bd02c5cdc791eb123b9d9f7e72e954de3bcbf7d314064a1eced78d13679d040dd4080640d18c37bbde")));
        list.add(vm.addLocalObject(arr2));

        Number number = module.callFunction(emulator, 0x9e98, list.toArray());
        ByteArray resultArr = vm.getObject(number.intValue());
        System.out.println("result: " + bytesToHex(resultArr.getValue()));
    }


    public void traceAESRead() {
        ReadHook readHook = new ReadHook() {
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                int now = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_PC).intValue();
                if((now > module.base) & (now < (module.base + module.size))){
                    System.out.println(now - module.base);
                }
            }

            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {

            }
        };
        emulator.getBackend().hook_add_new(readHook, module.base, module.base+module.size, null);
    }


    // fee函数会报错，直接patch掉
    public void patch() {
        try (Keystone keystone = new Keystone(KeystoneArchitecture.Arm64, KeystoneMode.LittleEndian);) {
            KeystoneEncoded encoded = keystone.assemble("b #0x28");
            byte[] patchCode = encoded.getMachineCode();
            emulator.getMemory().pointer(module.base + 0x48cc).write(patchCode);
        }
        //UnidbgPointer pointer = UnidbgPointer.pointer(emulator, module.base + 0x104ca);
        //byte[] code = {(byte) 0xa0, (byte) 0x0e, (byte) 0x00, (byte) 0x54};
        //pointer.write(code);
    }

    public void trade(){
        String tradeFile = "unidbg-android/src/test/java/com/zhihu/trade.txt";
        PrintStream traceStream = null;
        try {
            traceStream = new PrintStream(new FileOutputStream(tradeFile), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 核心 trade开启代码，也可以自己指定函数地址和偏移量
        emulator.traceCode(module.base, module.base+ module.size).setRedirect(traceStream);
    }

    public static void main(String[] args) {
        Crypto crypto = new Crypto();
        crypto.patch();
        //crypto.trade();
        //crypto.traceAESRead();
        crypto.callByAddress();
    }
}
