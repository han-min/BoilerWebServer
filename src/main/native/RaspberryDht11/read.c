#include <wiringPi.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <ctype.h>
#include <stdbool.h>

#define MAXTIMINGS      85
#define MAX_ATTEMPT	20

bool m_verbose = FALSE;
float m_tempc = -9999;
float m_hum = -9999;

int dht11_dat[5] = { 0, 0, 0, 0, 0 };

/*

	This reads the DHT11 on the Raspberry Pi using WiringPi.
	You'll have to install WiringPi to use this.

	To build, use this command
		gcc -o read read.c -lwiringPi -lwiringPiDev

	To read from pin 7 (wiringPi numbering) and return 't'emperature
		sudo ./read -p 7 -d t
	

*/

bool read_dht11_dat(int pin){
        uint8_t laststate       = HIGH;
        uint8_t counter         = 0;
        uint8_t j               = 0, i;
        float   f;

        dht11_dat[0] = dht11_dat[1] = dht11_dat[2] = dht11_dat[3] = dht11_dat[4] = 0;

        pinMode( pin, OUTPUT );
        digitalWrite( pin, LOW );
        delay( 18 );
        digitalWrite( pin, HIGH );
        delayMicroseconds( 40 );
        pinMode( pin, INPUT );

        for ( i = 0; i < MAXTIMINGS; i++ )
        {
                counter = 0;
                while ( digitalRead( pin ) == laststate )
                {
                        counter++;
                        delayMicroseconds( 1 );
                        if ( counter == 255 )
                        {
                                break;
                        }
                }
                laststate = digitalRead( pin );

                if ( counter == 255 )
                        break;

                if ( (i >= 4) && (i % 2 == 0) )
                {
                        dht11_dat[j / 8] <<= 1;
                        if ( counter > 16 )
                                dht11_dat[j / 8] |= 1;
                        j++;
                }
        }

        if ( (j >= 40) &&
             (dht11_dat[4] == ( (dht11_dat[0] + dht11_dat[1] + dht11_dat[2] + dht11_dat[3]) & 0xFF) ) )
        {
                if (m_verbose){
			f = dht11_dat[2] * 9. / 5. + 32;
	                printf( "Humidity = %d.%d %% Temperature = %d.%d C (%.1f F)\n",
	                        dht11_dat[0], dht11_dat[1], dht11_dat[2], dht11_dat[3], f );
		}
		m_tempc = dht11_dat[2];
		m_hum = dht11_dat[0];
		return TRUE;
        } else  {
		if (m_verbose){
	                printf( "Data not good, skip\n" );
		}
        }
	return FALSE;
}

int main (int argc, char **argv){

  int c;
  int pin = -1;
  char datatype = 'a'; //'a' all, 't' temp 'h' humidity

  while ((c = getopt (argc, argv, "vhd:p:")) != -1){
    switch (c){
      case 'v':
	m_verbose = TRUE;
	break;
      case 'd':
	datatype = optarg[0];
	break;
      case 'p':
        pin = atoi(optarg);
        break;
      case 'h':
      default:
        printf("Help\n");
	printf("  -v           verbose\n");
        printf("  -p <pin>     pin number used for data.\n");
        printf("  -d <a, t, h> all, temperature or humidity.\n");
	printf("  -h           print help.\n");
        return 0;
      }
  }
  // check pin is specified
  if (pin < 0){
	printf("Error: Pin not specified.\n");
	return(-1);
  }
  // echo output
  if (m_verbose){
	  printf ("pin %d, data: %c\n", pin, datatype);
  }
  // start wiring pi
  if ( wiringPiSetup() == -1 ){
	printf("Error setting up wiringPi.\n");
	exit( -1 );
  }
  // attempt to read until we get a result
  int attempt=0;
  while(read_dht11_dat(pin) == FALSE && attempt < MAX_ATTEMPT){
	attempt++;
  }
  // echo result
  if (m_verbose){
	  printf("Got %0.1f %0.1f \n", m_hum, m_tempc);
  }
  // print only the result requested
  switch(datatype){
	case 'h':
		printf("%0.1f \n", m_hum);
		break;
	case 't':
		printf("%0.1f \n", m_tempc);
		break;
	case 'a':
		printf("%0.1f %0.1f\n", m_tempc, m_hum);
  }

  return 0;
}

