/*
To build, run
  gcc RCSwitch.cpp codesend.cpp -o codesend -DRPI -lwiringPi -lwiringPiDev

with the unmodified RCSwitch.cpp at the same folder

//-- Original documention below, may not be applicabled --//

Usage: ./codesend decimalcode [protocol] [pulselength]
Sends the given decimalcode as an RF command.

decimalcode - As decoded by RFSniffer
protocol    - optional. According to rc-switch definitions
pulselength - optional. pulselength in microseconds

'codesend' hacked from 'send' by @justy

- The provided rc_switch 'send' command uses the form systemCode, unitCode, command
which is not suitable for our purposes.  Instead, we call 
send(code, length); // where length is always 24 and code is simply the code
we find using the RF_sniffer.ino Arduino sketch.

(Use RF_Sniffer.ino to check that RF signals are being produced by the RPi's transmitter 
or your remote control)
*/
#include "RCSwitch.h"
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <ctype.h>
#include <stdbool.h>

int main(int argc, char *argv[]) {

	int opt;
	int pin = -1;	//wiring pi pin numbering
	int protocol = 0;
	int code = -9999;
	while ((opt = getopt (argc, argv, "hc:p:m:")) != -1){
		switch(opt){
		case 'p':	//pin
			pin = atoi(optarg);
			break;
		case 'c':	//protocol
			protocol = atoi(optarg);
			break;
		case 'm':	//message
			code = atoi(optarg);
			break;
		case 'h':
		default:
			printf("\nUsage: %s -p <pin> -m <message> -c <protocol>\n", argv[0]);
			printf("   -p    output pin numbered as of wiring pi\n");
			printf("   -m    message to be sent\n");
			printf("   -c    (optional) protocol to be used\n");
			printf("   -h    print help\n");
			return 0;
		}
	}
	if (code == -9999){
		printf("Message undefined! Type %s -h for help.\n", argv[0]);
		return -1;
	} else if ( pin < 0){
		printf("Pin undefined! Input %s -h for help\n", argv[0]);
		return -1;
	}

	int pulseLength = 0; // not configurable

	if (wiringPiSetup () == -1){
		printf("ERROR! Unable to setup wiring pi. Abort.\n");
		 return -1;
	}
	printf("sending code[%i]\n", code);
	RCSwitch mySwitch = RCSwitch();
	if (protocol != 0){
		 mySwitch.setProtocol(protocol);
	}
	mySwitch.enableTransmit(pin);
	mySwitch.send(code, 24);

	return 0;

}
