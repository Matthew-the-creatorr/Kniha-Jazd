# Kniha jázd – Visual Prototype 0.1

Vizuálny Android prototyp v Kotline a Jetpack Compose. **Neobsahuje GPS sledovanie, AI, backend ani skutočnú registráciu.** Zadané údaje sú len v pamäti aplikácie. Automobil v pozadí je zatiaľ štylizovaná vektorová silueta, nie presný 3D model.

## Testovacia APK
1. Otvor kartu **Actions**.
2. Vyber **Build Android APK** a úspešný beh.
3. V sekcii **Artifacts** stiahni **KnihaJazd-debug-apk**.
4. Rozbaľ ZIP a nainštaluj **app-debug.apk** na Android.

## Obsah
- Registrácia len ako demo, výber značky, modelu, EČV, farby a povrchu.
- Tri sekcie, ikony v spodnej lište a horizontálny swipe.
- Prázdna kniha jázd, analytika a profil vozidla.
- Demo štart a ukončenie, bez záznamu polohy.
- Výber vozidla zatiaľ nie je kompletný katalóg.

## Lokálne zostavenie
Android Studio / JDK 17 / Android SDK 35. Build APK alebo Gradle 8.9: `gradle :app:assembleDebug`.
