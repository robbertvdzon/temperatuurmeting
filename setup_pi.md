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

# Install java:
sudo apt install openjdk-17-jdk -y

# Install temperatuurmeting:
mkdir ~/git
cd ~/git
git clone https://github.com/robbertvdzon/temperatuurmeting

# Initial build and run the code:
cd ~/git/temperatuurmeting
./update_and_run.sh

# Add the folling to  "crontab -e"
@reboot sh /home/robbert/temperatuurmeting.sh

# Create /home/robbert/temperatuurmeting.sh:
#!/bin/sh
echo startting >> /tmp/temperatuurmeting.log
cd /home/robbert/git/temperatuurmeting
./run.sh

# Change permissions:
chmod a+x /home/robbert/temperatuurmeting.sh
