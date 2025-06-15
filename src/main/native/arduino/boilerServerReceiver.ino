/*
  This is Arduino code to allow it to detect and response to 
  ON/OFF message sent by the Raspberry Pi with a 433MHz 
  transmitter.
 
  Arduino
  https://github.com/sui77/rc-switch/

  RaspberryPi
  https://github.com/rotv/433Utils
  
*/

#include <RCSwitch.h>

const int SERIAL_BAUD = 9600; //set to zero to disable
const int RECEIVER_INTERRUPT = 0; // interrupt 0 is pin #2

const long SWITCH_ON_CODE = 384477;
const long SWITCH_OFF_CODE = 416957;
const int PROTOCOL = 1;

const int ON_OFF_PIN = 13;
const boolean ON_IS_HIGH = true;

RCSwitch rc = RCSwitch();
int onHighLow;
int offHighLow;
boolean currentStateIsOn = true;

void setup() {
  onHighLow = ON_IS_HIGH ? HIGH : LOW;
  offHighLow = ON_IS_HIGH ? LOW : HIGH;
  if (SERIAL_BAUD > 0){
    Serial.begin(SERIAL_BAUD);
    Serial.println("Started..");
  }
  // config pin
  pinMode(ON_OFF_PIN, OUTPUT);
  digitalWrite(ON_OFF_PIN, onHighLow);
  // enable receiver
  rc.enableReceive(RECEIVER_INTERRUPT);  // Receiver on interrupt 0 => that is pin #2
}

void loop() {
  if (rc.available()) {
    long receivedValue = rc.getReceivedValue();
    int receivedProtocol = rc.getReceivedProtocol();
    rc.resetAvailable();
    //
    if (receivedProtocol == PROTOCOL){
      if (!currentStateIsOn && receivedValue == SWITCH_ON_CODE){
        digitalWrite(ON_OFF_PIN, onHighLow);
        currentStateIsOn = true;
      } else if (currentStateIsOn && receivedValue == SWITCH_OFF_CODE){
        digitalWrite(ON_OFF_PIN, offHighLow);
        currentStateIsOn = false;
      }
    }
    //
    if (SERIAL_BAUD > 0){ //Serial.available() doesn't work
      Serial.print("Received ");
      Serial.print(receivedValue);
      Serial.print(", protocol: ");
      Serial.println(receivedProtocol);
    }
  }
}
