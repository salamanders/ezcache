
# EZ-Cache
"Cache some stuff."

A toy app to try Kotlin 1.4 + GAE + JRE 11


## Dev Setup

Gcloud console: create new project 'ezcache'

    gcloud components update
    gcloud auth login
    gcloud config set project ezcache
    # Run the Application.kt for local debug
    mvn package && gcloud app deploy --quiet

https://ezcache.appspot.com
https://ezcache.appspot.com/static/ktor_logo.svg
https://ezcache.appspot.com/session/increment
https://ezcache.appspot.com/json/gson

