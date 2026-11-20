#!/usr/bin/env bash
set -euo pipefail
# Runs on a fresh CI emulator with only synthetic test data.
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell svc wifi disable
adb shell svc data disable

instrument() {
  local result
  result=$(adb shell am instrument -w "$@" com.nourish.app.test/androidx.test.runner.AndroidJUnitRunner)
  echo "$result"
  grep -Eq '^OK \(1 test\)' <<< "$result"
}

instrument -e class com.nourish.app.DiaryStorageTest
instrument -e class com.nourish.app.NourishAppTest -e phase create
adb shell am force-stop com.nourish.app
instrument -e class com.nourish.app.NourishAppTest -e phase reopen
adb exec-out run-as com.nourish.app cat files/diary-test.png > app/build/diary-test.png

instrument -e class com.nourish.app.InputMigrationTest
instrument -e class com.nourish.app.InputFlowTest
adb exec-out run-as com.nourish.app cat files/photo-input-test.png > app/build/photo-input-test.png

instrument -e class com.nourish.app.MealDraftTest
adb exec-out run-as com.nourish.app cat files/meal-draft-test.png > app/build/meal-draft-test.png

instrument -e class com.nourish.app.ConfirmedChatTest
adb exec-out run-as com.nourish.app cat files/confirmed-chat-test.png > app/build/confirmed-chat-test.png
