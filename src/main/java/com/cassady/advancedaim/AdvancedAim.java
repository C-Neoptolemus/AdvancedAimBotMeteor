package com.cassady.advancedaim;

import com.cassady.advancedaim.modules.AdvancedAimAssist;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class AdvancedAim extends MeteorAddon {
    @Override
    public void onInitialize() {
        Modules.get().add(new AdvancedAimAssist());
    }

    @Override
    public String getPackage() {
        return "com.cassady.advancedaim";
    }
}
