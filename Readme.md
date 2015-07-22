# Jenkinspider
Jenkinspider is a plugin developed to be used in Jenkins to scan builds

### Prerequisite
Things that are needed to
* AppSpider Enterprise Rest URL
* Username and password to the AppSpider Enterprise Server

### Version
0.0.1

### Steps
1. Build the hpi file (See How to build the hpi file section below)

2. In the browser, go to your jenkins server

3. Navigate to Manage Jenkins > Manage Plugins 

4. Navigate to the Advance tab

5. In the Upload Plugin section, click on the 'Choose file' button

6. Navigate to where the hpi file was created

7. Click on the Upload button

### How to build the hpi file
1. Clone the git repository

    ```sh
    $ git clone git@github.com:nbugash-r7/jenkinspider.git
    ```
    
2. Change directory to the jenkinspider repository
    ```sh
    $ cd jenkinspider
    ```
    
3. A. Build the hpi file. For first time build run: 

    ```sh
    $ mvn hpi:run
    ```
    
when the build is complete, kill the session by CTRL + C. For successive build:

    ```sh
    $ mvn hpi:hpi
    ```