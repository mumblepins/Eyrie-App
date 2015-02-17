# Eyrie-App
Android app for the open source Eyrie Thermostat.

The Eyrie thermostat is intended to be a internet controlled, open source, semi-learning thermostat, based on the [Spark Core](http://spark.io), soon to (probably) be based on the Spark Photon.

See it in action:

<a href="http://www.youtube.com/watch?feature=player_embedded&v=mpitid8ROWk" target="_blank"><img src="http://img.youtube.com/vi/mpitid8ROWk/0.jpg" 
alt="IMAGE ALT TEXT HERE" width="240" height="180" border="10" /></a>

This is the Android app, which currently implements Spark cloud login, API key generation and fetching, and control of the thermostat.

## Other components of Eyrie Thermostat
* Still to come (will include custom shield, and Spark code)

### To-do list:
* Schedule can't be uploaded currently
* Don't create a new API key if we already have one for the current phone (Key name is based on Android serial number)
* ...

#####Distributed under the GPL v3 License

Uses code and icons from [RangeSeekBar](https://github.com/yahoo/android-range-seek-bar), heavily modified to work with more than two points, and to have temperature changes.
