package dev.beecube31.updchecker;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SimpleUpdateChecker.MODID)
public class SimpleUpdateChecker {
    public static final String MODID = "simple_updates_checker";
    public static final Logger LOGGER = LogManager.getLogger("SimpleUpdateChecker");
    public static SimpleUpdateChecker INSTANCE;

    public SimpleUpdateChecker() {}
}
