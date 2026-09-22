# Course API collector

Small Java/Maven app that polls several public APIs and writes the result to JSON or CSV.

## APIs

The project currently uses:

- RandomUser API
- EmojiHub API
- Agent Nexus public discovery API

Agent Nexus uses the public discovery endpoint and does not require an API key for basic anonymous usage.
To make repeated polling return different kinds of data, `AgentNexusApi` cycles through several search needs such as email, payments, GitHub, PostgreSQL, video processing and monitoring.

## Run examples

```sh
# All APIs, JSON, 10 objects per API, max 2 parallel tasks, interval 1 second
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --count=10 --threads=2 --interval=1"
```

```sh
# Only Agent Nexus + Emoji, JSON, 10 objects per API
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --apis=agentnexus,emoji --format=json --output=discovery --count=10 --threads=2 --interval=1"
```

```sh
# Only RandomUser, CSV, 15 objects
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --apis=randomuser --format=csv --output=users --count=15 --threads=1 --interval=1"
```

```sh
# All APIs, CSV, append to existing file. If CSV headers changed, old rows are migrated.
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=csv --output=data --append --count=10 --threads=3 --interval=1"
```

```sh
# Print only selected API results after saving
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --count=10 --threads=2 --interval=1 --print-apis=agentnexus,emoji"
```

```sh
# Interactive mode
mvn exec:java -Dexec.mainClass="com.voronina.course.Main"
```

## Tests and coverage

```sh
mvn test
```

```sh
mvn verify
```

Coverage report is generated here:

```text
target/site/jacoco/index.html
```

The Jacoco check is configured for at least 70% line coverage.

### API unit tests

The API tests use Mockito to replace the project-owned `HttpSender` interface, so `mvn test` does not send real requests to EmojiHub, RandomUser or Agent Nexus. The real `fetchData()` methods still run, including JSON parsing, URL creation and validation. `RealHttpSender` is the small production adapter that uses Java `HttpClient`.
