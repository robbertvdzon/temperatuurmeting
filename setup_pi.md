# Install pi:
https://www.raspberrypi.com/software/
Download Raspberry Pi Imager on osx
Run “Raspberry Pi Imager” on osx
Choose OS: “Raspberry PI OS (32 bit)”
Write to SD card
Place SD card in PI and startup (with display and keyboard/mouse)
perform the following steps with a screen and keyboard/mouse on the PI
finish installation (choose country and language)
username: robbert
passwd: <secret>
connect to wifi
open raspberry pi configuration
enable: ssh
save
Finish the rest of the installation using ssh
ssh pi@192.168.178.88

# Update the system:
sudo apt-get update
sudo apt-get upgrade
sudo rpi-update

# Install java:
sudo apt install openjdk-8-jdk
(note, we need java8. Later versions are not supported by the used pi4j library)

# Install schaakrob:
mkdir ~/git
cd ~/git
git clone https://github.com/robbertvdzon/schaakrob

# Initial build and run the code:
cd ~/git/schaakrob
./update_and_run.sh

# Add the folling to  "crontab -e"
@reboot sh /home/pi/ui.sh

# Create /home/pi/ui.sh:
#!/bin/sh
echo startting >> /tmp/schaak.log
FILE=/tmp/rebuildui
cd /home/pi/git/schaakrob
while :
do
if test -f "$FILE"; then
echo "rebuild"
sudo /usr/bin/git pull
sudo ./mvnw package
rm $FILE
fi
java -jar schaakrob-server/target/schaakrob-server-1.0-shaded.jar >> /tmp/robot.log  2>&1
sleep 2
done

# Change permissions:
chmod a+x /home/pi/ui.sh

# After starting the server for the first time: fill in the calibration in the 'manual' section
A8: 16470,13130
H1: 4420,900
A11:2200,13000
H10: 400,850
A21: 20410,12700
H20:18600,450
Pakker hoogte: -1900
Snelheid: 0.8

# optional: check i2c status
i2cdetect -y 1

# optional: check gpio status:
gpio readall

# optional: If you afterwards needs to enable an interface (ssh, i2c, SPI):
sudo raspi-config
choose: 3 Interface Options

# optional: When you want to change the default java version:
#SKIP THIS STEP!!#sudo update-alternatives --config java
