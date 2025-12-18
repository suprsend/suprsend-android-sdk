#!/bin/bash
# sh connect.sh
# sh connect.sh 28115REWR57530
# Optional device id (e.g. emulator-5554 or R9XXXX)
DEVICE_ID="$1"

# Helper to run adb with or without -s
adb_cmd() {
  if [ -n "$DEVICE_ID" ]; then
    adb -s "$DEVICE_ID" "$@"
  else
    adb "$@"
  fi
}

# Restart adb in TCP/IP mode on port 5555
echo "🔄 Restarting adb in tcpip mode..."
adb_cmd tcpip 5555

# Wait for a second to ensure it restarts
sleep 2

# Get the device IP address
DEVICE_IP=$(adb_cmd shell ip route | awk '{print $9}')

if [ -z "$DEVICE_IP" ]; then
  echo "❌ Could not determine device IP."
  echo "   Make sure the device is connected, authorized, and reachable."
  exit 1
fi

echo "📱 Device IP found: $DEVICE_IP"

# Connect to the device over Wi-Fi
echo "🔌 Connecting to $DEVICE_IP:5555..."
CONNECT_OUTPUT=$(adb_cmd connect "$DEVICE_IP:5555" 2>&1)
echo "$CONNECT_OUTPUT"

if echo "$CONNECT_OUTPUT" | grep -q "connected to"; then
  echo "✅ Done! You are now connected over Wi-Fi."
else
  echo "❌ Failed to connect. Check Wi-Fi and device state."
fi