# Triggerfish
Triggerfish is a tool for music performance, using supercollider for processing and a web interface for control.

I was writing it in javascript and sclang, but didn't like the way things were going. I want to learn clojure/clojurescript, so I'm starting over from scratch.

To run this...

Make sure the supercollider i/o environmental variables are set:
    export SC_JACK_DEFAULT_OUTPUTS="system"
    export SC_JACK_DEFAULT_INPUTS="system"
Then do:

    lein npm install
    rlwrap lein figwheel server-dev client-dev
    node out/server_out/triggerfish_server_with_figwheel.js 

and load localhost:3000 in a browser

...and probably some other stuff

For a reason I have yet to discover, the repl will focus on client-dev first. To switch focus, type ":cljs/quit" and choose server-dev. Then you can (in-ns 'triggerfish.server.scsynth).

Try:
(call-scsynth "/s_new" "default" 1000 0 0)
(call-scsynth "/s_new" "default" 1001 0 0 "freq" 660)
(call-scsynth "/s_new" "default" 1001 0 0 "freq" 880)
(call-scsynth "/s_new" "default" 1001 0 0 "freq" 1110)

Or maybe plug your guitar in and do something like:
(in-ns 'triggerfish.server.patch)
(add-object! (create-object :tremolo))
(add-object! (create-object :adc))
