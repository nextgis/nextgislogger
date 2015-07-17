// Arduino Uno

#include <SoftwareSerial.h>
#include <DHT.h>

// DHT22 at pin 12
#define DHTPIN 12;
DHT dht(DHTPIN, DHT22);

// Bluetooth at TX = 10, RX = 11
SoftwareSerial bt(10, 11);

// order, short and full name, unit of all sensors in json
String pnp = "{\"0\":{\"short\":\"Temp\",\"full\":\"Temperature\",\"unit\":\"°C\"},\"1\":{\"short\":\"Hum\",\"full\":\"Humidity\",\"unit\":\"%\"}}";

void setup() {
  bt.begin(9600);
  dht.begin();
}

void loop() {
  if (bt.available()) {
    int val = bt.read();

    // get json
    if (val == 'h' || val == 'H') {
      bt.println(pnp);
    }

    // get data
    if (val == 'd' || val == 'D') {
      // read data from sensors
      float h = dht.readHumidity();
      float t = dht.readTemperature();

      // send it to Bluetooth
      bt.print(t, 0); bt.print(';');
      bt.println(h, 0);
    }
  }

  // delay should be lesser than Android update interval
  // NextGIS Logger interval default 100
  delay(10);
}
