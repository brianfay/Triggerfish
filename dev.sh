#!/bin/zsh
# I know that gulp and grunt were made for this sort of thing but I mean come'on do I really need all of that?

# live-reload res/index.html res/bundle.js --port=9999 &
watchify -t reactify client/client.jsx -o res/bundle.js -v &
cd server
nodemon Server.js
