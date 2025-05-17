#include <ESP8266WiFi.h>
#include <Wire.h>
#include <Adafruit_ADS1X15.h>
#include <FirebaseESP8266.h>
#include <Ticker.h>
#include <math.h>

Adafruit_ADS1115 ads;

#define WIFI_SSID "RAHMA"
#define WIFI_PASSWORD "bahri0912$"
#define FIREBASE_HOST "https://smartwatt-b1cca-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "AIzaSyCqBaNp2C5u-SNDqJmbbayN0S7t64RXWTo"

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

Ticker dataTicker;
volatile bool doSendData = false;

const int currentOffset = 20000;
const float currentFactor = 1.0 / 800.0;
const float ZMPT101B_SCALE = 220.0 / 1.65;
const float MAX_POWER = 450.0; // Watt maksimum per ruangan

#define RELAY1_PIN D5
#define RELAY2_PIN D6
#define RELAY3_PIN D3
#define RELAY4_PIN D4

void IRAM_ATTR triggerData() {
  doSendData = true;
}

void setup() {
  Serial.begin(9600);
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
  pinMode(RELAY3_PIN, OUTPUT);
  pinMode(RELAY4_PIN, OUTPUT);

  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  dataTicker.attach(2, triggerData);
}

void loop() {
  if (doSendData) {
    doSendData = false;
    readAndSendData();
  }
}

void readAndSendData() {
  if (WiFi.status() != WL_CONNECTED || !Firebase.ready()) {
    Serial.println("Firebase belum siap atau WiFi tidak tersambung.");
    return;
  }

  // Handler untuk dua ruangan
  ruanganHandler("ruangan1", 0, 1, RELAY1_PIN, RELAY2_PIN);
  ruanganHandler("ruangan2", 2, 3, RELAY3_PIN, RELAY4_PIN);
}

void ruanganHandler(String ruangan, uint8_t chVolt, uint8_t chCurr, int pinRelay1, int pinRelay2) {
  float v = readVoltage(chVolt);
  float i = readCurrent(chCurr);
  float p = v * i;

  // if (p > MAX_POWER) {
  //   digitalWrite(pinRelay1, LOW);
  //   digitalWrite(pinRelay2, LOW);
  //   Serial.printf("%s: Daya %.1fW melebihi batas! Relay dimatikan.\n", ruangan.c_str(), p);
  // } else {
  //   kontrolRuangan(ruangan, pinRelay1, pinRelay2);
  // }

  kontrolRuangan(ruangan, pinRelay1, pinRelay2);

  Serial.printf("%s: V=%.1fV I=%.2fA P=%.1fW\n", ruangan.c_str(), v, i, p);
}

// Mengontrol relay berdasarkan status dari Firebase
void kontrolRuangan(String ruangan, int pinRelay1, int pinRelay2) {
  String path = "/monitoring/" + ruangan + "/";
  
  // Membaca status relay dari Firebase
  bool statusRelay1 = getRelayStatus(path + (ruangan == "ruangan1" ? "relay1" : "relay3"));
  bool statusRelay2 = getRelayStatus(path + (ruangan == "ruangan1" ? "relay2" : "relay4"));

  // Memastikan relay sesuai dengan status Firebase
  digitalWrite(pinRelay1, statusRelay1 ? HIGH : LOW);
  digitalWrite(pinRelay2, statusRelay2 ? HIGH : LOW);

  Serial.printf("%s: Relay1: %s, Relay2: %s\n", ruangan.c_str(), (statusRelay1 ? "ON" : "OFF"), (statusRelay2 ? "ON" : "OFF"));
}

// Membaca status relay dari Firebase
bool getRelayStatus(String path) {
  if (Firebase.getBool(fbdo, path)) {
    bool status = fbdo.boolData();
    Serial.println("Status relay dari Firebase: " + String(status ? "ON" : "OFF"));
    return status;
  } else {
    Serial.println("Gagal membaca status relay dari: " + path + " -> " + fbdo.errorReason());
    return false;  // Jika gagal, defaultkan ke OFF
  }
}

// Fungsi untuk membaca tegangan
float readVoltage(uint8_t channel) {
  float sum = 0;
  for (int i = 0; i < 100; i++) {
    int16_t raw = ads.readADC_SingleEnded(channel);
    float v = (raw * 4.096) / 32767.0;
    sum += v * v;
    delay(1);
  }
  return sqrt(sum / 100.0) * ZMPT101B_SCALE;
}

// Fungsi untuk membaca arus
float readCurrent(uint8_t channel) {
  float sum = 0;
  for (int i = 0; i < 100; i++) {
    int16_t raw = ads.readADC_SingleEnded(channel);
    float current = (raw - currentOffset) * currentFactor;
    sum += current * current;
    delay(1);
  }
  float rms = sqrt(sum / 100.0);
  return (rms < 0.05) ? 0.0 : rms;
}
