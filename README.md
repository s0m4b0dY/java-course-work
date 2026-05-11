```sh
# Все API, JSON, 10 объектов, максимум 2 задачи одновременно, интервал 1 секунда
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --count=10 --threads=2 --interval=1"
```

```sh
# Только Freepik + Emoji, JSON, 5 объектов, максимум 2 задачи одновременно, интервал 1 секунда
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --apis=freepik,emoji --format=json --output=icons --count=5 --threads=2 --interval=1"
```

```sh
# Только RandomUser, CSV, 15 объектов, максимум 1 задача одновременно, интервал 1 секунда
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --apis=randomuser --format=csv --output=users --count=15 --threads=1 --interval=1"
```

```sh
# Все API, CSV, дописывать к существующему файлу, 10 объектов, максимум 3 задачи одновременно, интервал 1 секунда
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=csv --output=data --append --count=10 --threads=3 --interval=1"
```

```sh
# Все API, JSON, напечатать только RandomUser после сохранения, 10 объектов
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --count=10 --threads=2 --interval=1 --print-apis=randomuser"
```

```sh
# Все API, JSON, напечатать RandomUser и Emoji после сохранения
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --count=10 --threads=2 --interval=1 --print-apis=randomuser,emoji"
```

```sh
# Все API, CSV, напечатать RandomUser и Freepik после сохранения
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=csv --output=result --count=10 --threads=2 --interval=1 --print-apis=randomuser,freepik"
```

```sh
# Все API, JSON, не перезаписывать файл, а дописать данные, потом напечатать все
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --append --count=10 --threads=2 --interval=1"
```

```sh
# Интерактивный режим ConsoleGui
mvn exec:java -Dexec.mainClass="com.voronina.course.Main"
```
