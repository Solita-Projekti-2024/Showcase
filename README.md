
# ShowcaseApp

## Yleiskatsaus
ShowcaseApp on Android-sovellus, joka yhdistyy Movesense-anturiin seuratakseen liikettä ja sykettä. Sovellus käsittelee anturidataa ja voi luoda HL7 FHIR -viestejä kaatumisten ja korkean sykkeen hälytysten varalta. Se on rakennettu Java-kielellä ja hyödyntää Bluetooth-yhteyttä anturidatan vastaanottamiseen
## Ominaisuudet
-	Yhdistää Movesense-liikeanturiin Bluetoothin kautta
-	Kerää ja käsittelee kiihtyvyysanturi- ja syketietoja
-	Tunnistaa kaatumiset ja korkean sykkeen tapahtumat
-	Luo HL7 FHIR -viestejä
-	Tallentaa ja visualisoi anturidataa
## Sovelluksen kulku
1. InfoActivity (info.java) - Päänäkymä, josta käyttäjä aloittaa.
2. MovesenseActivity (MovesenseActivity.java) - Käsittelee anturin Bluetooth-yhteyden.
3. MultiSensorSubscribeActivity (MultiSensorSubscribeActivity.java) - Tilaa useita anturidatasignaaleja ja kerää tietoa.
4. Datan käsittely - Tunnistaa poikkeavuudet, kuten kaatumiset tai korkean sykkeen.
5. FhirMessageCreator (FhirMessageCreator.java) - Luo HL7 FHIR -viestit, jos poikkeavuus havaitaan.
6. Lokitus ja visualisointi - Anturidata tallennetaan ja näytetään.
7. Paluu InfoActivityyn tai poistuminen - Käyttäjä voi irrottaa anturin tai sulkea sovelluksen.
## Asennus ja käyttöönotto
1.	Kloonaa repositorio: 
2.	git clone <repository_url>
3.	Avaa projekti Android Studiossa. 
* movesense/movesense-movesense-mobile-lib-c5a784a22b05/android/Movesense
4.	Synkronoi Gradle ja asenna riippuvuudet.
5.	Suorita sovellus Android-laitteella, jossa on Bluetooth käytössä.
## Käyttöohjeet
1.	Käynnistä sovellus ja myönnä tarvittavat käyttöoikeudet.
2.	Yhdistä Movesense-anturiin.
3.	Seuraa liikettä ja sykettä reaaliajassa.
4.	Tarkastele tallennettua anturidataa.
# ShowcaseApp
## Overview
ShowcaseApp is an Android application that connects to Movesense sensor to monitor motion and heart rate. The app processes sensor data and can generate HL7 FHIR messages for fall detection and high heart rate alerts. It is built using Java and leverages Bluetooth communication for sensor data retrieval.
## Features
-	Connects to Movesense motion sensor via Bluetooth
-	Collects and processes accelerometer and heart rate data
-	Detects falls and high heart rate events
-	Generates HL7 FHIR messages
-	Logs and visualizes sensor data
## Activity Flow
1. InfoActivity (info.java) - The main screen where the user starts.
2. MovesenseActivity (MovesenseActivity.java) - Handles sensor connection via Bluetooth.
3. MultiSensorSubscribeActivity (MultiSensorSubscribeActivity.java) - Subscribes to sensors and collects data.
4. Data Processing - Detects anomalies such as falls or high heart rate.
5. FhirMessageCreator (FhirMessageCreator.java) - Creates HL7 FHIR messages if an anomaly is detected.
6. Logging and Visualization - Sensor data is stored and displayed.
7. Return to InfoActivity or Exit - User can disconnect or close the app.
## Installation & Setup
1.	Clone the repository: 
2.	git clone <repository_url>
3.	Open the project in Android Studio.
* movesense/movesense-movesense-mobile-lib-c5a784a22b05/android/Movesense
4.	Sync Gradle and install dependencies.
5.	Run the application on an Android device with Bluetooth enabled.
## Usage
1.	Launch the app and allow necessary permissions.
2.	Connect to a Movesense sensor.
3.	Monitor motion and heart rate in real time.
4.	View logged sensor data.


