# NutriTracker

An Android app for keeping a food diary of **calories, carbohydrates, sugars and sodium**.

## Features

- **Diary entries** with a date/time stamp and a **Breakfast / Lunch / Dinner / Snack** tag
  (the tag defaults from the time of day and can be changed).
- **Scan a Nutrition Facts label** with the camera (or pick a photo). Text is read on-device
  with Google ML Kit Text Recognition, and the app pulls out the per-serving *Serving size*,
  *Calories*, *Total Carbohydrate*, *Total Sugars* and *Sodium*. You review and can correct
  every value before saving.
- **Serving size and servings eaten** sit next to the nutrient fields. Each nutrient shows the
  per-serving value and the total for the amount you ate.
- **Totals** for each meal and for the whole day, plus a **history** of every logged day with
  its totals. Tap a day to open it.
- Data is stored locally on the device (Room/SQLite). Nothing is sent to a server.

## Building

Open the project in Android Studio (Ladybug or newer), or run:

```
./gradlew assembleDebug        # build the APK
./gradlew testDebugUnitTest    # run the label-parser unit tests
```

Requires JDK 17+ and the Android SDK (compileSdk 35). minSdk is 26 (Android 8.0).

## Notes on label scanning

The parser targets the US Nutrition Facts format (line names as defined in
[21 CFR 101.9](https://www.ecfr.gov/current/title-21/chapter-I/subchapter-B/part-101/subpart-A/section-101.9)).
OCR is not perfect, so curved packages, glare or small print can produce missed or wrong values.
That's why scanned values always land in the editable form instead of being saved directly.
On dual-column labels the first (per-serving) column is used.

## Project layout

```
app/src/main/java/com/kenkho/nutritracker/
  data/   Room entity (FoodEntry), DAO, database
  ocr/    NutritionLabelParser (pure Kotlin, unit tested) and LabelTextReader (ML Kit glue)
  ui/     Compose screens: Diary, History, Add/Edit entry, Scan; ViewModels
```
