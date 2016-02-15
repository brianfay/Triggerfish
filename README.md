# Triggerfish
Triggerfish is an audio patching environment. Audio objects built in SuperCollider can be patched together using a web interface (coming soon).

To run the development instance...

Make sure the supercollider i/o environmental variables are set:
    export SC_JACK_DEFAULT_OUTPUTS="system"
    export SC_JACK_DEFAULT_INPUTS="system"
Then do:

    gem install sass
    rlwrap lein run -m clojure.main --init script/figwheel.clj -r
    node out/server_out/triggerfish_server_with_figwheel.js 

and load localhost:3000 in a browser


To switch focus in the repl between builds, type ":cljs/quit" and choose client-dev or server-dev. From there you can run (in-ns 'triggerfish.server.scsynth) (or whatever namespace you want to test).

You might try

    (in-ns 'triggerfish-test.server.manual)
    (connect-test)

To hear a really simple example of sine waves going through tremolo effects.