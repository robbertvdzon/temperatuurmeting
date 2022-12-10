#!/bin/sh
scp build/libs/temperatuurmeting.jar  robbert@192.168.178.84:/home/robbert
ssh robbert@192.168.178.84 "killall java; cd /home/robbert; java -jar temperatuurmeting.jar &"