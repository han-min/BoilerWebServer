package com.mint.boilerws.switcher;

import org.apache.log4j.Logger;

import com.mint.boilerws.config.Config;
import com.mint.boilerws.handler.ScheduleHandler;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

/**
 * Switch the pin on the GPIO directly
 * 
 * This uses Pi4J and requires 'wiringpi' installed on the Pi itself.
 * 
 */
public class GpioSwitcher extends Switcher {

    /**
     * https://pi4j.com/1.2/pins/model-b-rev2.html
     * 
     * 3.3v     5.0v
     *  8        x
     *  9       GND
     *  7       15
     *  x       16
     *  0        1
     *  2        x
     *  3        4
     *  x        5
     * ..
     */
    private static final int DEFAULT_PIN = 7;
    private static final long DELAY_BEFORE_CHECKING_PIN_STATE = 500;
    private static final boolean PIN_HIGH_IS_ON = true;
    private static final PinState DEFAULT_PIN_STATE = PinState.HIGH;
    
    private static final Logger LOG = Logger.getLogger(ScheduleHandler.class);
    private static final Pin[] GPIO_MAP = {
            RaspiPin.GPIO_00, 
            RaspiPin.GPIO_01,
            RaspiPin.GPIO_02,
            RaspiPin.GPIO_03,
            RaspiPin.GPIO_04,
            RaspiPin.GPIO_05,
            RaspiPin.GPIO_06,
            RaspiPin.GPIO_07,
            RaspiPin.GPIO_08,
            RaspiPin.GPIO_09,
            RaspiPin.GPIO_10,
            RaspiPin.GPIO_11,
            RaspiPin.GPIO_12,
            RaspiPin.GPIO_13,
            RaspiPin.GPIO_14,
            RaspiPin.GPIO_15,
            RaspiPin.GPIO_16
    };
    
    private final PinWrapper centralHeatingPinOut;
        
    public GpioSwitcher(final Config config) {
        super(config);
        //
        int pinToUse = config.get("gpio.boiler.ch.pin.out", DEFAULT_PIN);
        if (pinToUse > GPIO_MAP.length-1) {
            LOG.error("Invalid config for GPIO pin:" + pinToUse);
            pinToUse = DEFAULT_PIN;
        }
        final Pin pinOut = GPIO_MAP[pinToUse];
        LOG.info("Boiler Central Heating using pin: " + pinToUse);
        //
        PinWrapper pinDigitalOut = new PinWrapper(DEFAULT_PIN_STATE);
        try {
            final GpioController control = GpioFactory.getInstance();
            final GpioPinDigitalOutput out = control.provisionDigitalOutputPin(pinOut, PinState.HIGH);
            out.setShutdownOptions(true, DEFAULT_PIN_STATE); // state when program is shutdown    
            pinDigitalOut = new PinWrapper(out);
        } catch (UnsatisfiedLinkError e) {
            LOG.error("Unable to initialize GPIO", e);
        }
        this.centralHeatingPinOut = pinDigitalOut;
    }

    @Override
    protected boolean isCurrentlyOn() {
        final boolean pinInHigh = this.centralHeatingPinOut.isHigh();
        return (pinInHigh == PIN_HIGH_IS_ON);
    }

    @Override
    protected boolean doSwitch(final boolean switchOn) {
        final boolean pinOnStateIsHigh = (PIN_HIGH_IS_ON) ? true : false;
        final boolean ok;
        if (switchOn) {
            ok = setPinState(pinOnStateIsHigh);
        } else {
            ok = setPinState(!pinOnStateIsHigh);
        }
        return ok;
    }
    
    private boolean setPinState(final boolean toPinStateHigh) {
        if (toPinStateHigh) {
            this.centralHeatingPinOut.high();
        } else {
            this.centralHeatingPinOut.low();
        }
        sleep();
        if (this.centralHeatingPinOut.isHigh() != toPinStateHigh) {
            LOG.error("Pin not switched correctly: " + centralHeatingPinOut.isHigh() + ", " + toPinStateHigh);
            return false;
        } else {
            return true;
        }
    }
    
    private void sleep() {
        try {
            Thread.sleep(DELAY_BEFORE_CHECKING_PIN_STATE);
        } catch (InterruptedException ignored) {
        }
    }

    private static class PinWrapper {
        private final GpioPinDigitalOutput pin;
        private boolean isHigh = false;

        public PinWrapper(PinState pinState) {
            this.pin = null;
            isHigh = (pinState == PinState.HIGH);
        }
        
        public PinWrapper(GpioPinDigitalOutput pin) {
            super();
            this.pin = pin;
        }

        public void high() {
            if (pin != null) {
                pin.high();
            }
            isHigh = true;
        }

        public void low() {
            if (pin != null) {
                pin.low();
            }
            isHigh = false;
        }

        public boolean isHigh() {
            if (pin != null) {
                return pin.isHigh();
            }
            return isHigh;
        }
    }
}
