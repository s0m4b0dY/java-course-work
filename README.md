```sh
# Все API, JSON, 10 объектов, интервал 50 мс
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --count=10 --interval=50"

# Только Freepik + Emoji, JSON, 5 объектов, интервал 10 мс:
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --apis=freepik,emoji --format=json --output=icons --count=5 --interval=10"

# Только RandomUser, CSV, 15 объектов, интервал 30 мс
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --apis=randomuser --format=csv --output=users --count=15 --interval=30"

# Все API, CSV, дописывать к существующему файлу, 10 объектов, интервал 50 мс
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=csv --output=data --append --count=10 --interval=50"

# Все API, JSON, напечатать только randomuser после сохранения, 10 объектов, интервал 50 мс
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --count=10 --interval=50 --api=randomuser"

# Интерактивный режим (ConsoleGui) — без аргументов
mvn exec:java -Dexec.mainClass="com.voronina.course.Main"
```
