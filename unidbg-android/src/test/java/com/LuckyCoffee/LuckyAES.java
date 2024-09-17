package com.LuckyCoffee;

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
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import unicorn.ArmConst;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.util.Random;

public class LuckyAES extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    public LuckyAES() {
        emulator = AndroidEmulatorBuilder.for32Bit().build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分

        // 模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建Android虚拟机
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/LuckyCoffee/ruixing5.0.01.apk"));
        vm.setVerbose(true);
        // 加载基本库
        new AndroidModule(emulator, vm).register(memory);
        // 加载so到虚拟内存
        DalvikModule dm = vm.loadLibrary("cryptoDD", true);
        module = dm.getModule();
        // 设置JNI
        vm.setJni(this);
        dm.callJNI_OnLoad(emulator);
    }

    public static byte[] hexStringToBytes(String hexString) {
        return DatatypeConverter.parseHexBinary(hexString);
        //if(hexString.isEmpty()){
        //    return null;
        //}
        //hexString = hexString.toLowerCase();
        //byte[] byteArray = new byte[hexString.length() >> 1];
        //int index = 0;
        //for(int i = 0; i < hexString.length(); i++) {
        //    if(index > hexString.length() - 1){
        //        return byteArray;
        //    }
        //    byte highDit = (byte) (Character.digit(hexString.charAt(index), 16) & 0xFF);
        //    byte lowDit = (byte) (Character.digit(hexString.charAt(index + 1), 16) & 0xFF);
        //    byteArray[i] = (byte) (highDit << 4 | lowDit);
        //    index += 2;
        //}
        //return byteArray;
    }

    public static String bytesTohexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public void traceAESRead() {
        ReadHook readHook = new ReadHook() {
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                int now = emulator.getBackend().reg_read(ArmConst.UC_ARM_REG_PC).intValue();
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

    public static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

    public void dfaAttack() {
        emulator.attach().addBreakPoint(module.base + 0x14f98 + 1, new BreakPointCallback() {
            int count = 0;
            UnidbgPointer pointer;

            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                count += 1;
                RegisterContext registerContext = emulator.getContext();
                pointer = registerContext.getPointerArg(0);
                emulator.attach().addBreakPoint(registerContext.getLRPointer().peer, new BreakPointCallback() {
                    @Override
                    public boolean onHit(Emulator<?> emulator, long address) {
                        if (count % 9 == 0) {
                            pointer.setByte(randInt(0, 15), (byte) randInt(0, 0xff));
                        }
                        return true;
                    }
                });
                return true;
            }
        });
    }

    public void call_wbaes_encrypt_ecb() {
        MemoryBlock inBlock = emulator.getMemory().malloc(16, true);
        UnidbgPointer inPtr = inBlock.getPointer();
        MemoryBlock outBlock = emulator.getMemory().malloc(16, true);
        UnidbgPointer outPtr = outBlock.getPointer();

        byte[] stub = hexStringToBytes("7a736b6b6b2e636e7a736b6b6b2e636e");
        assert stub != null;
        inPtr.write(0, stub, 0, stub.length);
        module.callFunction(emulator, 0x17bd5, inPtr, 16, outPtr, 0);
        String ret = bytesTohexString(outPtr.getByteArray(0, 0x10));
        System.out.println("white box result:" + ret);
        inBlock.free();
        outBlock.free();
    }

    public static void main(String[] args) {
        LuckyAES luckyAES = new LuckyAES();
        luckyAES.dfaAttack();
        for (int i = 0; i < 200; i++) {
            luckyAES.call_wbaes_encrypt_ecb();
        }
    }
}
