gsm_logger
==========

Productive GSM data logger for Android
***
#### version 1.1.3.1 (29.11.2014)
* Fixed app crash while delete characters fast in Mark Name edit
* Possibly fixed GSM log headers

#### version 1.1.3 (20.11.2014)
* Added "Identification string" to settings
* Added current network type/generetaion, psc for UMTS and identification string to log
* Improved current network type info. Now generation and type show in main screen as separate line
* Fixed app crash on Samsung and Nexus (perhaps others) devices when using API 17+ option
* Changed "Try use API 17+" to true by default

#### version 1.1.2 (13.11.2014)
* Added preset categories for mark name (choose preset file in options)

#### version 1.1.1 (10.11.2014)
* Fixed LAC and CID (were swapped in csv)
* Fixed "Type" column in accelerometer log (now indicates "Linear"/"Raw")
* Fixed linear acceleration. Now uses build in sensor (available on Gingerbread 2.3 or above and if device has linear sensor)
* Added new method to find nearby cells on devices for Adnroid >=4.2 (check it in options)
* Added WakeLock to Service (data saves continuously when screen is off)
* Logger period moved to settings

#### version 1.1 (09.11.2014)
* Added accelerometer data to logger
* Added settings screen for accelerometer