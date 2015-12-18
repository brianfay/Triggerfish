# Triggerfish
Triggerfish is a tool for music performance, using supercollider for processing and a web interface for control.

I was writing it in javascript and sclang, but didn't like the way things were going. I want to learn clojure/clojurescript, so I'm starting over from scratch.

It doesn't do much of anything yet - you can make a tone with something like (call-scsynth "/s_new" "default" 3051 0 0) but only if you already ran sclang to get a default group and compile the default synthdef.

To run this, do 

    lein npm install
    rlwrap lein figwheel server-dev client-dev
    node out/server_out/triggerfish_server_with_figwheel.js 

and load localhost:3000 in a browser

...and probably some other stuff

For a reason I have yet to discover, the repl will focus on client-dev first. To switch focus, type ":cljs/quit" and choose server-dev. Then you can (in-ns 'server.core).
