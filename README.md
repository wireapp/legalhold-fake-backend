## LegalHold Fake Backend

Command line tool, sending and receiving messages to LegalHold service.
This tool will manage the calls /initiate and /confirm and then upload relevant data to a real backend.

It functions as a proxy, bypassing some limitations of the backend flow to add LegalHold on a user.

As LegalHold it concerned, it will look like a real backend adding a new device

On the backend, this will not display as a LegalHold device, but as a regular one.

### Stack
- Kotlin 2.0 on JDK17
- Ktor http client

### Run
`./gradlew run --args="--email email@wire.com --password pwd --teamId 123 --userId 456"` (and other optional params)

Use `./gradlew run --args="--help` to get a description of the parameters