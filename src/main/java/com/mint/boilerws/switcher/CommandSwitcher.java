package com.mint.boilerws.switcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.mint.boilerws.config.Config;

/**
 * Use 433Mhz transmitter in Raspberry Pi to send out on/off signal to remote
 * device (eg Arduino) to switch on/off. 433Mhz transmitter used is from
 * https://github.com/rotv/433Utils, using command such as "sudo ./codesend
 * 500002" under the "433Utils/RPi_utils" folder of the downloaded package.
 * 
 * 
 * @author mint
 *
 */
public class CommandSwitcher extends Switcher {
    
    private static final boolean DEFAULT_STATE_IS_ON = false;
    
    private static final Logger LOG = Logger.getLogger(CommandSwitcher.class);

    private final String onCommand;
    private final String offCommand;
    
    public CommandSwitcher(final Config config) {
        super(config);
        this.onCommand = config.get("command.switcher.on", null);
        this.offCommand = config.get("command.switcher.off", null);
        if (this.onCommand == null || offCommand == null) {
            throw new RuntimeException("Config command not found: " + onCommand + ", " + offCommand);
        }
        // switch on/off to have a known default state
        final boolean defaultState = (config.get("command.switcher.default.is.on", DEFAULT_STATE_IS_ON));
        switchOn(defaultState);
    }
    
    public static boolean isConfigured(Config config) {
        return (config.get("command.switcher.on", null) != null
               && config.get("command.switcher.off", null) != null);
    }

    @Override
    protected boolean doSwitch(boolean switchOn) {
        final String command = (switchOn) ? this.onCommand : this.offCommand;
        final boolean ok = runCommand(command);
        return ok;
    }
    
    private boolean runCommand(final String command) {
        try {
            LOG.info("Running: " + command);
            String s;
            Process p = Runtime.getRuntime().exec(command);
            final BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            final StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null) {
                sb.append(s).append("\n");
            }
            LOG.info("Got: " + sb.toString());
            return true;
        } catch (Exception e) {
            LOG.error("Error running command: " + command, e);
        }
        return false;
    }

}
