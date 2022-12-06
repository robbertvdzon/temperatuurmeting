from machine import Pin, Timer
import machine
import utime
import usocket
import time
import network
import urequests
import network
import ubinascii

def blink(timer):
    measure()

def measure():
    led.toggle()
    rawreading = sensor_temp.read_u16()
    mac = ubinascii.hexlify(network.WLAN().config('mac'),':').decode()
    r = urequests.get("http://192.168.178.55:8000/post/"+mac+"/"+str(rawreading))
    print(r.content)
    r.close()

def registerStart():
    mac = ubinascii.hexlify(network.WLAN().config('mac'),':').decode()
    r = urequests.get("http://192.168.178.55:8000/start/"+mac)
    print(r.content)
    r.close()

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
print(mac)

led = machine.Pin("LED", machine.Pin.OUT)
sensor_temp = machine.ADC(4)
timer = Timer()
registerStart()
measure()



timer.init(freq=1/120, mode=Timer.PERIODIC, callback=blink)

