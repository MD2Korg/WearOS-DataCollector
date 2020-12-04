# MD2K WearOS Data Collection Platform
This software is designed to collect accelerometer, gyroscope, and PPG data from the Fossil-series smartwatches.    

## Disclaimer
This software is intended for informational and demonstration purposes only and is not designed to diagnose, treat, cure, prevent, or track disease or health states. No content provided in this software is intended to serve as a substitute for any kind of professional (e.g., medical) advice.

# Installation Instructions

## Fossil Watch
You need to enable developer options and connect the watch to Android Studio eight through Wi-Fi or Bluetooth [https://developer.android.com/training/wearables/apps/debugging](https://developer.android.com/training/wearables/apps/debugging).  
We recommends the Wi-Fi open since data retrieval will be quite a bit faster after you have collected some data.


## Android Studio
1. Open Android Studio and `File -> New -> Project from Version Control`
2. Enter the URL of this repo. `https://github.com/MD2Korg/WearOS-DataCollector`
3. Build and run the app on a Fossil smartwatch


## Google Play
This app is available on Google's Play Store.  Please email dev@md2k.org for an invitation to the closed testing program while we continue to finalize the app for release on the production system.


# Collecting and Accessing Data
The watch app is straight forward and once you launch the app, there will be three toggle buttons, one for each of the sensors (accelerometer, gyroscope, and PPG).  
By toggling a sensor on, the watch will begin collecting data and logging to its internal memory. The watch should sample the accelerometer and gyroscope sensors 
at 50hz and the PPG sensor at ~100hz.  

All data is stored in chuncked, gzip-compressed csv files in the main user directory on the watch.  You have two options to pull this data from the watch back to a computer:
1. Android Studio has a `Device File Explorer` where you can navigate to `/sdcard` and select and copy the files to your local directory of choice.
2. ADB tools can copy this data from a commandline to your computer.  `adb pull /sdcard /local/path/to/store/data`

In both scenarios, you should delete the files from the watch once they are safely stored on your computer to ensure sufficient space is available for future data collections.

   
## Contributors

Link to the [list of contributors](https://github.com/MD2Korg/WearOS-DataCollector/graphs/contributors) who participated in this project.

## License

This project is licensed under the BSD 2-Clause - see the [license](https://github.com/MD2Korg/WearOS-DataCollector/blob/master/LICENSE) file for details.

## Acknowledgments

* [National Institutes of Health](https://www.nih.gov/) - [Big Data to Knowledge Initiative](https://datascience.nih.gov/bd2k)
  * Grants: R01MD010362, 1UG1DA04030901, 1U54EB020404, 1R01CA190329, 1R01DE02524, R00MD010468, 3UH2DA041713, 10555SC, P41 EB028242
* [National Science Foundation](https://www.nsf.gov/)
  * Grants: 1640813, 1722646
* [Intelligence Advanced Research Projects Activity](https://www.iarpa.gov/)
  * Contract: 2017-17042800006
