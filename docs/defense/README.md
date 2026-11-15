# Project defense reference

[Open the complete reviewer](FiveNightsAtGudos_Complete_Defense_Reviewer.md).

The reference covers every Java file and explicit method/constructor, clickable source locations, click-by-click feature demonstrations, worked gameplay/save examples, pattern participants, and 60 defense questions. The downloadable PDF edition is supplied separately.

The Java comments are based on source commit `b27297ab9e687518fc1543908740e5926969cf96`. The reviewer source links are pinned to annotated commit `7e6f0112de35a3fb648e45b1c0f53e876f6e98e4`.

New Game, Continue, night selection and next/replay buttons all use the `Navigation.startNight` contract implemented by `Application`, which has a facade role. `AudioService` is another facade, renderer wrappers implement Decorator, and the secret level uses Builder.

Validation for the comment changes: all 59 Java files have unchanged parsed executable syntax; all 70 production class files compile and match the original byte-for-byte with debug metadata disabled. The available JDK 21 compiler used source/target 17. The full Maven verification could not download dependencies because the Maven host was unreachable; it is not reported as passed. Twelve documentation screenshots were rendered from actual Swing views with controlled demo state.

The screenshots illustrate game layouts and model states, and do not prove physical audio or native focus/fullscreen on every operating system.
