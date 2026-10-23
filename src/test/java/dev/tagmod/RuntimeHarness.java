package dev.tagmod;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
public final class RuntimeHarness {
    public static void main(String[] args) throws Exception {
        Knot knot=new Knot(EnvType.valueOf(System.getProperty("tagmod.testEnvironment","SERVER")));
        ClassLoader target=knot.init(new String[0]);
        target.loadClass("dev.tagmod.CoreTests").getMethod("main",String[].class).invoke(null,(Object)new String[0]);
    }
}
