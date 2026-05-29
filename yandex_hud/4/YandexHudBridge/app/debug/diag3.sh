#!/system/bin/sh
# HUD DIAG v3 - run from Bugjaeger ADB shell
# Output to USB storage on car

LOG=/storage/4A21-0000/Download/hud_diag3.log
echo "=== HUD DIAG3 $(date) ===" > $LOG

echo "--- A: Interface descriptor ---" >> $LOG
service call autoservice 1598968902 >> $LOG 2>&1

FID=1275068440
VAL=3

echo "--- B: Safe read probes ---" >> $LOG
service call autoservice 4 >> $LOG 2>&1
service call autoservice 5 >> $LOG 2>&1
service call autoservice 7 i32 0 >> $LOG 2>&1

echo "--- C: Format A: i32 1 i32 FID i32 1 i32 VAL ---" >> $LOG
for tx in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 25 30; do
  echo "TX=$tx:" >> $LOG
  service call autoservice $tx i32 1 i32 $FID i32 1 i32 $VAL >> $LOG 2>&1
done

echo "--- D: Format B: i32 devtype i32 1 i32 FID i32 1 i32 VAL ---" >> $LOG
for dt in 1001 1007 1009 1013; do
  for tx in 1 2 3 4 5 6 7 8 9 10; do
    echo "TX=$tx dt=$dt:" >> $LOG
    service call autoservice $tx i32 $dt i32 1 i32 $FID i32 1 i32 $VAL >> $LOG 2>&1
  done
done

echo "--- E: Format C: i32 1 i32 FID i32 VAL ---" >> $LOG
for tx in 1 2 3 4 5 6 7 8 9 10; do
  echo "TX=$tx:" >> $LOG
  service call autoservice $tx i32 1 i32 $FID i32 $VAL >> $LOG 2>&1
done

echo "--- F: dumpsys autoservice ---" >> $LOG
dumpsys autoservice >> $LOG 2>&1

echo "--- G: Framework JARs ---" >> $LOG
ls -la /system/framework/*.jar >> $LOG 2>&1
ls -la /system_ext/framework/*.jar >> $LOG 2>&1
ls -la /vendor/framework/*.jar >> $LOG 2>&1

echo "--- H: Boot classpath ---" >> $LOG
getprop ro.boot.classpath >> $LOG 2>&1

echo "--- I: Service list BYD ---" >> $LOG
service list 2>/dev/null | grep -i -E 'byd|auto|navi|hud|instrument|car|vehicle|map' >> $LOG 2>&1

echo "=== DONE ===" >> $LOG