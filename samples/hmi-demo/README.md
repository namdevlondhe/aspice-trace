# hmi-demo: sample input for aspice-trace

A tiny, made-up climate HMI used to demonstrate the tool. The code is illustrative and is not compiled.

It has gaps on purpose, so the report has something to show:

- SW-HMI-001 to SW-HMI-004 are implemented and verified.
- SW-HMI-005 has a test but no code yet (test written first).
- SW-HMI-006 and SW-HMI-007 are implemented but have no test.
- SW-HMI-008 is not started.
- SW-HMI-099 is cited by the code but defined in no spec.

Only headings and the first cell of table rows define requirements, so this list does not.
Regenerate the committed report in `docs/sample-report` with `./gradlew sampleReport`.
