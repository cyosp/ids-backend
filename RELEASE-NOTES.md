Release notes
-------------
##### 5.1.0 (2022-03-23)
 * Add configuration flag to define preview directory as static on server startup

##### 5.0.1 (2022-03-22)
 * Fix: Preview directory reversed order is not applied

##### 5.0.0 (2022-03-12)
 * Based on a configuration flag images can be public shared

##### 4.2.2 (2022-01-19)
 * Fix: Incorrect encoding on Docker volumes

##### 4.2.1 (2022-01-07)
 * Fix: Missing bc tool for generateAlternativeFormats.sh

##### 4.2.0 (2021-12-30)
 * Build Docker image for Linux ARM v7

##### 4.1.0 (2021-09-19)
 * Add user role on signin endpoint

##### 4.0.0 (2021-09-18)
 * Add deleteImage mutation

##### 3.2.1 (2021-09-08)
 * Fix: changePassword mutation parameters can be optionals

##### 3.2.0 (2021-09-08)
 * Check password has changed during changePassword mutation

##### 3.1.0 (2021-09-07)
 * Improve changePassword mutation errors details

##### 3.0.0 (2021-09-01)
 * Add changePassword mutation
 * Save user hashed password in correct User field
 * Remove password from User in GraphQL schema

##### 2.1.0 (2021-07-31)
 * Don't display hidden directories

##### 2.0.0 (2021-07-08)
* Set cache control to images and alternative formats
  Feature dedicated to @raoul2000

##### 1.5.0 (2021-07-01)
* Add script to generate alternative formats

##### 1.4.0 (2021-06-30)
* Alternative formats generation can be specified for a directory

##### 1.3.2 (2021-06-28)
* Add logs when alternate formats are created

##### 1.3.1 (2021-06-28)
* Fix: Missing JPEG libraries for Docker image

##### 1.3.0 (2021-06-27)
 * User registration can be disabled by configuration

##### 1.2.0 (2021-06-26)
 * Save logged user in an audit CSV file
 * Rename config folder into data folder

##### 1.1.0 (2021-06-25)
 * Set without distortion preview image maximum size to 1080 pixel 

##### 1.0.0 (2020-12-12)
 * Release IDS backend Jar and Docker image
