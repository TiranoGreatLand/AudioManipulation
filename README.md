# AudioManipulation
Java Speech processing audio segmentation detection

It is a Java program to achieve such functions:

1. transfer other format of audio file(such as mp3, or other format but I've just test mp3) into audio file in wav format(maybe other format is also access)

2. audio segment

3. onset detection

It is a rough code and adjustment is needed.

I may update it in fulture.

Now the new application is updated.
It is in the new Branch 'EngergyAndZero' in this repository.
The file 'UtterManipulation' is an Android Project with CPP code. The CPP code is webRTC's VAD part. 
The Android project read audio wave file and write audio wave file by Java code, and the function of VAD is via CPP code.
