#!/system/bin/sh
# Pull BYD JARs to USB storage

DST=/storage/4A21-0000/Download/byd_jars
mkdir -p $DST

for dir in /system/framework /system_ext/framework /vendor/framework /product/framework; do
  [ -d "$dir" ] || continue
  for jar in "$dir"/*.jar; do
    [ -f "$jar" ] || continue
    name=$(basename "$jar")
    case "$name" in
      *byd*|*BYD*|*auto*|*Auto*|*instrument*|*Instrument*|*hud*|*HUD*|*navi*|*Navi*|*car*|*Car*|*vehicle*|*Vehicle*)
        cp "$jar" $DST/ 2>/dev/null
        echo "copied: $jar"
        ;;
    esac
  done
done

# Boot classpath JARs too
for jar in $(getprop ro.boot.classpath | tr ':' ' '); do
  [ -f "$jar" ] && cp "$jar" $DST/ 2>/dev/null && echo "boot: $jar"
done

echo "=== Pulled ==="
ls -la $DST/