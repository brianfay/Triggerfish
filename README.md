# Triggerfish
Triggerfish is a tool for music performance, using supercollider for processing and a web interface for control.

I was writing it in javascript and sclang, but didn't like the way things were going. I want to learn clojure/clojurescript, so I'm starting over from scratch.

To run this, do 

    lein npm install
    rlwrap lein figwheel server-dev client-dev
    node out/server_out/triggerfish_server_with_figwheel.js 

...and probably some other stuff
