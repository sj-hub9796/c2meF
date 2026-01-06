package com.ishland.c2me.fixes.worldgen.threading_issues.common;

import com.ishland.c2me.base.common.config.ConfigSystem;

public class Config {

    public static final boolean enforceSafeWorldRandomAccess = new ConfigSystem.ConfigAccessor()
            .key("fixes.enforceSafeWorldRandomAccess")
            .comment("""
                        Enforces safe world random access. \s
                        This feature detects unsafe off-thread world random access, helping to find the causes \s
                        of mysterious "Accessing LegacyRandomSource from multiple threads" crash. \s
                        When enabled, the mod will throw an exception when unsafe access is detected. \s
                        When disabled, the mod will log a warning and use a fallback random source. \s
                        
                        Default is disabled to prevent crashes with mods that access random from different threads. \s
                        Enable this only for debugging purposes or if you want to catch threading issues. \s
                        
                        """)
            .getBoolean(false,true);

    public static void init() {
    }

}
