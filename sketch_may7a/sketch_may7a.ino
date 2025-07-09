#include <ESP8266WiFi.h>
#include <Wire.h>
#include <Adafruit_ADS1X15.h>
#include <FirebaseESP8266.h>
#include <Ticker.h>
#include <math.h>

Adafruit_ADS1115 ads;

#define WIFI_SSID "GH"
#define WIFI_PASSWORD "panipu123"
#define FIREBASE_HOST "https://smartwatt-b1cca-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "AIzaSyCqBaNp2C5u-SNDqJmbbayN0S7t64RXWTo"

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

Ticker dataTicker;
volatile bool doSendData = false;

// Kalibrasi Sensor
const float ZMPT101B_SCALE = 312.5;
const float ACS712_SENSITIVITY = 0.100;
const float ADC_REF_VOLTAGE = 4.096;
const float ADC_RESOLUTION = 32767.0;
const int NUM_SAMPLES = 100;

float vMid = 1.65;

#define RELAY1_PIN D6
#define RELAY2_PIN D7

void IRAM_ATTR triggerData() {
  doSendData = true;
}

void setup() {
  Serial.begin(115200);  // 🔧 Tampilkan data ke serial monitor

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Menghubungkan WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi Terhubung");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  Wire.begin(D2, D1);
  ads.begin();

  pinMode(RELAY1_PIN, OUTPUT);
  pinMode(RELAY2_PIN, OUTPUT);

  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  autoCalibrateMidpoint();

  Serial.print("Offset arus (vMid): ");
  Serial.println(vMid, 4);

  dataTicker.attach(1, triggerData);
}

void loop() {
  if (doSendData) {
    doSendData = false;
    readAndSendData();
  }
}

void autoCalibrateMidpoint() {
  float sum = 0;
  for (int i = 0; i < NUM_SAMPLES; i++) {
    int16_t raw = ads.readADC_SingleEnded(1);
    float voltage = (raw * ADC_REF_VOLTAGE) / ADC_RESOLUTION;
    sum += voltage;
    delay(1);
  }
  vMid = sum / NUM_SAMPLES;
}

void readAndSendData() {
  if (WiFi.status() != WL_CONNECTED || !Firebase.ready()) {
    Serial.println("❌ Firebase belum siap atau WiFi tidak tersambung.");
    return;
  }

  ruanganHandler("ruangan1", 0, 1, RELAY1_PIN);
  ruanganHandler("ruangan2", 2, 3, RELAY2_PIN);
}

void ruanganHandler(String ruangan, uint8_t chVolt, uint8_t chCurr, int pinRelay) {
  float v = readVoltage(chVolt);
  float i = readCurrent(chCurr);
  float p = v * i;

  kontrolRelay(ruangan, pinRelay);

  // Pembulatan ke 2 angka di belakang koma
  float v_rounded = round(v * 100) / 100.0;
  float i_rounded = round(i * 100) / 100.0;
  float p_rounded = round(p * 100) / 100.0;

  Serial.printf("[%s] V = %.2f V\tI = %.2f A\tP = %.2f W\n", ruangan.c_str(), v_rounded, i_rounded, p_rounded);

  String path = "/monitoring/" + ruangan + "/";

  // Tetap kirim sebagai number (float), tapi sudah dibulatkan
  Firebase.setFloat(fbdo, path + "tegangan", v_rounded);
  Firebase.setFloat(fbdo, path + "arus", i_rounded);
  Firebase.setFloat(fbdo, path + "daya", p_rounded);
}



float readVoltage(uint8_t channel) {
  float sum = 0;
  for (int i = 0; i < NUM_SAMPLES; i++) {
    int16_t raw = ads.readADC_SingleEnded(channel);
    float voltage = (raw * ADC_REF_VOLTAGE) / ADC_RESOLUTION;
    sum += voltage * voltage;
    delay(1);
  }
  float rms = sqrt(sum / NUM_SAMPLES);
  return rms * ZMPT101B_SCALE;
}

float readCurrent(uint8_t channel) {
  float sum = 0;
  for (int i = 0; i < NUM_SAMPLES; i++) {
    int16_t raw = ads.readADC_SingleEnded(channel);
    float voltage = (raw * ADC_REF_VOLTAGE) / ADC_RESOLUTION;
    float current = (voltage - vMid) / ACS712_SENSITIVITY;
    sum += current * current;
    delay(1);
  }
  float rms = sqrt(sum / NUM_SAMPLES);
  return (rms < 0.05) ? 0.0 : rms;
}

void kontrolRelay(String ruangan, int pinRelay) {
  String path = "/monitoring/" + ruangan + "/relay";
  if (Firebase.getBool(fbdo, path)) {
    bool status = fbdo.boolData();
    digitalWrite(pinRelay, status ? LOW : HIGH);
    Serial.printf("[%s] Relay: %s\n", ruangan.c_str(), status ? "ON" : "OFF");
  } else {
    Serial.println("Gagal membaca status relay dari Firebase: " + path);
  }
}
