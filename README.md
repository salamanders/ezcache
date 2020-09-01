
# EZ-Cache
"Cache some stuff."

A toy app to try Kotlin 1.4 + GAE + JRE 11


## Dev Setup

Gcloud console: create new project 'ezcache'

    mvn package ?
    gcloud components update
    gcloud auth login
    gcloud config set project ezcache
    mvn package com.google.cloud.tools:appengine-maven-plugin:run # does not work https://github.com/GoogleCloudPlatform/app-maven-plugin/issues/390
    mvn com.google.cloud.tools:appengine-maven-plugin:deploy # does not work.
    mvn package appengine:deploy # does not work.
    gcloud app deploy

https://ezcache.appspot.com