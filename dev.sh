#!/bin/zsh
# stage 0 is used for the pure render plugin, can find another method later
watchify -t [ babelify --stage 0 ] client/client.jsx -o client/build/app.js &
nodemon server/Server.js
