# DoorPi-App - NOT BEING MAINTAINED
Android Application for DoorPi Project. Published on Google Play Store [here](https://play.google.com/store/apps/details?id=com.jedeboer.doorpi)

Works together with [DoorPi-Server](https://github.com/Jyppino/DoorPi-Server).


## The Idea
- Run the server on a Raspberry Pi hooked up to an electronic lock of your front door. 
- Connect to the server using this Android application 
  - Both server and android device need to be on the same network
- First device to connect can setup the server and becomes admin
- Admin can manage devices (add + remove), view statistics and create other admins
- New devices are added by scanning a QR Code
- New devices create a public / private key pair using the Android Keystore and register their public key
- Verification is done by encrypting a 'challenge' server side using the registered public key
  - Challenges have a short expirary date
  - Answered by decrypting challenge using private key, which is stored locally
  - Access to private key is dependent on fingerprint verification
 - Upon successful verification the door is unlocked using the GPIO of the Raspberry Pi 
 
 ## Screenshots
 ![Image 1](https://github.com/Jyppino/DoorPi-App/blob/main/screenshots/app_1.jpeg)
 ![Image 2](https://github.com/Jyppino/DoorPi-App/blob/main/screenshots/app_2.jpeg)
 ![Image 3](https://github.com/Jyppino/DoorPi-App/blob/main/screenshots/app_3.jpeg)
 ![Image 4](https://github.com/Jyppino/DoorPi-App/blob/main/screenshots/app_4.jpeg)
 ![Image 5](https://github.com/Jyppino/DoorPi-App/blob/main/screenshots/app_5.jpeg)
 ![Image 6](https://github.com/Jyppino/DoorPi-App/blob/main/screenshots/app_6.jpeg)
