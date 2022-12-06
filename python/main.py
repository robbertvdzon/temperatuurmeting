from machine import Pin, Timer
import machine
import utime
import usocket
import time
import network
import urequests
import network
import ubinascii

# Startup
print("Booting")

# Show that we wakeup
print("Signal led")
led.toggle()
time.sleep(50)
led.toggle()
time.sleep(50)
led.toggle()
time.sleep(50)
led.toggle()

# Connect to WLAN
print("Connect to WLAN")
wlan = network.WLAN(network.STA_IF)
wlan.active(True)
if not wlan.isconnected():
    wlan.connect("robbertkaren", "robbert12345")
    print("Waiting for Wi-Fi connection", end="...")
    while not wlan.isconnected():
        print(".", end="")
        time.sleep(1)
    print()
print("Connected")
mac = ubinascii.hexlify(network.WLAN().config('mac'),':').decode()
print("MAC address:"+mac)

led = machine.Pin("LED", machine.Pin.OUT)
sensor_temp = machine.ADC(4)

print("Measure")
rawreading = sensor_temp.read_u16()


print("Send to PI")
r = urequests.get("http://192.168.178.84:8000/post/"+mac+"/"+str(rawreading))
print(r.content)
r.close()

print("Send to Laptop")
r = urequests.get("http://192.168.178.55:8000/post/"+mac+"/"+str(rawreading))
print(r.content)
r.close()

print("Deep sleep")
machine.deepsleep(5000)


